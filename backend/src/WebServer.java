import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class WebServer {
    private static final long SESSION_TIMEOUT = Duration.ofMinutes(30).toNanos();
    private static final long SAVE_INTERVAL = Duration.ofSeconds(10).toNanos();
    private static final int SESSION_LIMIT = 64;
    private final Path frontend = Path.of(System.getenv().getOrDefault("FRONTEND_DIR", "frontend"));
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Properties settings = IOUtils.readPropertiesFile("res/app.properties");
    private final Properties messages = IOUtils.readPropertiesFile("res/message.properties");

    private final SaveStore saves;

    private WebServer(SaveStore saves) {
        this.saves = saves;
    }

    private static final class Session {
        final ShadowDungeon game;
        long lastUsed = System.nanoTime();

        long lastSaveAttempt = System.nanoTime();

        Session(ShadowDungeon game) {
            this.game = game;
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        String portSetting = args.length == 0 ? System.getenv().getOrDefault("PORT", "8080") : args[0];
        int port = Integer.parseInt(portSetting);
        String bindAddress = System.getenv().getOrDefault("BIND_ADDRESS", "127.0.0.1");
        var app = new WebServer(SaveStore.fromEnvironment());
        var server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Shadow Dungeon · http://" + bindAddress + ":" + server.getAddress().getPort());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/api/session") || path.equals("/api/frame") || path.equals("/api/save")) {
                if (!method.equals("POST")) {
                    exchange.getResponseHeaders().set("Allow", "POST");
                    send(exchange, 405, "POST required");
                } else if (path.equals("/api/session")) {
                    createSession(exchange);
                } else {
                    tick(exchange, path.equals("/api/save"));
                }
            } else if (method.equals("GET")) {
                serveFile(exchange, path);
            } else {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405, "GET required");
            }
        }
    }

    private synchronized void createSession(HttpExchange exchange) throws IOException {
        removeExpiredSessions(System.nanoTime());
        String token = exchange.getRequestHeaders().getFirst("X-Game-Session");
        UUID id = sessionId(exchange);
        if (token != null && id == null) {
            send(exchange, 400, "Invalid session");
            return;
        }
        Session session = sessions.get(id);
        boolean resume = id != null;
        if (session == null) {
            if (sessions.size() >= SESSION_LIMIT) {
                send(exchange, 503, "Session limit reached");
                return;
            }
            try {
                if (resume) {
                    ShadowDungeon game = saves == null ? null : saves.load(id, settings, messages);
                    if (game == null) {
                        send(exchange, 404, "Save not found");
                        return;
                    }
                    session = new Session(game);
                } else {
                    id = UUID.randomUUID();
                    session = new Session(new ShadowDungeon(settings, messages));
                    if (saves != null) saves.save(id, session.game);
                }
            } catch (SQLException | IOException error) {
                send(exchange, 503, "Save storage unavailable");
                return;
            }
            sessions.put(id, session);
        }
        session.lastUsed = System.nanoTime();
        send(exchange, resume ? 200 : 201, id + "\n" + settings.getProperty("window.width") + "\n"
                + settings.getProperty("window.height") + "\n" + (saves != null));
    }

    private static UUID sessionId(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("X-Game-Session");
        if (token == null || token.length() != 36) return null;
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private void removeExpiredSessions(long now) {
        sessions.values().removeIf(session -> now - session.lastUsed >= SESSION_TIMEOUT);
    }

    private synchronized Session findSession(UUID id) {
        long now = System.nanoTime();
        removeExpiredSessions(now);
        Session session = sessions.get(id);
        if (session != null) session.lastUsed = now;
        return session;
    }

    private void tick(HttpExchange exchange, boolean manualSave) throws IOException {
        UUID id = sessionId(exchange);
        Session session = findSession(id);
        if (session == null) {
            send(exchange, 404, "Session expired or missing");
            return;
        }

        if (manualSave) {
            if (saves == null) {
                send(exchange, 503, "Save storage unavailable");
                return;
            }
            synchronized (session) {
                try {
                    saves.save(id, session.game);
                    session.lastSaveAttempt = System.nanoTime();
                } catch (SQLException | IOException error) {
                    send(exchange, 503, "Save failed");
                    return;
                }
            }
            send(exchange, 200, "Saved");
            return;
        }

        byte[] payload = exchange.getRequestBody().readNBytes(257);
        String[] fields = new String(payload, StandardCharsets.UTF_8).trim().split(" +");
        Input input;
        int ticks;
        try {
            if (payload.length > 256 || fields.length != 6) throw new IllegalArgumentException();
            ticks = Integer.parseInt(fields[0]);
            int held = Integer.parseInt(fields[1]);
            int pressed = Integer.parseInt(fields[2]);
            int fire = Integer.parseInt(fields[3]);
            double x = Double.parseDouble(fields[4]);
            double y = Double.parseDouble(fields[5]);
            int mask = (1 << Keys.values().length) - 1;
            if (ticks < 1 || ticks > 4 || (held & ~mask) != 0 || (pressed & ~mask) != 0
                    || fire < 0 || fire > 1 || !Double.isFinite(x) || !Double.isFinite(y)
                    || x < 0 || x > Integer.parseInt(settings.getProperty("window.width"))
                    || y < 0 || y > Integer.parseInt(settings.getProperty("window.height"))) {
                throw new IllegalArgumentException();
            }
            input = new Input(held, pressed, fire == 1, x, y);
        } catch (IllegalArgumentException error) {
            send(exchange, 400, "Invalid frame input");
            return;
        }

        String frame;
        synchronized (session) {
            Player player = session.game.getPlayer();
            String room = session.game.getCurrRoomName();
            double coins = player.getCoins();
            double keys = player.getKeys();
            CharacterType character = player.getCurrentCharacter();
            frame = session.game.frame(input, ticks);
            long now = System.nanoTime();
            if (saves != null && (now - session.lastSaveAttempt >= SAVE_INTERVAL
                    || player != session.game.getPlayer() || !room.equals(session.game.getCurrRoomName())
                    || coins != player.getCoins() || keys != player.getKeys()
                    || character != player.getCurrentCharacter())) {
                session.lastSaveAttempt = now;
                try {
                    saves.save(id, session.game);
                } catch (SQLException | IOException error) {
                    System.err.println("Automatic game save failed: " + error.getClass().getSimpleName());
                    exchange.getResponseHeaders().set("X-Game-Save-Failed", "true");
                }
            }
        }
        send(exchange, 200, frame);
    }

    private void serveFile(HttpExchange exchange, String path) throws IOException {
        Path file = switch (path) {
            case "/" -> frontend.resolve("index.html");
            case "/game.js", "/styles.css" -> frontend.resolve(path.substring(1));
            default -> {
                if (path.matches("/res/[a-z_]+\\.(png|otf)")) yield Path.of(path.substring(1));
                if (path.matches("/assets/[a-zA-Z0-9_-]+\\.(js|css|otf)")) {
                    yield frontend.resolve(path.substring(1));
                }
                yield null;
            }
        };
        if (file == null || !Files.isRegularFile(file)) {
            send(exchange, 404, "Page not found");
            return;
        }
        String type = switch (file.toString().substring(file.toString().lastIndexOf('.') + 1)) {
            case "html" -> "text/html; charset=utf-8";
            case "js" -> "text/javascript; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "otf" -> "font/otf";
            default -> "image/png";
        };
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, Files.size(file));
        Files.copy(file, exchange.getResponseBody());
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}

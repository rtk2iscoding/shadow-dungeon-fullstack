import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class WebServer {
    private static final long SESSION_TIMEOUT = Duration.ofMinutes(30).toNanos();
    private static final int SESSION_LIMIT = 64;
    private final Map<String, Session> sessions = new HashMap<>();
    private final Properties settings = IOUtils.readPropertiesFile("res/app.properties");
    private final Properties messages = IOUtils.readPropertiesFile("res/message.properties");

    private static final class Session {
        final ShadowDungeon game;
        long lastUsed = System.nanoTime();

        Session(Properties settings, Properties messages) {
            game = new ShadowDungeon(settings, messages);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        var app = new WebServer();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Shadow Dungeon · http://127.0.0.1:" + server.getAddress().getPort());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/api/session") || path.equals("/api/frame")) {
                if (!method.equals("POST")) {
                    exchange.getResponseHeaders().set("Allow", "POST");
                    send(exchange, 405, "POST required");
                } else if (path.equals("/api/session")) {
                    createSession(exchange);
                } else {
                    tick(exchange);
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
        if (sessions.size() >= SESSION_LIMIT) {
            send(exchange, 503, "Session limit reached");
            return;
        }
        String id = UUID.randomUUID().toString();
        sessions.put(id, new Session(settings, messages));
        send(exchange, 201, id + "\n" + settings.getProperty("window.width") + "\n"
                + settings.getProperty("window.height"));
    }

    private void removeExpiredSessions(long now) {
        sessions.values().removeIf(session -> now - session.lastUsed >= SESSION_TIMEOUT);
    }

    private synchronized Session findSession(String id) {
        long now = System.nanoTime();
        removeExpiredSessions(now);
        Session session = sessions.get(id);
        if (session != null) session.lastUsed = now;
        return session;
    }

    private void tick(HttpExchange exchange) throws IOException {
        Session session = findSession(exchange.getRequestHeaders().getFirst("X-Game-Session"));
        if (session == null) {
            send(exchange, 404, "Session expired or missing");
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
            frame = session.game.frame(input, ticks);
        }
        send(exchange, 200, frame);
    }

    private void serveFile(HttpExchange exchange, String path) throws IOException {
        Path file = switch (path) {
            case "/" -> Path.of("frontend/index.html");
            case "/game.js", "/styles.css" -> Path.of("frontend" + path);
            default -> path.matches("/res/[a-z_]+\\.(png|otf)") ? Path.of(path.substring(1)) : null;
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

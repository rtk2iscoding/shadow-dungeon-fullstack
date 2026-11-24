import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

final class SaveStore {
    private static final int MAX_SAVE_BYTES = 1_048_576;
    // Only server-produced game snapshots are stored. Reject other types on restore.
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=40;maxrefs=50000;maxarray=20000;maxbytes=1048576;"
            + "ShadowDungeon;PrepRoom;BattleRoom;EndRoom;Player;Store;Image;Image$Size;Point;"
            + "Rectangle;Door;RestartArea;TheMarine;TheRobot;WeaponManage;Bullet;FireBall;"
            + "Key;KeyBulletKin;BulletKin;AshenBulletKin;TreasureBox;Wall;River;Basket;Table;"
            + "CharacterType;java.util.ArrayList;java.util.Vector;java.lang.Object;"
            + "java.lang.Enum;java.lang.Double;java.lang.Number;!*");

    private final String url;
    private final Properties credentials = new Properties();

    static SaveStore fromEnvironment() throws SQLException {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) return null;
        return new SaveStore(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
    }

    SaveStore(String url, String user, String password) throws SQLException {
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("DB_URL must be a PostgreSQL JDBC URL");
        }
        this.url = url;
        credentials.setProperty("connectTimeout", "3");
        credentials.setProperty("socketTimeout", "3");
        if (user != null) credentials.setProperty("user", user);
        if (password != null) credentials.setProperty("password", password);
        try (var db = connect(); var statement = db.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS game_saves (
                    save_id UUID PRIMARY KEY,
                    state BYTEA NOT NULL,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CHECK (octet_length(state) <= 1048576)
                )
                """);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, credentials);
    }

    ShadowDungeon load(UUID id, Properties settings, Properties messages)
            throws IOException, SQLException {
        try (var db = connect();
             var stmt = db.prepareStatement("SELECT state FROM game_saves WHERE save_id = ?")) {
            stmt.setObject(1, id);
            try (var rows = stmt.executeQuery()) {
                return rows.next() ? decode(rows.getBytes(1), settings, messages) : null;
            }
        }
    }

    void save(UUID id, ShadowDungeon game) throws IOException, SQLException {
        byte[] state = encode(game);
        try (var db = connect(); var stmt = db.prepareStatement("""
            INSERT INTO game_saves (save_id, state) VALUES (?, ?)
            ON CONFLICT (save_id) DO UPDATE
            SET state = EXCLUDED.state, updated_at = CURRENT_TIMESTAMP
            """)) {
            stmt.setObject(1, id);
            stmt.setBytes(2, state);
            stmt.executeUpdate();
        }
    }

    static byte[] encode(ShadowDungeon game) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(game);
        }
        if (bytes.size() > MAX_SAVE_BYTES) throw new IOException("Game snapshot exceeds its size limit");
        return bytes.toByteArray();
    }

    static ShadowDungeon decode(byte[] bytes, Properties settings, Properties messages) throws IOException {
        if (bytes.length > MAX_SAVE_BYTES) throw new IOException("Game snapshot exceeds its size limit");
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            input.setObjectInputFilter(FILTER);
            if (!(input.readObject() instanceof ShadowDungeon game)) throw new IOException("Invalid game snapshot");
            game.restoreConfiguration(settings, messages);
            return game;
        } catch (ClassNotFoundException error) {
            throw new IOException("Unsupported game snapshot", error);
        }
    }
}

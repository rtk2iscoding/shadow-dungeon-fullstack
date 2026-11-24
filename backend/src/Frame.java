/** Draw commands in paint order, encoded as tab-separated rows for the canvas. */
final class Frame {
    private static final ThreadLocal<StringBuilder> COMMANDS = new ThreadLocal<>();

    static void begin() {
        COMMANDS.set(new StringBuilder());
    }

    static String finish(String room) {
        return room + "\n" + COMMANDS.get();
    }

    static void discard() {
        COMMANDS.remove();
    }

    static void image(String path, double x, double y) {
        var commands = COMMANDS.get();
        if (commands != null) {
            commands.append("image\t").append(x).append('\t').append(y)
                    .append('\t').append(path).append('\n');
        }
    }

    static void text(String text, double x, double y, int size, boolean centered) {
        var commands = COMMANDS.get();
        if (commands != null) {
            commands.append("text\t").append(x).append('\t').append(y)
                    .append('\t').append(size).append('\t').append(centered)
                    .append('\t').append(text.replaceAll("[\\r\\n\\t]", " ")).append('\n');
        }
    }
}

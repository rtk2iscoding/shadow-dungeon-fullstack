final class Window {
    static int getWidth() {
        return Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("window.width"));
    }

    static int getHeight() {
        return Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("window.height"));
    }
}

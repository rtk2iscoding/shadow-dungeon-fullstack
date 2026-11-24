/**
 * Helper methods to display information for the player
 */
public class UserInterface {
    public static void drawStats(double health, double coins, double keys,double weapon) {
        int fontSize = Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("playerStats.fontSize"));
        drawData(String.format("%s %.1f", ShadowDungeon.current().getMessageProps().getProperty("healthDisplay"), health), fontSize,
                IOUtils.parseCoords(ShadowDungeon.current().getGameProps().getProperty("healthStat")));
        drawData(String.format("%s %.0f", ShadowDungeon.current().getMessageProps().getProperty("coinDisplay"), coins), fontSize,
                IOUtils.parseCoords(ShadowDungeon.current().getGameProps().getProperty("coinStat")));
        drawData(String.format("%s %.0f", ShadowDungeon.current().getMessageProps().getProperty("keyDisplay"), keys), fontSize,
                IOUtils.parseCoords(ShadowDungeon.current().getGameProps().getProperty("keyStat")));
        drawData(String.format("%s %.0f", ShadowDungeon.current().getMessageProps().getProperty("weaponDisplay"), weapon), fontSize,
                IOUtils.parseCoords(ShadowDungeon.current().getGameProps().getProperty("weaponStat")));


    }

    public static void drawStartMessages() {
        drawTextCentered("title", Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("title.fontSize")), Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("title.y")));
        drawTextCentered("moveMessage", Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("prompt.fontSize")), Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("moveMessage.y")));
        drawText("robotDescription",Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("playerStats.fontSize")),IOUtils.parseCoords(ShadowDungeon.current().getGameProps().getProperty("robotMessage")));
        drawText("marineDescription",Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("playerStats.fontSize")),IOUtils.parseCoords(ShadowDungeon.current().getGameProps().getProperty("marineMessage")));
    }

    public static void drawEndMessage(boolean win) {
        drawTextCentered(win ? "gameEnd.won" : "gameEnd.lost", Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("title.fontSize")), Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("title.y")));
    }

    public static void drawTextCentered(String textPath, int fontSize, double posY) {
        String text = ShadowDungeon.current().getMessageProps().getProperty(textPath);
        Frame.text(text, Window.getWidth() / 2.0, posY, fontSize, true);
    }

    public static void drawData(String data, int fontSize, Point location) {
        Frame.text(data, location.x, location.y, fontSize, false);
    }
    public static void drawText(String textPath, int fontSize, Point location) {
        String text = ShadowDungeon.current().getMessageProps().getProperty(textPath);
        double posX = location.x;
        double posY = location.y;
        Frame.text(text, posX, posY, fontSize, false);
    }



}

/**
 * Store: in-game purchasing UI and logic
 */
public class Store implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Point positon ;
    private boolean available;
    private final Image sprite = new Image("res/store.png");

    public Store(String config) {
        this.positon = IOUtils.parseCoords(config);
    }
    /**
     * Handles purchase actions from player input.
     * <p>Press E to buy +50 health points if affordable.
     * Press L to upgrade the weapon if not at max level.</p>
     *
     * @param input  the current keyboard/mouse input state
     * @param player the player object to apply purchases to
     */
    public void update(Input input,Player player) {
        //player = ShadowDungeon.current().getPlayer();
        if(player != null) {
            if (this.available && player.getHealth() >= 0) {
                if (input.wasPressed(Keys.P)) {
                    ShadowDungeon.current().resetGameState(ShadowDungeon.current().getGameProps());
                }
                if (input.wasPressed(Keys.E)) {
                    if (player.getCoins() >= 50) {
                        player.spendCoins(ShadowDungeon.current().getGameProps().getProperty("healthPurchase"));
                        player.addHealth(ShadowDungeon.current().getGameProps().getProperty("healthBonus"));
                    }
                }
                if (input.wasPressed(Keys.L) &&player.getWeaponLevel()<=1&& player.getCoins() >= 50) {
                    player.upgradeWeapon(Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("weaponPurchase")));
                }
            }
            UserInterface.drawStats(player.getHealth(), player.getCoins(), player.getKeys(), player.getWeaponLevel());
        }

    }

    public void appear(){
        this.available = true;
    }
    public void draw(){
        if(this.available){
            sprite.draw(positon.x, positon.y);
        }
    }

    public void disappear() {
        this.available = false;
    }
}

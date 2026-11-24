/**
 * Hazard that applies damage for as long as the player is on it
 * Marine character is immune
 */
public class River implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Point position;
    private final Image image;
    private final double damagePerFrame;

    public River(Point position) {
        this.position = position;
        this.image = new Image("res/river.png");
        damagePerFrame = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("riverDamagePerFrame"));
    }

    public void update(Player player) {
        if (hasCollidedWith(player)&&player.getCurrentCharacter()!=CharacterType.MARINE) {
            player.receiveDamage(damagePerFrame);
        }
    }
    /** Renders the river sprite. */
    public void draw() {
        image.draw(position.x, position.y);
    }
    /**
     * Checks overlap with the player using bounding boxes.
     *
     * @param player the player to test
     * @return true if overlapping; false otherwise
     */
    public boolean hasCollidedWith(Player player) {
        return image.getBoundingBoxAt(position).intersects(player.getCurrImage().getBoundingBoxAt(player.getPosition()));
    }
}
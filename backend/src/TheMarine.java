/**
 * marine character preview entity.
 * Renders the marine sprite at a fixed position and supports hit-testing
 * with the player (for selection or interaction).
 */
public class TheMarine implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Point position;
    private static final Image sprite = new Image("res/marine_sprite.png");
    boolean choose = false;
    /**
     * Creates a marine entity at the given position.
     * @param position the screen position to render at
     */

    public TheMarine(Point position)
    {
        this.position = position;
    }
    /** Draws the marine sprite on screen. */
    public void draw(){
        sprite.draw(position.x, position.y);
    }
    /**
     * Checks if the marine collides with the player.
     * @param player the player
     * @return true if they collide, false otherwise
     */
    public boolean hasCollidedWith(Player player) {
        return sprite.getBoundingBoxAt(position).intersects(player.getCurrImage().getBoundingBoxAt(player.getPosition()));
    }





}

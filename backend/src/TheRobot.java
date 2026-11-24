import java.awt.*;
/**
 * Robot character preview entity.
 * Renders the robot sprite at a fixed position and supports hit-testing
 * with the player (for selection or interaction).
 */
public class TheRobot {
    private final Point position;
    private static final Image sprite = new Image("res/robot_sprite.png");
    boolean choose;
    /**
     * Creates a robot entity at the given position.
     * @param position the screen position to render at
     */
    public TheRobot(Point position) {
        this.position = position;
    }
    /** Draws the robot sprite on screen. */
    public void draw(){
        sprite.draw(position.x, position.y);
    }
    /**
     * Checks if the robot collides with the player.
     * @param player the player
     * @return true if they collide, false otherwise
     */
    public boolean hasCollidedWith(Player player) {
        return sprite.getBoundingBoxAt(position).intersects(player.getCurrImage().getBoundingBoxAt(player.getPosition()));
    }

}

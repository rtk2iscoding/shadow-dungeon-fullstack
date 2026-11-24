/**
 * destruct by the player to earn coins
 */
public class Table implements Obstacle{
    private final Image image;
    private final Point position;
    private boolean dead = false;
    public Table(Point position) {
        this.position = position;
        this.image = new Image("res/table.png");
    }


    public void update(Player player) {
        if (!dead&&hasCollidedWith(player)) {
            // set the player to its position prior to attempting to move through this wall
            player.move(player.getPrevPosition().x, player.getPrevPosition().y);
        }
    }
    public void draw() {
        if (!dead) {
            image.draw(position.x, position.y);
        }

    }

    public boolean hasCollidedWith(Player player) {
        return image.getBoundingBoxAt(position).intersects(player.getCurrImage().getBoundingBoxAt(player.getPosition()));
    }
    public boolean isDestructible() {
        return !dead;
    }
    public void destruct(){
        dead = true;
    }
    public boolean isDead() {
        return dead;
    }
    public Rectangle getHitBox() {
        return image.getBoundingBoxAt(position);
    }
}

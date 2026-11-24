/**
 * destruct by the player to earn coins
 */
public class Basket implements Obstacle,Rewardable {
    private final Image image;
    private final Point position;
    private final double coinValue;
    private boolean dead=false;

    public Basket(Point position, double coinValue) {
        this.position = position;
        this.coinValue = coinValue;
        this.image = new Image("res/basket.png");
    }
    public void update(Player player) {
        if (!dead&&hasCollidedWith(player)) {
         // set the player to its position prior to attempting to move through this wall
            player.move(player.getPrevPosition().x, player.getPrevPosition().y);
        }

    }

    public void draw() {
        if(!dead){
            image.draw(position.x, position.y);
        }
    }

    public boolean hasCollidedWith(Player player) {
        return image.getBoundingBoxAt(position).intersects(player.getCurrImage().getBoundingBoxAt(player.getPosition()));
    }
    public boolean isDestructible() {
        return true;
    }
    public void destruct() {
        this.dead=true;
    }
    public boolean isDead() {
        return dead;
    }
    public Rectangle getHitBox() {
        return image.getBoundingBoxAt(position);
    }
    public void reward(Player player) {
        if(player!=null){
            player.earnCoins(coinValue);
        }

    }
    public boolean isActive(){
        return !dead;
    }
}

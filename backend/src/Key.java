/**
 * Collect that can unlock treasure boxes to earn coins
 */
public class Key {
    private final Image image;
    private Point position;
    private boolean appear;
    private boolean used = false;


    public Key(Point position) {
        this.position = position;
        this.image = new Image("res/key.png");
        this.appear = false;
    }
    public void update(Player player) {
        if(hasCollidedWith(player)&&!used){
            player.addKey();
            appear = false;
            used = true;
        }
        if(!used){
            draw();
        }
    }
    public void draw() {
        image.draw(position.x, position.y);
    }
    public boolean isAppear() {
        return appear;
    }
    public void setAppear() {
        appear = true;
    }
    public boolean hasCollidedWith(Player player) {
        return image.getBoundingBoxAt(position).intersects(player.getCurrImage().getBoundingBoxAt(player.getPosition()));
    }
    public void setPosition(Point position) {
        this.position = position;
    }
}

import java.util.Vector;
/**
 * Shoot from enemies that attack player
 */

public class FireBall implements Moveable, Obstacle{
    private Point prevPosition;
    private Point position;
    private final double speed ;
    private final double damage;
    private static final Image sprite = new Image("res/fireball.png");
    private final Vector<Double> direction;

    Rectangle rect;
    private boolean dead;

    public FireBall(Point position,  Vector<Double> direction){
        this.speed = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("fireballSpeed"));
        this.damage = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("fireballDamage"));
        this.direction = direction;
        this.dead = false;
        this.position = position;
    }
    public void update()
    {
        move(direction.get(0), direction.get(1));
        double currX = position.x;
        double currY = position.y;
        rect = sprite.getBoundingBoxAt(new Point(currX, currY));
    }
    @Override
    public void move(double vx, double vy) {
        this.prevPosition =this.position;
        this.position = new Point(prevPosition.x + vx*speed, prevPosition.y + vy*speed);
    }
    public void draw() {
        if(!dead){
            sprite.draw(position.x, position.y);
        }

    }
    @Override
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
        return rect;
    }

    public double getDamage() {
        return damage;
    }
}

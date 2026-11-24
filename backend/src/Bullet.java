import java.util.Vector;
/**
 * Shoot by player that attack Obstacles and enemies
 */

public class Bullet implements Moveable, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Point position;
    private Point prevPosition;
    private final double speed;
    private double damage;
    private final Vector<Double> direction;
    private static final Image sprite = new Image("res/bullet.png");
    private Rectangle rect;
    private boolean dead;

    public Bullet(Point position, Vector<Double> direction, double level){
        this.position = position;
        this.prevPosition = position;
        this.speed = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("bulletSpeed"));
        this.damage = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("weaponStandardDamage"));
        this.direction = direction;
        this.dead = false;
        setLevel(level);
    }
    public void update(){
        move(direction.get(0), direction.get(1));
        double currX = position.x;
        double currY = position.y;
        rect = sprite.getBoundingBoxAt(new Point(currX, currY));
        if (!Double.isFinite(currX) || !Double.isFinite(currY)
                || !rect.intersects(new Rectangle(0, 0, Window.getWidth(), Window.getHeight()))) {
            destruct();
        }

    }
    public void setLevel(double level){
        if(level == 0){
           damage = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("weaponStandardDamage"));
        }
        if(level == 1){
            damage = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("weaponAdvanceDamage"));
        }
        if(level == 2){
            damage = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("weaponEliteDamage"));
        }
    }
    public void move(double vx, double vy) {
        this.prevPosition =this.position;
        this.position = new Point(prevPosition.x + vx*speed, prevPosition.y + vy*speed);
    }
    public void draw(){
        if(!dead){
            sprite.draw(position.x, position.y);
        }

    }
    public boolean isDead(){
        return this.dead;
    }

    public void destruct() {
        this.dead=true;
    }


    public Rectangle getHitBox() {
        return rect;
    }

    public Double getDamage(){
        return this.damage;
    }
}

import java.util.ArrayList;
import java.util.Vector;

import static java.lang.Double.parseDouble;

/**
 * Enemy that been attacked when the bullets overlap with it ,becoming key after defeated.
 */
public class KeyBulletKin implements Damageable, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Point position;
    private final Image image;
    private boolean active = false; /** True only while the battle room is active. */
    private boolean dead = false;/** True once health ≤ 0. */
    private double health;
    private double speed;
    private final double damage;
    /** Looping path waypoints (moves p0 → p1 → ... → pn → p0). */
    final ArrayList<Point> route = new ArrayList<>();
    int idx;
    private Point prevPosition;


    public KeyBulletKin(Point startPos) {
        this.position = startPos;
        this.image = new Image("res/key_bullet_kin.png");
        this.health = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("keyBulletKinHealth"));
        this.speed = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("keyBulletKinSpeed"));
        this.damage = 0.2;

        this.idx = 0;
        this.prevPosition = startPos;


    }

    /**
     * Per-frame update: route, damage,
     * and sets {@code dead} when health ≤ 0.
     *
     * @param player current player (for collision/damage)
     */

    public void update(Player player) {
        move();

        if (hasCollidedWith(player)) {
            player.loseHealth(damage);
        }
        if(this.health <= 0){
            dead = true;
            active = false;
        }
    }

    public void draw() {
        image.draw(position.x, position.y);
    }

    public boolean hasCollidedWith(Player player) {
        return image.getBoundingBoxAt(position).intersects(player.getCurrImage().getBoundingBoxAt(player.getPosition()));
    }

    public boolean isDead() {
        return dead;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    public void takeDamage(double damage){
        this.health -= damage;
    }
    public Rectangle getHitBox(){
        return image.getBoundingBoxAt(position);
    }
    /**
     * Moves towards the current waypoint at speed and
     * advances to the next waypoint when reached; loops indefinitely.
     * No movement occurs if the route is empty.
     */
    public void move() {
        if (route.isEmpty()) return;
        if (idx >= route.size()) idx = 0;

        final double EPS = 1e-6;
        Point t = route.get(idx);
        double dx = t.x - position.x, dy = t.y - position.y;
        double dist = Math.hypot(dx, dy);
        if (dist <= EPS) { position = new Point(t.x, t.y); idx = (idx + 1) % route.size(); return; }

        double step = Math.min(speed, dist);  // 关键一行
        position = new Point(position.x + dx * step / dist,
                position.y + dy * step / dist);
        if (step == dist) idx = (idx + 1) % route.size();
    }
    /**
     * Adds a waypoint to the movement route.
     *
     * @param p waypoint position to append to the route
     */
    public void addWaypoint(Point p) {
        route.add(p);
    }
    public Point getPosition() {
        return position;
    }
}

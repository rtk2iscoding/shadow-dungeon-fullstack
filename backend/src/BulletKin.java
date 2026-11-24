import javax.swing.*;
import java.util.Vector;
/**
 * Shooting fireballs to player's position
 */
public class BulletKin implements Damageable,Rewardable{
    private final Point position;
    private final Image image;
    private boolean active = false; // only true when the Battle Room has been activated
    private boolean dead = false;
    private final double firingRate;
    private double health;
    private final double coins;
    private final double damageContact;
    private double fireCooldown;

    public BulletKin(Point startPos) {
        this.position = startPos;
        this.image = new Image("res/bullet_kin.png");
        this.health = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("bulletKinHealth"));
        this.coins = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("bulletKinCoin"));
        this.firingRate =Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("bulletKinShootFrequency"));
        this. damageContact = 0.2;
    }
    /** update */
    public void update(Player player) {
        if (hasCollidedWith(player)) {
            player.loseHealth(damageContact);
        }
        if(health <= 0){
            dead = true;
            active = false;
            player.earnCoins(coins);
            if(player.getCurrentCharacter()==CharacterType.ROBOT){
                player.earnCoins(5);
            }
        }
        /** cooldown */
        if (fireCooldown > 0) fireCooldown--;

        if(fireCooldown == 0&&!dead){
            double dx = player.getPosition().x - position.x;
            double dy = player.getPosition().y - position.y;
            double distance = Math.hypot(dx,dy);
            Vector<Double> dir = new Vector<>(2);
            dir.add(dx/distance);
            dir.add(dy/distance);
            ShadowDungeon.current().spawnFireBall(position,dir);
            fireCooldown = firingRate;
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
    public void takeDamage(double amount) {
        health -= amount;
    }
    public void reward(Player player){
        player.earnCoins(coins);
    }
    public Rectangle getHitBox(){
        return image.getBoundingBoxAt(position);
    }

}

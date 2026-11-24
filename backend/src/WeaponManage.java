import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
/**
 * Manages player bullets and enemy fireballs: spawning, updating,
 * collision handling, rendering, and cleanup.
 */
public class WeaponManage implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    List<Bullet> bullets;
    List<FireBall> fireBalls;
    /** Creates empty bullet and fireball pools. */
    public WeaponManage(){
        bullets = new ArrayList<>();
        fireBalls = new ArrayList<>();
    }
    /**
     * Spawns a player bullet.
     *
     * @param pos  bullet start position (center)
     * @param dir  normalized direction vector
     * @param weaponLevel weapon level affecting bullet damage
     */
    public void spawnBullet(Point pos,Vector<Double> dir,double weaponLevel){
        bullets.add(new Bullet(pos, dir,weaponLevel));
       }
    /**
     * Spawns an enemy fireball.
     *
     * @param pos start position (center)
     * @param dir normalized direction vector
     */
    public void spawnFireBall(Point pos, Vector<Double> dir){fireBalls.add(new FireBall(pos,dir));}
    /**
     * Updates all bullets: movement, collisions, rewards, damage and rendering.
     *
     * @param targets obstacles (e.g., walls/tables/baskets)
     * @param enermies damageable targets (enemies)  // spelling kept as-is
     * @param player  player (for rewards on destructible/Rewardable targets)
     */
    public void update(ArrayList<Obstacle> targets, ArrayList<Damageable> enermies, Player player) {
        for (Bullet bullet : bullets) {
            bullet.update();
            for (Obstacle target : targets) {
                if (!bullet.isDead() && !target.isDead() && bullet.getHitBox().intersects(target.getHitBox())) {
                    if (target instanceof Rewardable) {
                        Rewardable r = (Rewardable) target;
                        if (r.isActive()) {
                            r.reward(player);
                        }
                    }
                    target.destruct();
                    bullet.destruct();
                    break;
                }
            }
            for(Damageable enermy : enermies) {
                if (!bullet.isDead() && !enermy.isDead() && bullet.getHitBox().intersects(enermy.getHitBox())){
                    enermy.takeDamage(bullet.getDamage());
                    bullet.destruct();
                    break;
                }
            }
            bullet.draw();

        }
        bullets.removeIf(Bullet::isDead);
    }
    /**
     * Updates all enemy fireballs: movement, collisions with obstacles/player, and rendering.
     *
     * @param targets obstacles that destroy fireballs on contact
     * @param player  player to damage on hit
     */
    public void updateFireBall(ArrayList<Obstacle> targets,Player player){
        for(FireBall fireBall : fireBalls){
            fireBall.update();
            //only target player
            for(Obstacle target : targets){
                if(!fireBall.isDead()&&!target.isDead()&&fireBall.getHitBox().intersects(target.getHitBox())){
                    fireBall.destruct();
                    break;
                }
            }
            if(!fireBall.isDead()&&fireBall.getHitBox().intersects(player.getHitBox())){
                fireBall.destruct();
                player.receiveDamage(fireBall.getDamage());
                break;
            }
            fireBall.draw();
        }
        fireBalls.removeIf(FireBall::isDead);
    }
    /** Draws all active bullets and fireballs. */
    public void draw(){
        for(Bullet bullet : bullets){
            //System.out.println("Updating Bullet");
            bullet.draw();
        }
        for(FireBall fireBall: fireBalls){
            fireBall.draw();
        }
    }
    /** Destroys all active projectiles (used on room transition). */
    public void clear(){
        for (Bullet bullet : bullets){
            bullet.destruct();
        }
        for(FireBall fireBall : fireBalls){
            fireBall.destruct();
        }

    }
    /** Updates and renders all projectiles without collision handling. */
    public void update(){
        for(Bullet bullet : bullets){
            //System.out.println("Updating Bullet");
            bullet.update();
            bullet.draw();
        }
        bullets.removeIf(Bullet::isDead);
        for(FireBall fireball : fireBalls){
            fireball.update();
            fireball.draw();
        }
        fireBalls.removeIf(FireBall::isDead);
    }



}

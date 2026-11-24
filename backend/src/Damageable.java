/**
 * Represents an entity that can take damage and be destroyed.
 */
public interface Damageable {
    /**
     * Applies damage to this object.
     *
     * @param damage the amount of damage to apply
     */
    void takeDamage(double damage);
    /**
     * Checks whether this object has been destroyed.
     *
     * @return true if dead; false otherwise
     */
    boolean isDead();
    /**
     * Returns the hit box used for collision detection.
     *
     * @return the bounding rectangle
     */
    Rectangle getHitBox();
}


/**
 * Common contract for in-room obstacles (e.g., walls, tables, baskets).
 * Some obstacles are destructible; others are permanent blockers.
 */
public interface Obstacle {
       boolean isDestructible();
       void destruct();
       boolean isDead();
       Rectangle getHitBox();
}

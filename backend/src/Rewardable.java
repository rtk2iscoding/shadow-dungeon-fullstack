/**
 * Represents an entity that can grant a reward to the player,
 * such as coins, keys, or upgrades.
 */
public interface Rewardable{
    void reward(Player player);
    boolean isActive();
    }

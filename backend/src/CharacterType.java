public enum CharacterType {
    ROBOT("Robot"),
    MARINE("Marine");

    private final String displayName;

    CharacterType(String displayName) { this.displayName = displayName; }

    /** Human-readable name for HUD/logs. */
    public String displayName() { return displayName; }
}

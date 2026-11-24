import java.util.Vector;

/**
 * Player character that can move around and between rooms, defeat enemies, collect coins
 */
public class Player implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Point prevPosition;
    private Point position;
    private Image currImage;
    private double health;
    private double key;
    private double weaponLevel;
    private final double speed;
    private double coins = 0;
    private boolean faceLeft = false;

    private static final Image RIGHT_IMAGE = new Image("res/player_right.png");
    private static final Image LEFT_IMAGE = new Image("res/player_left.png");
    private static final Image ROBOT_LEFT = new Image("res/robot_left.png");
    private static final Image ROBOT_RIGHT = new Image("res/robot_right.png");
    private static final Image MARINE_LEFT = new Image("res/marine_left.png");
    private static final Image MARINE_RIGHT = new Image("res/marine_right.png");



    private CharacterType currentCharacter = null;
    private boolean haveChoose;
    private int fireCooldown = 0;
    private final int fireFreq = Integer.parseInt(ShadowDungeon.current().getGameProps().getProperty("bulletFreq"));
    private String currentRoomName = ShadowDungeon.current().getCurrRoomName();



    public Player(Point position) {
        this.position = position;
        this.currImage = RIGHT_IMAGE;
        this.speed = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("movingSpeed"));
        this.health = Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("initialHealth"));
        this.key = 0;
        this.weaponLevel = 0;
        this.haveChoose = false;

    }
    /**
     * Per-frame update: handles movement (WASD), facing (mouse),
     * character sprite, shooting cooldown, and firing.
     * @param input Bagel input state
     */
    public void update(Input input) {
        // check movement keys and mouse cursor
        double currX = position.x;
        double currY = position.y;

        if (input.isDown(Keys.A)) {
            currX -= speed;
        }
        if (input.isDown(Keys.D)) {
            currX += speed;
        }
        if (input.isDown(Keys.W)) {
            currY -= speed;
        }
        if (input.isDown(Keys.S)) {
            currY += speed;
        }

        faceLeft = input.getMouseX() < currX;

        // update the player position accordingly and ensure it can't move past the game window
        Rectangle rect = currImage.getBoundingBoxAt(new Point(currX, currY));
        Point topLeft = rect.topLeft();
        Point bottomRight = rect.bottomRight();
        if (topLeft.x >= 0 && bottomRight.x <= Window.getWidth() && topLeft.y >= 0 && bottomRight.y <= Window.getHeight()) {
            move(currX, currY);
        }
        //choose character
        if(currentCharacter == CharacterType.ROBOT){
            currImage = faceLeft ? ROBOT_LEFT : ROBOT_RIGHT;

        }
        if(currentCharacter ==  CharacterType.MARINE){
            currImage = faceLeft ? MARINE_LEFT : MARINE_RIGHT;
        }
        //cooldown
        if (fireCooldown > 0) fireCooldown--;
        //shooting
        if(input.isDown(MouseButtons.LEFT)&&haveChoose&&fireCooldown == 0){
            double dx = input.getMouseX() - position.x;
            double dy = input.getMouseY() - position.y;
            double distance = Math.hypot(dx,dy);
            if (distance == 0) return;
            Vector<Double> dir = new Vector<>(2);
            dir.add(dx/distance);
            dir.add(dy/distance);
            ShadowDungeon.current().spawnBullet(position,dir,weaponLevel);
            fireCooldown = fireFreq;
        }
    }
    
    public void move(double x, double y) {
        prevPosition = position;
        position = new Point(x, y);
    }
    /** Renders the current player sprite and HUD stats. */

    public void draw() {
        if(!haveChoose){
            currImage = faceLeft ? LEFT_IMAGE : RIGHT_IMAGE; // NOTE: this is an example of using the ternary operator
        }
        else{
            if(currentCharacter == CharacterType.ROBOT){
                currImage = faceLeft ? ROBOT_LEFT : ROBOT_RIGHT;
            }
            if(currentCharacter == CharacterType.MARINE){
                currImage = faceLeft ? MARINE_LEFT : MARINE_RIGHT;
            }

        }
        currImage.draw(position.x, position.y);
        UserInterface.drawStats(health, coins, key,weaponLevel);

    }

    public void earnCoins(double coins) {
        this.coins += coins;
    }

    public void receiveDamage(double damage) {
        health -= damage;
        if (health <= 0) {
            ShadowDungeon.current().changeToGameOverRoom();
        }
    }

    public Point getPosition() {
        return position;
    }

    public Image getCurrImage() {
        return currImage;
    }

    public Point getPrevPosition() {
        return prevPosition;
    }

    public void setCharacterIsMarine() {
        this.currentCharacter = CharacterType.MARINE;
        this.haveChoose = true;
    }
    public void setCharacterIsRobot() {
        this.currentCharacter = CharacterType.ROBOT;
        this.haveChoose = true;
    }

    public double getHealth() {
        return this.health;
    }

    public void addKey() {
        this.key+=1;
    }
    public void addHealth(String config) {
        this.health +=Integer.parseInt(config);

    }

    public void spendCoins(String config) {
        this.coins -=Integer.parseInt(config);
    }

    public double getCoins() {
        return this.coins;
    }

    public double getWeaponLevel() {
        return this.weaponLevel;

    }
    /**
     * Upgrades the weapon by one level if affordable and below max.
     * @param cost coin cost per upgrade
     */
    public void upgradeWeapon(double cost){
            coins -= cost;
            weaponLevel = weaponLevel + 1;


    }

    public double getKeys() {
        return this.key;
    }

    public boolean getChoose() {
        return this.haveChoose;
    }
    /** @return current character type, or null if not selected */
    public CharacterType getCurrentCharacter() {
        return currentCharacter;
    }

    public String getCurrentRoomName() {
        return currentRoomName;
    }

    public void setCurrentRoomName(String currentRoomName) {
        this.currentRoomName = currentRoomName;
    }

    public void loseHealth(double damage) {
        this.health -= damage;
    }
    /** Decrements key count by one (on treasure unlock). */
    public void spendKey() {
        this.key -= 1;
    }
    /** @return current hit box for collision checks. */
    public Rectangle getHitBox(){
        return currImage.getBoundingBoxAt(position);
    }

}

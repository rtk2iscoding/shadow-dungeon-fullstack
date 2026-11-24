import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
/**
 * Room with doors that are locked until the plaer defeats all enemies
 */
public class BattleRoom implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final WeaponManage wm;
    private Player player;
    private Door primaryDoor;
    private Door secondaryDoor;
    private KeyBulletKin keyBulletKin;
    private ArrayList<AshenBulletKin> ashenBulletKins;
    private ArrayList<BulletKin> bulletKins;
    private ArrayList<TreasureBox> treasureBoxes;
    private ArrayList<Wall> walls;
    private ArrayList<River> rivers;
    private Key key;
    private ArrayList<Basket>baskets;
    private ArrayList<Table>tables;
    private boolean stopCurrentUpdateCall = false; // this determines whether to prematurely stop the update execution
    private boolean isComplete = false;
    private final String nextRoomName;
    private final String roomName;
    private final ArrayList<Obstacle> obstacles = new ArrayList<>();
    private final ArrayList<Damageable> enemys = new ArrayList<>();

    public BattleRoom(String roomName, String nextRoomName) {
        walls = new ArrayList<>();
        rivers = new ArrayList<>();
        treasureBoxes = new ArrayList<>();
        baskets = new ArrayList<>();
        tables = new ArrayList<>();
        ashenBulletKins = new ArrayList<>();
        bulletKins = new ArrayList<>();
        this.roomName = roomName;
        this.nextRoomName = nextRoomName;
        this.wm = new WeaponManage();
    }
    public void addTarget(Obstacle o) {
        if (o != null) obstacles.add(o);
    }
    public void addEnemy(Damageable d){
        if(d != null)enemys.add(d);
    }

    public void initEntities(Properties gameProperties) {
        // find the configuration of game objects for this room
        for (Map.Entry<Object, Object> entry: gameProperties.entrySet()) {
            String roomSuffix = String.format(".%s", roomName);

            if (entry.getKey().toString().contains(roomSuffix)) {
                String objectType = entry.getKey().toString()
                        .substring(0, entry.getKey().toString().length() - roomSuffix.length());
                String propertyValue = entry.getValue().toString();

                // ignore if the value is 0
                if (propertyValue.equals("0")) {
                    continue;
                }

                String[] coordinates;
                for (String coords: propertyValue.split(";")) {
                    switch (objectType) {
                        case "primarydoor":
                            coordinates = propertyValue.split(",");
                            primaryDoor = new Door(IOUtils.parseCoords(propertyValue), coordinates[2], this);
                            addTarget(primaryDoor);
                            break;
                        case "secondarydoor":
                            coordinates = propertyValue.split(",");
                            secondaryDoor = new Door(IOUtils.parseCoords(propertyValue), coordinates[2], this);
                            addTarget(secondaryDoor);
                            break;
                        case "keyBulletKin":
                            Point p = IOUtils.parseCoords(coords);
                            if (keyBulletKin == null) {
                                keyBulletKin = new KeyBulletKin(p);
                                key = new Key(p);
                                keyBulletKin.addWaypoint(p);
                                addEnemy(keyBulletKin);
                            } else {
                                keyBulletKin.addWaypoint(p);
                            }
                            break;
                        case "ashenBulletKin":
                            AshenBulletKin ashenBulletKin = new AshenBulletKin(IOUtils.parseCoords(coords));
                            ashenBulletKins.add(ashenBulletKin);
                            addEnemy(ashenBulletKin);
                            break;
                        case "bulletKin":
                            BulletKin bulletKin = new BulletKin(IOUtils.parseCoords(coords));
                            bulletKins.add(bulletKin);
                            addEnemy(bulletKin);
                            break;
                        case "wall":
                            Wall wall = new Wall(IOUtils.parseCoords(coords));
                            walls.add(wall);
                            addTarget(wall);
                            break;
                        case "treasurebox":
                            TreasureBox treasureBox = new TreasureBox(IOUtils.parseCoords(coords),
                                    Double.parseDouble(coords.split(",")[2]));
                            treasureBoxes.add(treasureBox);
                            break;
                        case "river":
                            River river = new River(IOUtils.parseCoords(coords));
                            rivers.add(river);

                            break;
                        case "basket":
                            Basket basket = new Basket(IOUtils.parseCoords(coords),
                                    Double.parseDouble(ShadowDungeon.current().getGameProps().getProperty("basketCoin")));
                            baskets.add(basket);
                            addTarget(basket);
                            break;
                        case "table":
                            Table table = new Table(IOUtils.parseCoords(coords));
                            tables.add(table);
                            addTarget(table);
                            break;

                        default:
                    }
                }
            }
        }
    }

    public void update(Input input) {
        // update and draw all active game objects in this room
        primaryDoor.update(player);
        primaryDoor.draw();
        if (stopUpdatingEarlyIfNeeded()) {
            return;
        }

        secondaryDoor.update(player);
        secondaryDoor.draw();
        if (stopUpdatingEarlyIfNeeded()) {
            return;
        }
        if (keyBulletKin.isActive()) {
            keyBulletKin.update(player);
            keyBulletKin.draw();
        }
        for(BulletKin bulletkin: bulletKins) {
            if(bulletkin.isActive()) {
                bulletkin.update(player);
                bulletkin.draw();
            }
        }
        for(AshenBulletKin ashenBulletKin: ashenBulletKins) {
            if(ashenBulletKin.isActive()) {
                ashenBulletKin.update(player);
                ashenBulletKin.draw();
            }
        }

        for (Basket basket: baskets) {
            basket.update(player);
            basket.draw();
        }
        for(Table table: tables) {
            table.update(player);
            table.draw();
        }

        for (Wall wall: walls) {
            wall.update(player);
            wall.draw();
        }

        for (River river: rivers) {
            river.update(player);
            river.draw();
        }

        for (TreasureBox treasureBox: treasureBoxes) {
            if (treasureBox.isActive()) {
                treasureBox.update(input, player);
                treasureBox.draw();
            }
        }

        if(keyBulletKin.isDead()){
            key.setPosition(keyBulletKin.getPosition());
            key.setAppear();
            key.update(player);
        }

        if (noMoreEnemies() && !isComplete() && allEnemiesDead()) {
            setComplete(true);
            unlockAllDoors();
        }
        wm.update(obstacles,enemys,player);
        wm.updateFireBall(obstacles,player);
        if (player != null) {
            player.update(input);
            player.draw();
        }
    }

    private boolean stopUpdatingEarlyIfNeeded() {
        if (stopCurrentUpdateCall) {
            player = null;
            wm.clear();
            stopCurrentUpdateCall = false;
            return true;
        }
        return false;
    }

    public void stopCurrentUpdateCall() {
        wm.clear();
        stopCurrentUpdateCall = true;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Door findDoorByDestination(String roomName) {
        if (primaryDoor.toRoomName.equals(roomName)) {
            return primaryDoor;
        } else {
            return secondaryDoor;
        }
    }

    private void unlockAllDoors() {
        primaryDoor.unlock(false);
        secondaryDoor.unlock(false);
    }


    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public void activateEnemies() {
        keyBulletKin.setActive(true);
        for(BulletKin bulletKin: bulletKins) {
            bulletKin.setActive(true);
        }
       for(AshenBulletKin  ashenBulletKin: ashenBulletKins) {
           ashenBulletKin.setActive(true);
       }
    }

    public boolean noMoreEnemies() {
        keyBulletKin.isDead();
        for(BulletKin bulletKin: bulletKins) {
            bulletKin.isDead();
        }
        for(AshenBulletKin ashenBulletKin: ashenBulletKins) {
            ashenBulletKin.isDead();
        }
        return keyBulletKin.isDead();
    }

    public void draw() {
        //player.draw();
        primaryDoor.draw();
        secondaryDoor.draw();
        if(keyBulletKin.isActive()) {
            keyBulletKin.draw();
        }
        for (BulletKin bulletKin: bulletKins) {
            if(bulletKin.isActive()) {
                bulletKin.draw();
            }
        }
        for (AshenBulletKin ashenBulletKin:ashenBulletKins ){
            if(ashenBulletKin.isActive()){
                ashenBulletKin.draw();
            }
        }
        for (Basket basket: baskets) {
            if(basket.isActive()) {
                basket.draw();
            }

        }
        for (Table table: tables) {
            if(!table.isDead()){
                table.draw();
            }

        }
        for (Wall wall: walls) {
            wall.draw();
        }
        for (River river: rivers) {
            river.draw();
        }
        for (TreasureBox treasureBox: treasureBoxes) {
            if(treasureBox.isActive()) {
                treasureBox.draw();
            }
        }
        if(key.isAppear()){
            key.draw();
        }
        wm.draw();
    }
    public void spawnBullet(Point positon, Vector<Double> dir, double weaponLevel) {
        wm.spawnBullet(positon, dir,weaponLevel);
    }
    public boolean allEnemiesDead(){
        for(AshenBulletKin ashenBulletKin: ashenBulletKins) {
            for (BulletKin bulletKin: bulletKins) {
                if(!bulletKin.isDead()||!ashenBulletKin.isDead()||!keyBulletKin.isDead()){
                    return false;
                }
            }
        }
    return true;
    }


    public void spawnFireBall(Point position, Vector<Double> dir) {
        wm.spawnFireBall(position, dir);
    }
}

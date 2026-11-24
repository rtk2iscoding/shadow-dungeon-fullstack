import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * Room where the game starts
 */
public class PrepRoom {
    private Player player;
    private Door door;
    private RestartArea restartArea;
    private boolean stopCurrentUpdateCall = false; // this determines whether to prematurely stop the update execution
    private TheMarine marine;
    private TheRobot robot;
    private final WeaponManage wm = new WeaponManage();
    public void initEntities(Properties gameProperties) {
        // find the configuration of game objects for this room
        for (Map.Entry<Object, Object> entry: gameProperties.entrySet()) {
            String roomSuffix = String.format(".%s", ShadowDungeon.PREP_ROOM_NAME);
            String objectType;
            String propertyValue;
            if (entry.getKey().toString().contains(roomSuffix)) {
                objectType = entry.getKey().toString().substring(0, entry.getKey().toString().length() - roomSuffix.length());
                propertyValue = entry.getValue().toString();
            }
            else{
                if (entry.getKey().toString().contains(".")) {
                    continue;
                }
                objectType = entry.getKey().toString();
                propertyValue = entry.getValue().toString();
            }
                switch (objectType) {
                    case "door":
                        String[] coordinates = propertyValue.split(",");
                        door = new Door(IOUtils.parseCoords(propertyValue), coordinates[2]);
                        break;
                    case "restartarea":
                        restartArea = new RestartArea(IOUtils.parseCoords(propertyValue));
                        break;
                    case "Marine":
                        marine = new TheMarine(IOUtils.parseCoords(propertyValue));
                        break;
                    case "Robot":
                        robot = new  TheRobot(IOUtils.parseCoords(propertyValue));
                        break;
                    default:
                }

        }
    }

    public void update(Input input) {
        UserInterface.drawStartMessages();

        // update and draw all game objects in this room
        door.update(player);
        door.draw();
        if (stopUpdatingEarlyIfNeeded()) {
            return;
        }
        //character choose
        robot.draw();
        marine.draw();

        restartArea.update(input, player);
        restartArea.draw();


        if (player != null) {
            player.update(input);
            player.draw();
        }

        // door unlock mechanism
        if (player.getChoose()&& !findDoor().isUnlocked()) {
            findDoor().unlock(false);
        }
        //choose character
        if(player != null) {
            if (input.wasPressed(Keys.M)) {
                marine.choose = true;
                robot.choose = false;
                player.setCharacterIsMarine();
            }
            if (input.wasPressed(Keys.R)) {
                marine.choose = false;
                robot.choose = true;
                player.setCharacterIsRobot();
            }
            wm.update();
        }



    }

    private boolean stopUpdatingEarlyIfNeeded() {
        if (stopCurrentUpdateCall) {
            player = null;
            stopCurrentUpdateCall = false;
            wm.clear();
            return true;
        }
        return false;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void stopCurrentUpdateCall() {
        stopCurrentUpdateCall = true;
    }

    public Door findDoor() {
        return door;
    }

    public Door findDoorByDestination() {
        return door;
    }


    public void spawnBullet(Point positon, Vector<Double> dir, double weaponLevel) {
        wm.spawnBullet(positon, dir, weaponLevel);
    }
    public void draw(){
        UserInterface.drawStartMessages();
        door.draw();
        restartArea.draw();
        marine.draw();
        robot.draw();
        wm.draw();
    }
}

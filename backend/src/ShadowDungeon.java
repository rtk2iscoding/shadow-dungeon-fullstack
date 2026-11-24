import java.util.Properties;
import java.util.Vector;

/**
 * Main game class that manages initialising the rooms and moving the player between rooms
 */
public class ShadowDungeon {
    private final Properties gameProps;
    private final Properties messageProps;

    private String currRoomName;
    private PrepRoom prepRoom;
    private BattleRoom battleRoomA;
    private BattleRoom battleRoomB;
    private EndRoom endRoom;
    private Player player;
    private final Image background;
    private final Store store;
    
    public static final String PREP_ROOM_NAME = "prep";
    public static final String BATTLE_ROOM_A_NAME = "A";
    public static final String BATTLE_ROOM_B_NAME = "B";
    public static final String END_ROOM_NAME = "end";

    private boolean paused = false;

    public ShadowDungeon(Properties gameProps, Properties messageProps) {
        this.gameProps = gameProps;
        this.messageProps = messageProps;
        this.background = new Image("res/background.png");
        store = new Store(gameProps.getProperty("store"));
        CURRENT.set(this);
        try {
            resetGameState(gameProps);
        } finally {
            CURRENT.remove();
        }
    }

    private static final ThreadLocal<ShadowDungeon> CURRENT = new ThreadLocal<>();

    static ShadowDungeon current() {
        return CURRENT.get();
    }

    String frame(Input input, int ticks) {
        CURRENT.set(this);
        try {
            for (int i = 1; i < ticks; i++) {
                Frame.discard();
                update(i == 1 ? input : input.withoutPresses());
            }
            Frame.begin();
            update(ticks == 1 ? input : input.withoutPresses());
            return Frame.finish(getCurrRoomName());
        } finally {
            Frame.discard();
            CURRENT.remove();
        }
    }

    public void resetGameState(Properties gameProps) {
        prepRoom = new PrepRoom();
        battleRoomA = new BattleRoom(BATTLE_ROOM_A_NAME, BATTLE_ROOM_B_NAME);
        battleRoomB = new BattleRoom(BATTLE_ROOM_B_NAME, END_ROOM_NAME);
        endRoom = new EndRoom();

        prepRoom.initEntities(gameProps);
        battleRoomA.initEntities(gameProps);
        battleRoomB.initEntities(gameProps);
        endRoom.initEntities(gameProps);

        currRoomName = PREP_ROOM_NAME;

        player = new Player(IOUtils.parseCoords(gameProps.getProperty("player.start")));
        prepRoom.setPlayer(player);
    }

    public void spawnBullet(Point position, Vector<Double> dir, double weaponLevel) {
        switch (currRoomName) {
            case PREP_ROOM_NAME:
                prepRoom.spawnBullet(position,dir,weaponLevel);
                return;
            case BATTLE_ROOM_A_NAME:
                battleRoomA.spawnBullet(position,dir,weaponLevel);
                return;
            case BATTLE_ROOM_B_NAME:
                battleRoomB.spawnBullet(position,dir,weaponLevel);
                return;
            default:
                endRoom.spawnBullet(position,dir,weaponLevel);
        }
    }

    public void spawnFireBall(Point position, Vector<Double> dir) {
        switch (currRoomName) {
            case BATTLE_ROOM_A_NAME:
                battleRoomA.spawnFireBall(position,dir);
                return;
            case BATTLE_ROOM_B_NAME:
                battleRoomB.spawnFireBall(position,dir);
                return;
            default:
        }
    }


    /**
     * Render the relevant screen based on the keyboard input given by the user and the status of the gameplay.
     * @param input The current mouse/keyboard input.
     */
    private void update(Input input) {
        if (input.wasPressed(Keys.SPACE)) {
            paused = !paused;
            if (paused) {
                store.appear();
            } else {
                store.disappear();
            }
        }

        background.draw((double) Window.getWidth() / 2, (double) Window.getHeight() / 2);
        if(paused) {
            switch (currRoomName) {
                case PREP_ROOM_NAME:
                    prepRoom.draw();
                    break;
                case BATTLE_ROOM_A_NAME:
                    battleRoomA.draw();
                    break;
                case BATTLE_ROOM_B_NAME:
                    battleRoomB.draw();
                    break;
                default:
                    endRoom.draw();
            }
            player.draw();
            store.draw();
            store.update(input,player);
            return;
        }

        switch (currRoomName) {
            case PREP_ROOM_NAME:
                prepRoom.update(input);
                return;
            case BATTLE_ROOM_A_NAME:
                battleRoomA.update(input);
                return;
            case BATTLE_ROOM_B_NAME:
                battleRoomB.update(input);
                return;
            default:
                endRoom.update(input);
        }


    }

    public void changeRoom(String roomName) {
        Door nextDoor;
        switch (roomName) {
            case PREP_ROOM_NAME:
                nextDoor = prepRoom.findDoorByDestination();

                // assume that prep room can only be entered through Battle Room A
                if (currRoomName.equals(BATTLE_ROOM_A_NAME)) {
                    battleRoomA.stopCurrentUpdateCall();
                }
                currRoomName = PREP_ROOM_NAME;

                // move the player to the center of the next room's door
                nextDoor.unlock(true);
                player.move(nextDoor.getPosition().x, nextDoor.getPosition().y);
                prepRoom.setPlayer(player);

                return;
            case BATTLE_ROOM_A_NAME:
                nextDoor = battleRoomA.findDoorByDestination(currRoomName);

                // assume that Battle Room A can only be entered through Prep Room or Battle Room B
                if (currRoomName.equals(BATTLE_ROOM_B_NAME)) {
                    battleRoomB.stopCurrentUpdateCall();
                } else if (currRoomName.equals(PREP_ROOM_NAME)) {
                    prepRoom.stopCurrentUpdateCall();
                }
                currRoomName = BATTLE_ROOM_A_NAME;

                // prepare the door to be able to activate the Battle Room
                if (!battleRoomA.isComplete()) {
                    nextDoor.setShouldLockAgain();
                }

                // move the player to the center of the next room's door
                nextDoor.unlock(true);
                player.move(nextDoor.getPosition().x, nextDoor.getPosition().y);
                battleRoomA.setPlayer(player);

                return;
            case BATTLE_ROOM_B_NAME:
                nextDoor = battleRoomB.findDoorByDestination(currRoomName);

                // assume that Battle Room B can only be entered through Battle Room A or End Room
                if (currRoomName.equals(BATTLE_ROOM_A_NAME)) {
                    battleRoomA.stopCurrentUpdateCall();
                } else if (currRoomName.equals(END_ROOM_NAME)) {
                    endRoom.stopCurrentUpdateCall();
                }
                currRoomName = BATTLE_ROOM_B_NAME;

                // prepare the door to be able to activate the Battle Room
                if (!battleRoomB.isComplete()) {
                    nextDoor.setShouldLockAgain();
                }

                // move the player to the center of the next room's door
                nextDoor.unlock(true);
                player.move(nextDoor.getPosition().x, nextDoor.getPosition().y);
                battleRoomB.setPlayer(player);

                return;
            default:
                nextDoor = endRoom.findDoorByDestination();

                // assume that end room can only be entered through Battle Room B
                if (currRoomName.equals(BATTLE_ROOM_B_NAME)) {
                    battleRoomB.stopCurrentUpdateCall();
                }
                currRoomName = END_ROOM_NAME;

                // move the player to the center of the next room's door
                nextDoor.unlock(true);
                player.move(nextDoor.getPosition().x, nextDoor.getPosition().y);
                endRoom.setPlayer(player);
        }
    }

    public void changeToGameOverRoom() {
        switch (currRoomName) {
            case PREP_ROOM_NAME:
                prepRoom.stopCurrentUpdateCall();
                break;
            case BATTLE_ROOM_A_NAME:
                battleRoomA.stopCurrentUpdateCall();
                break;
            case BATTLE_ROOM_B_NAME:
                battleRoomB.stopCurrentUpdateCall();
                break;
            default:
        }

        endRoom.isGameOver();
        currRoomName = END_ROOM_NAME;

        Point startPos = IOUtils.parseCoords(getGameProps().getProperty("player.start"));
        player.move(startPos.x, startPos.y);
        endRoom.setPlayer(player);
    }


    public Properties getGameProps() {
        return gameProps;
    }
    public Properties getMessageProps() {
        return messageProps;
    }
    public Player getPlayer() { return player; }

    public String getCurrRoomName() {
        return currRoomName;
    }
}

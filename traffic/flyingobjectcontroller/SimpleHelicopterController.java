package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;
import info.flowersoft.theotown.theotown.util.DataAccessor;

/**
 * Created by Lobby on 05.08.2016.
 */
public abstract class SimpleHelicopterController extends FlyingObjectController {

    public SimpleHelicopterController(City city, FlyingObjectSpawner spawner) {
        super(city, spawner);
    }

    @Override
    public final void onTarget(FlyingObject flyingObject) {
        int x = flyingObject.getActualX();
        int y = flyingObject.getActualY();
        int height = flyingObject.getActualHeight();
        int targetX = getStateTargetX(flyingObject);
        int targetY = getStateTargetY(flyingObject);
        int targetHeight = getStateTargetHeight(flyingObject);
        boolean targetLand = getStateTargetLand(flyingObject);
        int minHeight = city.getTileHeight(x, y);
        targetHeight = Math.max(minHeight, targetHeight);
        boolean finished = getStateFinished(flyingObject);
        int oldDir = flyingObject.getDir();

        if (!finished) {
            targetHeight = Math.max(targetHeight, minHeight + (x != targetX || y != targetY || !targetLand ? 32 : 0));
            targetHeight = Math.max(targetHeight, (x != targetX || y != targetY || !targetLand ? 48 : 0));
            if (targetHeight < height - 16) {
                targetHeight = height - 16;
            }
            if (targetHeight > height + 16) {
                targetHeight = height + 16;
            }

            if (x != targetX || y != targetY || height != targetHeight) {
                int dx = targetX - x;
                int dy = targetY - y;
                if (Math.abs(dx) + Math.abs(dy) > 0) {
                    int dir0 = dx != 0 ? Direction.fromDifferential(dx, 0) : 0;
                    int dir1 = dy != 0 ? Direction.fromDifferential(0, dy) : 0;

                    int dir = 1;
                    if (oldDir != 0) {
                        if (dir0 == oldDir) {
                            dir = dir0;
                        } else if (dir1 == oldDir) {
                            dir = dir1;
                        } else if (Direction.turnCCW(oldDir) == dir0 || Direction.turnCCW(oldDir) == dir1) {
                            dir = Direction.turnCCW(oldDir);
                        } else if (Direction.rotateCW(oldDir, Direction.ROTATION_90_DEGREE) == dir0
                                || Direction.rotateCW(oldDir, Direction.ROTATION_90_DEGREE) == dir1) {
                            dir = Direction.rotateCW(oldDir, Direction.ROTATION_90_DEGREE);
                        } else {
                            dir = Direction.turnCCW(oldDir);
                        }
                    }

                    flyingObject.flyTo(dir, dir, targetHeight);
                } else {
                    flyingObject.flyTo(0, oldDir, targetHeight);
                }
                flyingObject.setSpeed(Math.min(flyingObject.getSpeed() + 0.1f, 1));
            } else {
                setState(flyingObject, x, y, targetHeight, targetLand, true);
                finishedFlightCommand(flyingObject);
            }
        } else {
            if (targetLand) {
                flyingObject.setSpeed(Math.max(flyingObject.getSpeed() - 0.1f, 0));
                flyingObject.flyTo(0, oldDir, height);
            } else {
                targetHeight = Math.max(targetHeight, minHeight + 32);
                targetHeight = Math.max(targetHeight, 48);
                if (height == targetHeight) {
                    targetHeight += 4;
                }
                flyingObject.flyTo(0, oldDir, targetHeight);
            }
        }
    }

    protected abstract void finishedFlightCommand(FlyingObject flyingObject);

    @Override
    public void onSpawned(FlyingObject flyingObject) {

    }

    @Override
    public void update() {

    }

    protected final FlyingObject create(FlyingObjectDraft draft, int x, int y) {
        int height = city.getTileHeight(x, y);
        FlyingObject flyingObject = spawner.spawn(draft, x, y, 1, height);
        setState(flyingObject, x, y, height, true, true);
        return flyingObject;
    }

    protected final void flyTo(FlyingObject flyingObject, int x, int y, boolean land) {
        setState(flyingObject, x, y, city.getTileHeight(x, y), land, false);
        flyingObject.setSpeed(Math.max(0.5f, flyingObject.getSpeed()));
    }

    private void setState(FlyingObject flyingObject, int tx, int ty, int th, boolean land, boolean finished) {
        long data = flyingObject.data;
        data = DataAccessor.write(data, 10, 22, tx);
        data = DataAccessor.write(data, 10, 12, ty);
        data = DataAccessor.write(data, 10, 2, th);
        data = DataAccessor.write(data, 1, 1, land ? 1 : 0);
        data = DataAccessor.write(data, 1, 0, finished ? 1 : 0);
        flyingObject.data = data;
    }

    private int getStateTargetX(FlyingObject flyingObject) {
        return (int) DataAccessor.read(flyingObject.data, 10, 22);
    }

    private int getStateTargetY(FlyingObject flyingObject) {
        return (int) DataAccessor.read(flyingObject.data, 10, 12);
    }

    private int getStateTargetHeight(FlyingObject flyingObject) {
        return (int) DataAccessor.read(flyingObject.data, 10, 2);
    }

    private boolean getStateTargetLand(FlyingObject flyingObject) {
        return DataAccessor.read(flyingObject.data, 1, 1) == 1;
    }

    private boolean getStateFinished(FlyingObject flyingObject) {
        return DataAccessor.read(flyingObject.data, 1, 0) == 1;
    }

    @Override
    public String getTag() {
        return "simple helicopter";
    }
}

package info.flowersoft.theotown.theotown.components.traffic;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.components.traffic.traincontroller.DefaultTrainController;
import info.flowersoft.theotown.theotown.components.traffic.traincontroller.TrainController;
import info.flowersoft.theotown.theotown.components.traffic.traincontroller.TrainSpawner;
import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.TrainDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.Drawer;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.TrainPathfinding;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.components.Date;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Rail;
import info.flowersoft.theotown.theotown.map.objects.Train;
import info.flowersoft.theotown.theotown.stapel2d.util.CyclicWorker;

/**
 * Created by Lobby on 10.06.2016.
 */
public class TrainTrafficHandler extends TrafficHandler {

    private static int[] DRAW_INDEX_ORDER = new int[] {
            2, 0, 3, 1,
            0, 2, 1, 3,
            1, 3, 0, 2,
            3, 1, 2, 0 };

    private List<Train> trains;

    private List<TrainController> controllers;

    private TrainPathfinding trainPathfinding;

    private Executor pathfindingExecutor;

    private Date date;

    public TrainTrafficHandler(City city) {
        super(city);

        trains = new ArrayList<>();
        controllers = new ArrayList<>();

        controllers.add(new DefaultTrainController(city, new TrainSpawner() {
            @Override
            public void spawn(TrainDraft draft, int x, int y, int dir, int level, long data) {
                TrainTrafficHandler.this.spawnTrain(draft, x, y, dir, level, data, 0);
            }

            @Override
            public void driveTo(Train train, int x, int y) {
                TrainTrafficHandler.this.driveTrainTo(train, x, y);
            }

            @Override
            public void remove(Train train) {
                TrainTrafficHandler.this.removeTrain(train);
            }
        }));

        trainPathfinding = new TrainPathfinding(city);
        pathfindingExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void load(JsonReader src) throws IOException {
        while (src.hasNext()) {
            switch (src.nextName()) {
                case "trains":
                    src.beginArray();
                    while (src.hasNext()) {
                        src.beginObject();
                        Train train = new Train(src, city);
                        trains.add(train);
                        src.endObject();
                    }
                    src.endArray();
                    break;
                default:
                    src.skipValue();
            }
        }
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
        dest.name("trains").beginArray();
        for (Train train : trains) {
            if (train.isValid()) {
                dest.beginObject();
                train.save(dest);
                dest.endObject();
            }
        }
        dest.endArray();
    }

    @Override
    public void prepare() {
        date = (Date) city.getComponent(ComponentType.DATE);

        for (Train train : trains) {
            getController(train).registerTrain(train);
        }

        addDaylyWorker(new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                synchronized (trains) {
                    for (int i = 0; i < controllers.size(); i++) {
                        controllers.get(i).update();
                    }
                }
            }
        }));

        addWorker(new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                synchronized (trains) {
                    int timeDelta = date.getAnimationTimeDelta();
                    float time = timeDelta / 1000f;
                    float progress = timeDelta / 300f;

                    for (int i = 0; i < trains.size(); i++) {
                        Train train = trains.get(i);
                        int x = train.getX();
                        int y = train.getY();
                        int dir = train.getCurrentDir();
                        int nx = x + Direction.differenceX(dir);
                        int ny = y + Direction.differenceY(dir);
                        Tile tile = city.getTile(x / 2, y / 2);
                        Tile nTile = city.getTile(nx / 2, ny / 2);
                        Building building = tile.building;
                        unregisterTrain(train);

                        boolean incrementSpeed = true;
                        boolean reachedTarget = false;
                        float localProgress = progress;

                        if (building != null && building.getBuildingType() == BuildingType.RAILWAY_STATION) {
                            getController(train).visitStation(train, building);
                        }

                        while (localProgress > 0 && !reachedTarget) {
                            reachedTarget |= train.move(Math.min(localProgress, 1));
                            localProgress--;
                        }

                        if (reachedTarget) {
                            getController(train).onTarget(train);
                        } else {
                            x = train.getX();
                            y = train.getY();
                            tile = city.getTile(x / 2, y / 2);
                            if (tile.trains[train.getGroundLevel()][(x % 2) + 2 * (y % 2)] != null) {
                                incrementSpeed = false;
                            }
                        }

                        if (nTile != null && train.isValid()) {
                            if (nTile.trains[train.getGroundLevel()][(nx % 2) + 2 * (ny % 2)] != null) {
                                incrementSpeed = false;
                            }
                        }

                        if (train.isValid() && tile.rail[train.getGroundLevel()] == null) {
                            removeTrain(train);
                        }
                        if (train.isValid() && building != null) {
                            if (building.getBuildingType() == BuildingType.RAILWAY_STATION) {
                                incrementSpeed = false;
                            }
                        }

                        if (train.isValid()) {
                            if (incrementSpeed) {
                                train.incrementSpeed(time);
                            } else {
                                train.decrementSpeed(time);
                            }
                            registerTrain(train);
                        }
                    }
                }
            }
        }));
    }

    private void registerTrain(Train train) {
        int x = train.getX();
        int y = train.getY();

        registerTrain(train, x, y);

        int[] lastDirs = train.getLastDirs();
        for (int i = 0; i < lastDirs.length && lastDirs[i] != 0; i++) {
            int dir = lastDirs[i];
            x -= Direction.differenceX(dir);
            y -= Direction.differenceY(dir);
            registerTrain(train, x, y);
        }
    }

    private void registerTrain(Train train, int x, int y) {
        if (x >= 0 && y >= 0 && x < 2 * city.getWidth() && y < 2 * city.getHeight()) {
            int fx = x % 2;
            int fy = y % 2;
            int idx = fx + 2 * fy;
            city.getTile(x / 2, y / 2).trains[1][idx] = train;
        }
    }

    private void unregisterTrain(Train train) {
        int x = train.getX();
        int y = train.getY();

        unregisterTrain(train, x, y);

        int[] lastDirs = train.getLastDirs();
        for (int i = 0; i < lastDirs.length && lastDirs[i] != 0; i++) {
            int dir = lastDirs[i];
            x -= Direction.differenceX(dir);
            y -= Direction.differenceY(dir);
            unregisterTrain(train, x, y);
        }
    }

    private void unregisterTrain(Train train, int x, int y) {
        if (x >= 0 && y >= 0 && x < 2 * city.getWidth() && y < 2 * city.getHeight()) {
            int fx = x % 2;
            int fy = y % 2;
            int idx = fx + 2 * fy;
            city.getTile(x / 2, y / 2).trains[1][idx] = null;
        }
    }

    private void spawnTrain(TrainDraft draft, int x, int y, int dir, int level, long data, int controllerId) {
        Train train = new Train(draft, 2 * x, 2 * y, data, controllerId);
        train.setGroundLevel(level);
        train.pause(dir);
        trains.add(train);
        getController(train).registerTrain(train);
        getController(train).onSpawned(train);
    }

    private void driveTrainTo(final Train train, final int x, final int y) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                trainPathfinding.setStart(train.getX(), train.getY(), train.getCurrentDir());
                trainPathfinding.setTarget(x, y);
                trainPathfinding.setGroundLevel(train.getGroundLevel());
                trainPathfinding.calculate();

                synchronized (trains) {
                    if (trainPathfinding.hasResult()) {
                        getController(train).onFoundWay(train);
                        train.driveTo(trainPathfinding.getResult());
                    } else {
                        getController(train).onFoundNoWay(train);
                    }
                }
            }
        };

        pathfindingExecutor.execute(task);
    }

    private void removeTrain(Train train) {
        getController(train).unregisterTrain(train);
        trains.remove(train);
        train.setInvalid();
    }

    private TrainController getController(Train train) {
        return controllers.get(train.getControllerId());
    }

    public void draw(int x, int y, Tile tile, int groundLevel, Drawer d) {
        Rail rail = tile.rail[groundLevel];
        if (rail != null) {
            for (int i = 0; i < 4; i++) {
                int idx = DRAW_INDEX_ORDER[4 * d.rotation + i];
                Train train = tile.trains[groundLevel][idx];
                if (train != null) {
                    int ox = idx % 2;
                    int oy = idx / 2;
                    train.draw(d, ox, oy, rail);
                }
            }
        }
    }
}

package info.flowersoft.theotown.theotown.components.traffic;

import android.util.Log;

import info.flowersoft.theotown.theotown.components.DefaultWeather;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.BusController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.DefaultCarController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.FireEngineController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.GarbageTruckController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.GenericCarController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.HearseCarController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.MedicCarController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.PoliceCarController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.SWATCarController;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.TankController;
import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.RoadDraft;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.WinterManager;
import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import info.flowersoft.theotown.theotown.components.traffic.carcontroller.CarSpawner;
import info.flowersoft.theotown.theotown.components.traffic.carcontroller.CarController;
import info.flowersoft.theotown.theotown.draft.CarAnimationSpot;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.Drawer;
import info.flowersoft.theotown.theotown.map.MapArea;
import info.flowersoft.theotown.theotown.map.Pathfinding;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.components.Date;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.map.objects.Road;
import info.flowersoft.theotown.theotown.map.objects.RoadOccupationType;
import info.flowersoft.theotown.theotown.map.objects.Way;
import info.flowersoft.theotown.theotown.resources.Constants;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.stapel2d.util.CyclicWorker;

/**
 * Created by Lobby on 14.04.2016.
 */
public final class CarTrafficHandler extends TrafficHandler implements CarSpawner {

    private final static int MAX_PATHFINDING_TASKS = 256;

    private final static int NUMBER_OF_PATHFINDING_THREADS
            = Math.max(Runtime.getRuntime().availableProcessors() / 4, 1);

    private final static int[] ORDERED_PARCEL_ACCESS = {2, 3, 0, 1};
    private final static int[] CAR_FRAME_MAPPING = {1, 0, 3, 2};

    private final static int PATHFINDING_TIMEOUT = 60;
    private final static int PATHFINDING_IMPORTANT_TIMEOUT = 200;

    private final int width;
    private final int height;
    private final int levelStride;

    private final List<Car> cars = new LinkedList<>();
    private final Car[] carArray;
    private ExecutorService pathfindingExecutor;
    private AtomicInteger countPathfindingTasks;

    private List<CarController> carControllers = new ArrayList<>();

    private int carIdCounter;

    private TrafficAmountWorker trafficAmountWorker;
    private CyclicWorker updateCarsWorker;

    private int lastAnimationTime;

    public CarTrafficHandler(City city) {
        super(city);

        width = city.getWidth();
        height = city.getHeight();

        carIdCounter = 0;

        carArray = new Car[width * height * 4 * 2];
        levelStride = width * height * 4;

        addCarController(new DefaultCarController(this));
        addCarController(new BusController(this));
        addCarController(new FireEngineController(this));
        addCarController(new PoliceCarController(this));
        addCarController(new SWATCarController(this));
        addCarController(new MedicCarController(this));
        addCarController(new GarbageTruckController(this));
        addCarController(new HearseCarController(this));
        addCarController(new TankController(this));

        for (int i = 0; i < Drafts.BUILDINGS.size(); i++) {
            BuildingDraft buildingDraft = Drafts.BUILDINGS.get(i);
            if (buildingDraft.carSpawners != null) {
                for (int j = 0; j < buildingDraft.carSpawners.length; j++) {
                    addCarController(new GenericCarController(this, buildingDraft, buildingDraft.carSpawners[j]));
                }
            }
        }

        countPathfindingTasks = new AtomicInteger();
    }

    public void load(final JsonReader src) throws IOException {
        Map<String, CarController> nameToControllerMap = new HashMap<>();
        for (int i = 0; i < carControllers.size(); i++) {
            CarController controller = carControllers.get(i);
            nameToControllerMap.put(controller.getName(), controller);
        }

        CarController[] mapping = null;

        while (src.hasNext()) {
            String name = src.nextName();

            CarController carController = nameToControllerMap.get(name);

            if (carController != null) {
                src.beginObject();
                carController.load(src, city);
                src.endObject();
            } else {
                switch (name) {
                    case "controller mapping":
                        src.beginArray();
                        mapping = new CarController[src.nextInt()];
                        for (int i = 0; i < mapping.length; i++) {
                            mapping[i] = nameToControllerMap.get(src.nextString());
                        }
                        src.endArray();
                        break;
                    case "cars":
                        src.beginArray();
                        while (src.hasNext()) {
                            src.beginObject();
                            Car car = new Car(src, city);
                            src.endObject();

                            if (car.draft != null && car.way != null && car.way.size() > 0 && car.prog >= 0 && car.prog < car.way.size() && car.way.getDir(0) != 0) {
                                car.paused = false;
                                if (mapping != null) {
                                    if (car.controller >= 0 && car.controller < mapping.length
                                        && mapping[car.controller] != null) {
                                        car.controller = mapping[car.controller].getId();
                                    } else {
                                        car = null;
                                    }
                                }
                                if (car != null) {
                                    registerCar(car);
                                }
                            }
                        }
                        src.endArray();
                        break;
                    case "id counter":
                        carIdCounter = src.nextInt();
                        break;
                    default:
                        src.skipValue();
                }
            }
        }
    }

    public <T extends CarController> T getCarController(Class<T> type) {
        for (int i = 0; i < carControllers.size(); i++) {
            CarController controller = carControllers.get(i);
            if (controller.getClass().equals(type)) {
                return (T) controller;
            }
        }
        return null;
    }

    public List<Car> getCars() {
        return cars;
    }

    public List<Car> getCars(Road road) {
        List<Car> cars = new ArrayList<>(4);
        int x = road.getX();
        int y = road.getY();
        int level = road.level;
        for (int i = 0; i < 4; i++) {
            Car car = carArray[(x + y * width) * 4 + level * levelStride + i];
            if (car != null) cars.add(car);
        }
        return cars;
    }

    public Road getBadRoad() {
        return trafficAmountWorker.getBadRoad();
    }

    public void save(JsonWriter dest) throws IOException {
        synchronized (cars) {
            dest.name("controller mapping");
            dest.beginArray();
                dest.value(carControllers.size());
                for (int i = 0; i < carControllers.size(); i++) {
                    dest.value(carControllers.get(i).getName());
                }
            dest.endArray();

            dest.name("cars");
            dest.beginArray();
                for (Car car : cars) {
                    dest.beginObject();
                    car.save(dest);
                    dest.endObject();
                }
            dest.endArray();

            for (int i = 0; i < carControllers.size(); i++) {
                CarController controller = carControllers.get(i);
                dest.name(controller.getName());
                dest.beginObject();
                controller.save(dest);
                dest.endObject();
            }

            dest.name("id counter").value(carIdCounter);
        }
    }

    @Override
    public void prepare() {
        pathfindingExecutor = Executors.newFixedThreadPool(
                NUMBER_OF_PATHFINDING_THREADS,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        return new PathfindingThread(city, runnable);
                    }
                });

        trafficAmountWorker = new TrafficAmountWorker(city, carArray);
        addDaylyWorker(trafficAmountWorker);

        addDaylyWorker(new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < carControllers.size(); i++) {
                    CarController controller = carControllers.get(i);
                    synchronized (cars) {
                        controller.update();
                    }
                }
            }
        }));

        final Date date = (Date) city.getComponent(ComponentType.DATE);
        updateCarsWorker = new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                synchronized (cars) {
                    final int time = date.getAnimationTime();
                    int td = time - lastAnimationTime;
                    final int winter = WinterManager.isWinter() ? 1 : 0;
                    if (td < 0 || td > 1000) td = 50;
                    final int timeDelta = td;
                    lastAnimationTime = time;

                    float generalSpeedFactor = 1;
                    DefaultWeather weather = ((DefaultWeather) city.getComponent(ComponentType.WEATHER));
                    if (weather.isFogEnabled()) generalSpeedFactor *= 0.7f;
                    if (weather.getRain() >= 0.8f) generalSpeedFactor *= 0.7f;
                    final float finalGeneralSpeedFactor = generalSpeedFactor;

                    updateCars(cars.iterator(), time, winter, finalGeneralSpeedFactor, timeDelta);
                }
            }
        });
        addWorker(updateCarsWorker);

        for (int i = 0; i < carControllers.size(); i++) {
            CarController controller = carControllers.get(i);
            controller.prepare(city);
        }
    }

    private void updateCars(Iterator<Car> it, int time, int winter, float generalSpeedFactor, int timeDelta) {
        int notWinter = 1 - winter;

        while (it.hasNext()) {
            Car car = it.next();

            if (car.invalid
                    || car.x < 0 || car.y < 0 || car.x >= 2 * width || car.y >= 2 * height) {
                unregisterCar(car);
                it.remove();
                car.invalid = true;
                continue;
            }

            int lastX = car.x;
            int lastY = car.y;
            int lastLevel = car.level;
            Tile tile = city.getTile(car.x / 2, car.y / 2);
            Road road = tile.getRoad(car.level);
            int deltaProg = 0;
            float lastLastP = car.p;

            if ((time - car.lastMovement >= 20000 && car.lastMovement > 0) || road == null) {
                unregisterCar(car);
                it.remove();
                car.invalid = true;
                continue;
            }

            RoadDraft roadDraft = road.draft;
            float totalCarSpeed = (notWinter * roadDraft.speed + winter * roadDraft.speedWinter) * car.draft.speed;
            totalCarSpeed *= generalSpeedFactor;
            int occupationType = road.getOccupationType();
            if (occupationType != RoadOccupationType.NONE) {
                totalCarSpeed *= RoadOccupationType.getCarSpeed(road.getOccupationType());
                if ((car.flags & (Car.FLAG_POLICE | Car.FLAG_SWAT | Car.FLAG_MILITARY)) != 0) {
                    if (occupationType == RoadOccupationType.DEMONSTRATION
                            || (car.flags & (Car.FLAG_SWAT | Car.FLAG_MILITARY)) != 0) {
                        road.setOccupationType(RoadOccupationType.NONE);
                    }
                }
            }
            if (car.isImportant) totalCarSpeed *= 2;

            //totalCarSpeed = Math.min(totalCarSpeed, car.lastSpeed + timeDelta / 100f);

            totalCarSpeed *= timeDelta / 1000f;

            while (totalCarSpeed > 0.01f) {
                float carSpeed = Math.min(0.9f, totalCarSpeed);
                totalCarSpeed -= carSpeed;

                car.prog = Math.min(car.prog, car.way.size() - 1);
                int dir = car.way.getDir(car.prog);
                float lastP = car.p;
                car.p += carSpeed;

                if (!Direction.isIn(dir, road.getDirs(car.x % 2,car.y % 2))) {
                    unregisterCar(car);
                    it.remove();
                    car.invalid = true;
                    break;
                }

                if (car.p >= 1.f) {
                    if (!car.paused) {
                        if (car.prog >= car.way.size() - 1) {
                            int dx = Direction.differenceX(dir);
                            int dy = Direction.differenceY(dir);
                            if (carControllers.get(car.controller).onTarget(car, car.x + dx, car.y + dy)) {
                                unregisterCar(car);
                                it.remove();
                                car.invalid = true;
                            }
                            break;
                        }
                        int nx = car.x + Direction.differenceX(dir);
                        int ny = car.y + Direction.differenceY(dir);

                        Tile _tile = city.getTile(nx / 2, ny / 2);
                        if (car.level == 0 && road.dLevel == dir && _tile != tile) {
                            car.level = 1;
                        }
                        tile = _tile;
                        car.prog++;
                        deltaProg++;
                        car.p--;
                        car.x = nx;
                        car.y = ny;
                        lastP = 0.f;
                        road = tile.getRoad(car.level);
                        if (road == null && car.level == 1) {
                            road = tile.getRoad(0);
                            if (road != null && road.dLevel == Direction.opposite(dir)) {
                                car.level = 0;
                            } else {
                                road = null;
                            }
                        }
                        dir = car.way.getDir(car.prog);
                        if (road == null) {
                            unregisterCar(car);
                            it.remove();
                            car.invalid = true;
                            break;
                        }
                    }
                }

                int nx = car.x + Direction.differenceX(dir);
                int ny = car.y + Direction.differenceY(dir);
                int nLevel = car.level;
                Car other = getCarMarker(car.x, car.y, car.level);
                int distance = 0;
                if (other == car || other != null && other.invalid) {
                    other = null;
                }

                if (other == null) {
                    other = getCarMarker(nx, ny, nLevel);
                    distance = 1;
                    if (other == null) {
                        if (nLevel == 1) {
                            Tile nTile = city.isValid(nx / 2, ny / 2) ? city.getTile(nx / 2, ny / 2) : null;
                            Road nRoad = nTile != null ? nTile.getRoad(0) : null;
                            if (nRoad != null && nRoad.dLevel != 0) {
                                other = getCarMarker(nx, ny, 0);
                            }
                        } else {
                            if (road.dLevel == dir) {
                                other = getCarMarker(nx, ny, 1);
                            }
                        }
                    }
                    if (other == car || other != null && other.invalid) {
                        other = null;
                    }
                }

                if (other == null) {
                    nx += Direction.differenceX(dir);
                    ny += Direction.differenceY(dir);
                    other = getCarMarker(nx, ny, nLevel);
                    distance = 2;
                    if (other == car || other != null && other.invalid) {
                        other = null;
                    }
                }
                if (other != null) {
                    if (car.isImportant && !other.isImportant) {
                        other.invalid = true;
                    } else {
                        if (other.getDirection() == dir || other.getDirection() == 0) {
                            if (!tryToOverrun(car, dir, road)) {
                                float otherP = other.way.size() == 2 && other.way.getDir(1) == Direction.NONE || other.paused ? 0 : other.p;
                                float targetP = Math.max(otherP - 1.75f + distance, 0);
                                car.p = Math.min(targetP, 0.6f * (car.p - lastP) + lastP);
                            }
                        } else if (Direction.opposite(other.getDirection()) == dir) {
                            if (other.p > lastP) car.p = lastP;
                        } else if (car.way.size() > car.prog + 1 && car.way.getDir(car.prog + 1) == other.getDirection()) {
                            car.p = lastP;
                        } else {
                            car.p = 0.5f * car.p + 0.5f * lastP;
                        }
                    }
                }

                if (!canDrive(car.x, car.y, dir, time, car.level) && lastP <= 0.25f && !car.isImportant) {
                    car.p = lastP;
                }
                if (car.p - lastP >= 0.001f) {
                    car.lastMovement = time;
                } else {
                    break;
                }
            }

            if (!car.invalid && (lastX != car.x || lastY != car.y)) {
                setCarMarker(lastX, lastY, null, lastLevel);
                setCarMarker(car.x, car.y, car, car.level);
                car.lastSpeed = (car.p - lastLastP + deltaProg) / timeDelta * 1000f;
            }
        }
    }

    private boolean tryToOverrun(Car car, int dir, Road road) {
        Way way = car.way;
        int prog = car.prog;

        if (!road.draft.overrunnable) return false;
        if (prog >= way.size() - 1 - 2 || prog < 1) return false;
        if (dir == Direction.NONE) return false;
        if (Resources.RND.nextFloat() < 0.9f) return false;

        int dx = Direction.differenceX(dir);
        int dy = Direction.differenceY(dir);
        int x = car.x;
        int y = car.y;
        int lx = x % 2;
        int ly = y % 2;
        int baseX = x - lx;
        int baseY = y - ly;
        int nlx = lx;
        int nly = ly;
        if (dx != 0) {
            nly = (nly + 1) % 2;
        } else {
            nlx = (nlx + 1) % 2;
        }
        int startX = baseX + nlx;
        int startY = baseY + nly;
        int orthDir = Direction.fromDifferential(nlx - lx, nly - ly);
        int backDir = Direction.opposite(orthDir);

        if (!Direction.isIn(dir, road.getDirs(startX % 2, startY % 2))) return false;

        for (int i = 0; i < 3; i++) {
            if (getCarMarker(startX + i * dx, startY + i * dy, car.level) != null) return false;
        }

        x = car.x;
        y = car.y;
        int altX = startX + dx;
        int altY = startY + dy;
        int length = 0;
        int maxLength = 0;
        while (prog + length < car.way.size()
                && car.way.getDir(prog + length) == dir) {
            road = city.getTile(altX / 2, altY / 2).getRoad(0);
            if (road == null) break;
            int altDir = road.getDirs(altX % 2, altY % 2);
            if (Direction.isIn(backDir, altDir)) maxLength = length;
            if (!Direction.isIn(dir, altDir)) break;
            length++;
            x += dx;
            y += dy;
            altX += dx;
            altY += dy;
            if (x < 0 || y < 0 || x >= 2 * city.getWidth() || y >= 2 * city.getHeight()) break;
        }
        length = maxLength;

        if (length < 1) return false;

        car.x = startX;
        car.y = startY;
        if (way.size() > prog + length + 1 && way.getDir(prog + length + 1) == orthDir) {
            way.setDir(prog + length + 1, dir);
            car.prog++;
        } else {
            way.setDir(prog + length, backDir);
            way.setDir(prog - 1, dir);
            car.prog--;
        }

        return true;
    }

    private void addCarController(CarController controller) {
        controller.setId(carControllers.size());
        carControllers.add(controller);
    }

    @Override
    public void spawn(CarController controller, CarDraft draft, int frame, long data, Way way) {
        spawnCar(way, draft, controller, frame, data);
    }

    @Override
    public void spawn(MapArea start, MapArea target, CarController controller, CarDraft draft, long data) {
        int frame = Resources.RND.nextInt(draft.frames.length / 4);
        spawn(start, target, controller, draft, frame, data);
    }

    @Override
    public void spawn(final MapArea start, final MapArea target, final CarController controller,
                      final CarDraft draft, final int frame, final long data) {
        if (countPathfindingTasks.get() < MAX_PATHFINDING_TASKS) {
            countPathfindingTasks.incrementAndGet();
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    Pathfinding pathfinding = ((PathfindingThread) Thread.currentThread()).pathfinding;
                    pathfinding.setRoadFlags(draft.flags);
                    pathfinding.addStart(start);
                    pathfinding.addTarget(target);
                    pathfinding.calculate(
                            controller == carControllers.get(0) ? PATHFINDING_TIMEOUT : PATHFINDING_IMPORTANT_TIMEOUT);

                    if (pathfinding.hasResult()) {
                        Way way = pathfinding.getResult();
                        spawnCar(way, draft, controller, frame, data);
                    } else {
                        controller.onNotSpawn();
                    }

                    countPathfindingTasks.decrementAndGet();
                }
            };
            pathfindingExecutor.execute(task);
        } else {
            Log.e("Pathfinding", "Overloaded");
        }
    }

    @Override
    public void driveTo(final Car car, final MapArea target) {
        car.paused = true;
        if (countPathfindingTasks.get() < MAX_PATHFINDING_TASKS || car.isImportant) {
            countPathfindingTasks.incrementAndGet();
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    if (!car.invalid) {
                        Pathfinding pathfinding = ((PathfindingThread) Thread.currentThread()).pathfinding;
                        int dir = car.way.getDir(car.prog);
                        int dx = Direction.differenceX(dir);
                        int dy = Direction.differenceY(dir);
                        CarController carController = carControllers.get(car.controller);
                        pathfinding.setRoadFlags(car.draft.flags);
                        pathfinding.addStartParcel(car.x + dx, car.y + dy, car.level);
                        pathfinding.addTarget(target);
                        pathfinding.calculate(
                                car.controller != 0 || car.isImportant ? PATHFINDING_IMPORTANT_TIMEOUT : PATHFINDING_TIMEOUT);

                        synchronized (cars) {
                            if (pathfinding.hasResult()) {
                                setCarMarker(car.x, car.y, null, car.level);
                                Way way = pathfinding.getResult();
                                car.driveTo(way);
                                setCarMarker(car.x, car.y, car, car.level);
                                car.paused = false;
                                carController.foundWay(car);
                            } else if (pathfinding.isShortcut()) {
                                CarTrafficHandler.this.wait(car);
                            } else {
                                if (car.isImportant) {
                                    Log.e("Pathfinding", "Found no way");
                                }
                                car.paused = false;
                                carController.foundNoWay(car);
                                if (!car.paused) {
                                    unregisterCarFromList(car);
                                } else {
                                    car.paused = false;
                                }
                            }
                        }
                    }

                    countPathfindingTasks.decrementAndGet();
                }
            };
            pathfindingExecutor.execute(task);
        } else {
            synchronized (cars) {
                car.paused = false;
                carControllers.get(car.controller).foundNoWay(car);
                if (!car.paused) {
                    car.invalid = true;
                } else {
                    car.paused = false;
                }
            }

        }
    }

    @Override
    public void wait(Car car) {
        car.paused = true;
        synchronized (cars) {
            int dir = car.way.getDir(car.prog);
            if (dir == Direction.NONE && car.prog > 0) {
                dir = car.way.getDir(car.prog - 1);
                car.x -= Direction.differenceX(dir);
                car.y -= Direction.differenceY(dir);
            }
            setCarMarker(car.x, car.y, null, car.level);
            car.driveTo(new Way(car.x, car.y, car.level, dir));
            car.p = 1;
            setCarMarker(car.x, car.y, car, car.level);
            car.paused = false;
        }
    }

    public void draw(int x, int y, Road r, Drawer d) {
        Tile tile;
        int level = r.level;

        d.setClipRect(0, -4 * Constants.TILE_HEIGHT, Constants.TILE_WIDTH, 5 * Constants.BUTTON_HEIGHT);

        int neighborShiftX;
        int neighborShiftY;
        int neighborDir;

        neighborDir = Direction.rotateCCW(Direction.NORTH_WEST, d.rotation);
        neighborShiftX = Direction.differenceX(neighborDir);
        neighborShiftY = Direction.differenceY(neighborDir);

        if (city.isValid(x + neighborShiftX, y + neighborShiftY)) {
            tile = city.getTile(x + neighborShiftX, y + neighborShiftY);
            Road r0 = tile.getRoad(level);
            int lvl = level;

            if (r0 == null) {
                if (level == 1 && (r0 = tile.getRoad(0)) != null && r0.dLevel != 0) {
                    lvl = 0;
                } else {
                    r0 = null;
                }
                if (level == 0 && r.dLevel == neighborDir) {
                    lvl = 1;
                    r0 = tile.getRoad(lvl);
                }
            }

            if (r0 != null) {
                for (int i = 0; i < 4; i++) {
                    int acc = ORDERED_PARCEL_ACCESS[i];
                    int dx = Math.abs(2 * neighborShiftX + acc % 2) - (neighborShiftX + 1) / 2;
                    int dy = Math.abs(2 * neighborShiftY + acc / 2 % 2) - (neighborShiftY + 1) / 2;
                    if (Math.max(dx, dy) <= 1) {
                        Car car = carArray[(x + neighborShiftX + (y + neighborShiftY) * width) * 4 + acc + lvl * levelStride];
                        if (car != null) {
                            drawCar(x, y, r0, car, d, Direction.SOUTH_EAST | Direction.SOUTH_WEST);
                        }
                    }
                }
            }
        }

        neighborDir = Direction.rotateCCW(Direction.NORTH_EAST, d.rotation);
        neighborShiftX = Direction.differenceX(neighborDir);
        neighborShiftY = Direction.differenceY(neighborDir);

        if (city.isValid(x + neighborShiftX, y + neighborShiftY)) {
            tile = city.getTile(x + neighborShiftX, y + neighborShiftY);
            Road r0 = tile.getRoad(level);
            int lvl = level;

            if (r0 == null) {
                if (level == 1 && (r0 = tile.getRoad(0)) != null && r0.dLevel != 0) {
                    lvl = 0;
                } else {
                    r0 = null;
                }
                if (level == 0 && r.dLevel == neighborDir) {
                    lvl = 1;
                    r0 = tile.getRoad(lvl);
                }
            }

            if (r0 != null) {
                for (int i = 0; i < 4; i++) {
                    int acc = ORDERED_PARCEL_ACCESS[i];
                    int dx = Math.abs(2 * neighborShiftX + acc % 2) - (neighborShiftX + 1) / 2;
                    int dy = Math.abs(2 * neighborShiftY + acc / 2 % 2) - (neighborShiftY + 1) / 2;
                    if (Math.max(Math.abs(dx), Math.abs(dy)) <= 1) {
                        Car car = carArray[(x + neighborShiftX + (y + neighborShiftY) * width) * 4 + acc + lvl * levelStride];
                        if (car != null) {
                            drawCar(x, y, r0, car, d, Direction.SOUTH_EAST | Direction.SOUTH_WEST);
                        }
                    }
                }
            }
        }
        d.resetClipping();

        for (int i = 0; i < 4; i++) {
            Car car = carArray[(x + y * width) * 4 + ORDERED_PARCEL_ACCESS[i] + level * levelStride];
            if (car != null) {
                drawCar(x, y, r, car, d, Direction.ALL);
            }
        }
    }

    private void drawCar(int x, int y, Road r, Car car, Drawer d, int onlyDir) {
        if (car.invalid || car.way.size() <= car.prog) return;
        int level = r.level;

        int originalDir = car.way.getDir(car.prog);
        int movingDir = Direction.rotateCW(originalDir, d.rotation);
        if (originalDir == Direction.NONE) {
            originalDir = car.way.getDir(Math.max(car.prog - 1, 0));
        }
        int dir = Direction.rotateCW(originalDir, d.rotation);

        if (!Direction.isIn(dir, onlyDir) || originalDir == Direction.NONE) return;

        float p = Math.min(Math.max(car.p, 0.f), 1.f);

        int difX = car.x / 2 - x;
        int difY = car.y / 2 - y;
        float sx = 0.5f * p * Direction.differenceX(movingDir);
        float sy = 0.5f * p * Direction.differenceY(movingDir);
        float parcelShiftX = (car.x % 2) / 2.f;
        float parcelShiftY = (car.y % 2) / 2.f;

        switch (d.rotation) {
            case Direction.ROTATION_0_DEGREE:
                sx += parcelShiftX + difX;
                sy += parcelShiftY + difY;
                break;
            case Direction.ROTATION_90_DEGREE:
                sx += parcelShiftY + difY;
                sy += 0.5f - parcelShiftX - difX;
                break;
            case Direction.ROTATION_180_DEGREE:
                sx += 0.5f - parcelShiftX - difX;
                sy += 0.5f - parcelShiftY - difY;
                break;
            case Direction.ROTATION_270_DEGREE:
                sx += 0.5f - parcelShiftY - difY;
                sy += parcelShiftX + difX;
                break;
        }

        if (dir == Direction.NORTH_EAST) {
            sx -= 0.5f;
            sy -= 0.25f;
        }
        if (dir == Direction.NORTH_WEST) {
            sy -= 0.5f;
            sx -= 0.25f;
        }
        if (dir == Direction.SOUTH_EAST) {
            sx -= 0.2f;
        }
        if (dir == Direction.SOUTH_WEST) {
            sy -= 0.1f;
        }

        int px = (int) Math.floor((sx + sy) * Constants.TILE_WIDTH / 2.f);
        int py = (int) Math.floor((sx - sy) * Constants.TILE_HEIGHT / 2.f);

        py -= r.draft.bridgeHeight * level;

        if (r.dLevel != 0) {
            float _p;
            if (car.x / 2 == (car.x + Direction.differenceX(originalDir)) / 2 && car.y / 2 == (car.y + Direction.differenceY(originalDir)) / 2) {
                _p = p * 0.5f;
            } else {
                _p = p * 0.5f + 0.5f;
            }
            if (r.dLevel != originalDir) {
                _p = 1 - _p;
            }
            py -= r.draft.bridgeHeight * _p;
        }

        int mapping = CAR_FRAME_MAPPING[Direction.toIndex(dir)];
        d.draw(Resources.IMAGE_WORLD, px, py, car.draft.frames[mapping + 4 * car.frame]);

        if (car.draft.animationSpots != null) {
            int posIdx = 2 * mapping;
            int animationFrame = d.time / Constants.ANIMATION_SPEED;
            for (CarAnimationSpot spot : car.draft.animationSpots) {
                if (!spot.important || car.isImportant) {
                    d.draw(Resources.IMAGE_WORLD, px + spot.pos.get(posIdx), py + spot.pos.get(posIdx + 1), spot.draft.frames[animationFrame % spot.draft.frames.length]);
                }
            }
        }
    }

    private void setCarMarker(int x, int y, Car car, int level) {
        int tileX = x / 2;
        int tileY = y / 2;
        if (!city.isValid(tileX, tileY) || x < 0 || y < 0) return;
        carArray[(tileX + tileY * width) * 4 + x % 2 + 2 * (y % 2) + level * levelStride] = car;
    }

    private Car getCarMarker(int x, int y, int level) {
        int tileX = x / 2;
        int tileY = y / 2;
        if (!city.isValid(tileX, tileY) || x < 0 || y < 0) return null;
        return carArray[(tileX + tileY * width) * 4 + x % 2 + 2 * (y % 2) + level * levelStride];
    }

    private void registerCar(Car car) {
        CarController controller = carControllers.get(car.controller);

        if (controller != null) {
            setCarMarker(car.x, car.y, car, car.level);
            cars.add(car);
            controller.register(car);
        }
    }

    private void unregisterCarFromList(Car car) {
        car.invalid = true;
        cars.remove(car);
        unregisterCar(car);
    }

    private void unregisterCar(Car car) {
        Car markedCar = getCarMarker(car.x, car.y, car.level);
        if (markedCar == car) {
            setCarMarker(car.x, car.y, null, car.level);
        }
        car.invalid = true;

        CarController controller = carControllers.get(car.controller);
        controller.unregister(car);
    }

    private boolean canDrive(int x, int y, int dir, int time, int level) {
        int nx = x + Direction.differenceX(dir);
        int ny = y + Direction.differenceY(dir);

        if (x / 2 != nx / 2 || y / 2 != ny / 2) {
            Tile tile = city.getTile(nx / 2, ny / 2);
            Road road = tile.getRoad(level);
            if (road == null && level == 1) {
                road = tile.getRoad(level - 1);
                if (road != null && road.dLevel == 0) road = null;
            }
            if (road != null) {
                if (road.hasTrafficLights()) {
                    int align = road.getTrafficLightFrame(time);
                    return (dir == 1 || dir == 4) && (align == 0) || (dir == 2 || dir == 8) && (align == 2);
                }
            }
        }

        return true;
    }

    public boolean spawnCar(Way way, CarDraft draft, CarController controller, int frame, long data) {
        if (way.size() >= 3) {
            synchronized (cars) {
                Car car = new Car(draft, way, frame, controller.getId(), carIdCounter++);
                car.data = data;
                if (onSpawnCar(car)) {
                    carControllers.get(car.controller).foundWay(car);
                    registerCar(car);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return true;
        }
    }

    private boolean onSpawnCar(Car car) {
        CarController controller = carControllers.get(car.controller);
        return controller.onSpawn(car);
    }

    public int countCars() {
        return cars.size();
    }

    public int countCars(int x, int y, int level) {
        int offset = (x + y * width) * 4 + level * levelStride;
        int count = 0;
        for (int i = 0; i < 4; i++) {
            count += carArray[offset + i] != null ? 1 : 0;
        }
        return count;
    }

    @Override
    public void dispose() {
        super.dispose();

        pathfindingExecutor.shutdown();
    }

    private static class PathfindingThread extends Thread {
        Pathfinding pathfinding;

        public PathfindingThread(City city, Runnable runnable) {
            super(runnable);
            pathfinding = new Pathfinding(city);
        }
    }

}

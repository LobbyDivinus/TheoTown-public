package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import android.util.SparseIntArray;

import java.io.IOException;
import java.util.List;

import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.components.DefaultSoundManager;
import info.flowersoft.theotown.theotown.components.DefaultTraffic;
import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceController;
import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceHandler;
import info.flowersoft.theotown.theotown.components.soundsource.AbstractTileSoundSource;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.BuildingSampler;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.MapArea;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.components.Date;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.resources.SoundType;
import info.flowersoft.theotown.theotown.stapel2d.util.ProbabilitySelector;
import info.flowersoft.theotown.theotown.util.DataAccessor;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by Lobby on 28.11.2015.
 */
public abstract class OperationCarController extends CarController implements ActionPlaceHandler {

    protected boolean removeCar;

    protected MapArea actionPlace;

    protected int actionIdCounter;

    protected long lastActionDay;

    protected int virtualCars;

    private int actionPlaceType;

    protected ActionPlaceController actionPlaceController;

    public OperationCarController(CarSpawner spawner, int actionPlaceType) {
        super(spawner);

        removeCar = false;

        actionIdCounter = 1;
        lastActionDay = -1;

        this.actionPlaceType = actionPlaceType;
    }

    @Override
    public final void load(JsonReader src, City city) throws IOException {
        this.city = city;

        while (src.hasNext()) {
            String name = src.nextName();
            if (name.equals("actionId")) {
                actionIdCounter = src.nextInt();
            } else if (loadTag(src, name)) {
            } else {
                src.skipValue();
            }
        }
    }

    protected boolean loadTag(JsonReader src, String name) throws IOException {
        return false;
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
        dest.name("actionId").value(actionIdCounter);
        dest.name("actionDay").value(lastActionDay);
        if (actionPlace != null) {
            dest.name("actionPlace");
            actionPlace.save(dest);
        }

    }

    @Override
    public void prepare(City city) {
        super.prepare(city);
        if (actionPlaceType >= 0) {
            actionPlaceController = ((DefaultTraffic) city.getComponent(ComponentType.TRAFFIC)).getActionPlaceController();
            actionPlaceController.registerHandler(this, actionPlaceType);
        }
    }

    private void startSound(final Car car) {
        if (city != null && city.getComponent(ComponentType.SOUND) != null) {
            DefaultSoundManager soundManager = (DefaultSoundManager) city.getComponent(ComponentType.SOUND);
            soundManager.playLoop(getSoundID(car), SoundType.GAME, new AbstractTileSoundSource(city) {
                @Override
                public float getTileX() {
                    return car.x / 2f;
                }

                @Override
                public float getTileY() {
                    return car.y / 2f;
                }

                @Override
                public float getVolume() {
                    return 1;
                }

                @Override
                protected boolean isValid() {
                    return !car.invalid && car.isImportant;
                }
            });
        }
    }

    @Override
    public void foundWay(Car car) {
        if (actionPlace != null && !car.isImportant && DataAccessor.read(car.data, 32, 0) == actionIdCounter) {
            car.isImportant = true;
        }
    }

    abstract protected int getSoundID(Car car);

    @Override
    public void register(Car car) {
        super.register(car);
        if (car.isImportant) {
            startSound(car);
        }
    }

    public abstract boolean onWorkDone(Car car, int parcelX, int parcelY);

    @Override
    public boolean onTarget(Car car, int parcelX, int parcelY) {
        MapArea a = actionPlace;
        if (car.isImportant && a != null && city.getDistance().get(car.x / 2, car.y / 2, a) < 3) {
            boolean wait;

            if (car.way.size() > 2) {
                wait = true;
            } else {
                wait = !onWorkDone(car, parcelX, parcelY);
                if (!wait) {
                    car.isImportant = false;
                    actionPlaceController.solverReached(actionPlaceType);
                }
            }
            
            if (wait) {
                spawner.wait(car);
                return false;
            }
        }

        if (!removeCar) {
            return driveToNextTarget(car);
        } else {
            removeCar = false;
            return true;
        }
    }

    protected boolean driveToNextTarget(Car car) {
        Building station = getStationForCar(car);
        if (station != null) {
            Building building = sampleTarget(station);

            if (building != null) {
                MapArea target = new MapArea();
                target.add(building);

                spawner.driveTo(car, target);

                return false;
            }
        }
        return true;
    }

    @Override
    public void foundNoWay(Car car) {
        car.paused = 0.1f < Resources.RND.nextFloat();
        if (car.isImportant && car.paused) {
            car.isImportant = false;
            actionPlaceController.unregisterSolver(actionPlaceType);
        }
        car.data = DataAccessor.write(car.data, 32, 0, actionIdCounter);
    }

    protected abstract List<Building> getStations();

    protected int getStationRadius(Building building) {
        return 20;
    }

    protected final int getCarCountOf(Building building) {
        return  building.isWorking() ? getMaxCarCountOf(building) : 0;
    }

    protected int getMaxCarCountOf(Building building) {
        return building.getWidth();
    }

    protected abstract CarDraft getCarDraft(Building building);

    @Override
    public void update() {
        List<Building> stations = getStations();

        int maxCount = 0;
        if (stations != null) {
            for (Building station : new SafeListAccessor<>(stations)) {
                maxCount += getCarCountOf(station);
            }
        }

        if (maxCount > cars.size() && virtualCars <= 0) {
            Building startStation = sampleFreeStation();
            if (startStation != null){
                MapArea start = new MapArea();
                start.add(startStation);

                Building targetBuilding = sampleTarget(startStation);
                if (targetBuilding != null) {
                    MapArea target = new MapArea();
                    target.add(targetBuilding);
                    CarDraft draft = getCarDraft(startStation);
                    int frame = Resources.RND.nextInt(draft.frames.length / 4);

                    long data = 0;
                    data = DataAccessor.write(data, 10, 32, startStation.getX() + 1);
                    data = DataAccessor.write(data, 10, 42, startStation.getY() + 1);

                    if (actionPlace != null) {
                        data = DataAccessor.write(data, 32, 0, actionIdCounter);
                        target = actionPlace;
                        actionPlaceController.registerSolver(actionPlaceType);
                    }

                    spawner.spawn(start, target, this, draft, frame, data);
                    virtualCars++;

                    removeCar = false;
                }
            }
        } else if (maxCount < cars.size()) {
            removeCar = true;
        }
    }

    @Override
    public void unregister(Car car) {
        super.unregister(car);
        if (car.isImportant) {
            actionPlaceController.unregisterSolver(actionPlaceType);
        }
    }

    @Override
    public void notifyAction(final MapArea place, int type) {
        actionPlace = place;
        lastActionDay = ((Date) city.getComponent(ComponentType.DATE)).getAbsoluteDay();
        actionIdCounter++;

        for (Car car : new SafeListAccessor<>(cars)) {
            if (!car.isImportant) {
                car.isImportant = true;
                startSound(car);
            }
            car.data = DataAccessor.write(car.data, 32, 0, actionIdCounter);
            actionPlaceController.registerSolver(actionPlaceType);
            spawner.driveTo(car, place);
        }
    }

    @Override
    public void notifyNoAction(int type) {
        lastActionDay = -1;
        actionPlace = null;

        for (Car car : new SafeListAccessor<>(cars)) {
            if (car.isImportant) {
                car.isImportant = false;
                driveToNextTarget(car);
            }
        }
    }

    protected Building sampleTarget(Building station) {
        BuildingSampler sampler = new BuildingSampler(city, getStationRadius(station), station.getX(), station.getY());
        configureSampler(sampler);
        return sampler.sample();
    }

    protected void configureSampler(BuildingSampler sampler) {
    }

    private Building getStationForCar(Car car) {
        Building station = getBuildingData(car);
        if (station != null) {
            return station;
        } else {
            station = sampleFreeStation();

            if (station != null) {
                setBuildingData(car, station);
                return station;
            } else {
                return null;
            }
        }
    }

    protected Building sampleFreeStation() {
        SparseIntArray availableStations = new SparseIntArray();
        List<Building> stations = getStations();

        if (stations != null) {
            for (Building building : new SafeListAccessor<>(stations)) {
                availableStations.put(building.hashCode(), getCarCountOf(building));
            }
        }

        for (Car c : cars) {
            Building building = getBuildingData(c);
            if (building != null) {
                int index = availableStations.indexOfKey(building.hashCode());
                if (index >= 0){
                    int value = availableStations.valueAt(index);
                    value--;
                    availableStations.put(building.hashCode(), value);
                    if (value <= 0) {
                        availableStations.removeAt(index);
                    }
                }
            }
        }

        if (availableStations.size() > 0) {
            ProbabilitySelector<Building> selector = new ProbabilitySelector<>(Resources.RND);
            for (int i = 0; i < availableStations.size(); i++) {
                int xy = availableStations.keyAt(i);
                int x = xy & 0xFFFF;
                int y = xy >> 16;
                Building building = city.getTile(x, y).building;
                if (building != null) {
                    selector.insert(building, 1);
                }
            }
            return selector.hasResult() ? selector.getResult() : null;
        } else {
            return null;
        }
    }

    protected void setBuildingData(Car car, Building building) {
        int x = -1;
        int y = -1;
        long data = car.data;

        if (building != null) {
            x = building.getX();
            y = building.getY();
        }

        data = DataAccessor.write(data, 10, 32, x + 1);
        data = DataAccessor.write(data, 10, 42, y + 1);

        car.data = data;
    }

    protected Building getBuildingData(Car car) {
        int x = (int) DataAccessor.read(car.data, 10, 32) - 1;
        int y = (int) DataAccessor.read(car.data, 10, 42) - 1;
        if (city.isValid(x, y)) {
            return city.getTile(x, y).building;
        } else {
            return null;
        }
    }

    @Override
    public boolean onSpawn(Car car) {
        virtualCars--;
        return super.onSpawn(car);
    }

    @Override
    public void onNotSpawn() {
        virtualCars--;
        super.onNotSpawn();
    }
}

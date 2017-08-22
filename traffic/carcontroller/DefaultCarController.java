package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.AbstractList;

import info.flowersoft.theotown.theotown.components.DefaultTransportation;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.MapArea;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.map.objects.Way;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.util.DataAccessor;

/**
 * Created by Lobby on 11.09.2015.
 */
public class DefaultCarController extends CarController implements BuildingCarSpawner {

    private DefaultTransportation transportation;

    public DefaultCarController(CarSpawner spawner) {
        super(spawner);

        cars = new AbstractList<Car>() {
            private int size;

            @Override
            public boolean add(Car car) {
                size++;
                return true;
            }

            @Override
            public Car remove(int index) {
                size--;
                return null;
            }

            @Override
            public boolean remove(Object o) {
                size--;
                return true;
            }

            @Override
            public Car get(int index) {
                throw new UnsupportedOperationException("");
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    @Override
    public void prepare(City city) {
        super.prepare(city);

        transportation = (DefaultTransportation) city.getComponent(ComponentType.TRANSPORTATION);
    }

    @Override
    public void foundWay(Car car) {
        super.foundWay(car);

        long data = car.data;
        int x0 = (int) DataAccessor.read(data, 10, 0);
        int y0 = (int) DataAccessor.read(data, 10, 10);
        int x1 = (int) DataAccessor.read(data, 10, 20);
        int y1 = (int) DataAccessor.read(data, 10, 30);
        Building b0 = city.getTile(x0, y0).building;
        Building b1 = city.getTile(x1, y1).building;
        boolean newWay = DataAccessor.read(data, 1, 63) == 0;

        if (newWay) {
            transportation.addFoundWay(car.draft, car.frame, b0, b1, x0, y0, x1, y1, new Way(car.way));
        }
        transportation.foundCarWay(b0, b1, car.way.size());
    }

    @Override
    public void foundNoWay(Car car) {
        super.foundNoWay(car);

        long data = car.data;
        int x0 = (int) DataAccessor.read(data, 10, 0);
        int y0 = (int) DataAccessor.read(data, 10, 10);
        int x1 = (int) DataAccessor.read(data, 10, 20);
        int y1 = (int) DataAccessor.read(data, 10, 30);
        Building b0 = city.getTile(x0, y0).building;
        Building b1 = city.getTile(x1, y1).building;

        if (b0 != null && b1 != null) {
            transportation.foundNoCarWay(b0, b1);
        }
    }

    @Override
    public boolean onTarget(Car car, int parcelX, int parcelY) {
        return true;
    }

    @Override
    public void update() {
        transportation.updateCars(cars.size(), this);
    }

    @Override
    public void spawn(CarDraft draft, int frame, Building start, Building target,
                      int startX, int startY, int startLevel, int targetX, int targetY, int targetLevel) {
        if (frame < 0) {
            frame = Resources.RND.nextInt(draft.frames.length / 4);
        }

        long data = 0;
        data = DataAccessor.write(data, 10, 0, startX);
        data = DataAccessor.write(data, 10, 10, startY);
        data = DataAccessor.write(data, 10, 20, targetX);
        data = DataAccessor.write(data, 10, 30, targetY);
        data = DataAccessor.write(data, 1, 63, 0);

        MapArea startArea = new MapArea();
        MapArea targetArea = new MapArea();

        if (start != null) {
            startArea.add(start);
        } else {
            startArea.add(startX, startY, startLevel);
        }
        if (target != null) {
            targetArea.add(target);
        } else {
            targetArea.add(targetX, targetY, targetLevel);
        }

        spawner.spawn(startArea, targetArea, this, draft, frame, data);
    }

    @Override
    public void spawn(CarDraft draft, int frame, int startX, int startY, int targetX, int targetY, Way way) {
        if (frame < 0) {
            frame = Resources.RND.nextInt(draft.frames.length / 4);
        }

        long data = 0;
        data = DataAccessor.write(data, 10, 0, startX);
        data = DataAccessor.write(data, 10, 10, startY);
        data = DataAccessor.write(data, 10, 20, targetX);
        data = DataAccessor.write(data, 10, 30, targetY);
        data = DataAccessor.write(data, 1, 63, 1);

        spawner.spawn(this, draft, frame, data, new Way(way));
    }

    @Override
    public String getName() {
        return "DefaultCarController";
    }
}

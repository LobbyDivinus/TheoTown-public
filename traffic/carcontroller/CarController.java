package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import info.flowersoft.theotown.theotown.draft.RoadDraft;
import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.components.Date;
import info.flowersoft.theotown.theotown.map.objects.Car;

/**
 * Created by Lobby on 11.09.2015.
 */
public abstract class CarController {

    protected CarSpawner spawner;

    protected City city;

    protected List<Car> cars = new LinkedList<>();

    private int id;

    public CarController(CarSpawner spawner) {
        this.spawner = spawner;
    }

    public void load(JsonReader src, City city) throws IOException {
        this.city = city;

        while (src.hasNext()) {
            src.skipValue();
        }
    }

    public void save(JsonWriter dest) throws IOException {
    }

    public void prepare(City city) {
        this.city = city;
    }

    public List<Car> getCars() {
        return cars;
    }

    public boolean onSpawn(Car car) {
        return true;
    };

    public void onNotSpawn() {
    }

    public void register(Car car) {
        cars.add(car);
    }

    public abstract boolean onTarget(Car car, int parcelX, int parcelY);

    public void unregister(Car car) {
        cars.remove(car);
    }

    public abstract void update();

    public void foundWay(Car car) {
    }

    public void foundNoWay(Car car) {
    }

    protected Date getDate() {
        return (Date) city.getComponent(ComponentType.DATE);
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final int getId() {
        return id;
    }

    public abstract String getName();

}

package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;
import info.flowersoft.theotown.theotown.util.Saveable;
import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;

/**
 * Created by Lobby on 05.08.2016.
 */
public abstract class FlyingObjectController implements Saveable {

    protected City city;

    protected FlyingObjectSpawner spawner;

    protected List<FlyingObject> flyingObjects;

    public FlyingObjectController(City city, FlyingObjectSpawner spawner) {
        this.city = city;
        this.spawner = spawner;
        flyingObjects = new ArrayList<>();
    }

    @Override
    public void load(JsonReader src) throws IOException {
        while (src.hasNext()) src.skipValue();
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
    }

    public void prepare() {}

    public void register(FlyingObject flyingObject) {
        flyingObjects.add(flyingObject);
    }

    public void unregister(FlyingObject flyingObject) {
        flyingObjects.remove(flyingObject);
    }

    public abstract void onTarget(FlyingObject flyingObject);

    public abstract void onSpawned(FlyingObject flyingObject);

    public abstract void update();

    public abstract String getTag();

}

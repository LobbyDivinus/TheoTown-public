package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import java.io.IOException;

import info.flowersoft.theotown.theotown.components.airport.DefaultAirport;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;
import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;

/**
 * Created by lobby on 06.08.2017.
 */

public class AirplaneController extends FlyingObjectController {

    DefaultAirport airport;

    public AirplaneController(City city, FlyingObjectSpawner spawner) {
        super(city, spawner);
    }

    @Override
    public void load(JsonReader src) throws IOException {
        while (src.hasNext()) src.skipValue();
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
    }

    @Override
    public void prepare() {
        super.prepare();

        airport = (DefaultAirport) city.getComponent(ComponentType.AIRPORT);
    }

    @Override
    public void onTarget(FlyingObject flyingObject) {

    }

    @Override
    public void onSpawned(FlyingObject flyingObject) {

    }

    @Override
    public void update() {
        airport.getLanes();
    }

    @Override
    public String getTag() {
        return "airport";
    }
}

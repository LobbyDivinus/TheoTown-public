package info.flowersoft.theotown.theotown.components.traffic.traincontroller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.TrainDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Train;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.stapel2d.util.ProbabilitySelector;
import info.flowersoft.theotown.theotown.util.DataAccessor;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by Lobby on 10.06.2016.
 */
public class DefaultTrainController extends TrainController {
    public DefaultTrainController(City city, TrainSpawner spawner) {
        super(city, spawner);
    }

    @Override
    public void onTarget(Train train) {
        if (trains.size() > getStations().size()) {
            spawner.remove(train);
        } else {
            driveToNextStation(train);
        }
    }

    @Override
    public void onSpawned(Train train) {
        driveToNextStation(train);
    }

    private void driveToNextStation(Train train) {
        ProbabilitySelector<Building> selector = new ProbabilitySelector<>(Resources.RND);

        for (Building station : new SafeListAccessor<>(getStations())) {
            int distance = Math.max(Math.abs(station.getX() + station.getWidth() / 2 - train.getX() / 2),
                    Math.abs(station.getY() + station.getHeight() / 2 - train.getY() / 2));
            int waiting = 0;
            waiting = Math.max(waiting, 1);
            selector.insert(station, 1f * waiting / (distance + 1));
        }

        if (selector.hasResult()) {
            Building station = selector.getResult();

            spawner.driveTo(train, station.getX() + 1, station.getY() + 1);
        } else {
            spawner.remove(train);
        }
    }

    @Override
    public void onFoundWay(Train train) {
        train.setData(DataAccessor.write(train.getData(), 8, 0, 0));
    }

    @Override
    public void onFoundNoWay(Train train) {
        int counter = (int) DataAccessor.read(train.getData(), 8, 0);
        if (counter < 4) {
            train.setData(DataAccessor.write(train.getData(), 8, 0, counter + 1));
            driveToNextStation(train);
        } else {
            spawner.remove(train);
        }
    }

    private List<Building> getStations() {
        return city.getBuildings().getBuildingsOfDraft((BuildingDraft) Drafts.ALL.get("$railwaystation00"));
    }

    @Override
    public void update() {
        List<Building> stations = getStations();

        if (trains.size() < stations.size()) {
            Building station = sampleStation();
            if (station != null) {
                spawner.spawn((TrainDraft) Drafts.ALL.get("$train00"),
                        station.getX() + 1, station.getY() + 1, Direction.ALL, 1, 0);
            }
        }
    }

    protected Building sampleStation() {
        Map<Building, Integer> stationVisitors = new HashMap<>();
        for (Building station : new SafeListAccessor<>(getStations())) {
            stationVisitors.put(station, 0);
        }

        for (Train train : trains) {
            Building lastStation = getLastTrainStation(train);
            if (lastStation != null && stationVisitors.containsKey(lastStation)) {
                stationVisitors.put(lastStation, stationVisitors.get(lastStation) + 1);
            }
        }

        ProbabilitySelector<Building> stationSelector = new ProbabilitySelector<>(Resources.RND);
        for (Building station : stationVisitors.keySet()) {
            stationSelector.insert(station, 1f / (stationVisitors.get(station) + 1));
        }

        if (stationSelector.hasResult()) {
            return stationSelector.getResult();
        } else {
            return null;
        }
    }

    @Override
    public void onVisitStation(Train train, Building station) {
        station.removeWaiters(train.getDraft().capacity);
    }

    @Override
    public void load(JsonReader src) throws IOException {
        while (src.hasNext()) {
            src.skipValue();
        }
    }

    @Override
    public void save(JsonWriter dst) throws IOException {
    }
}

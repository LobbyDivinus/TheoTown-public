package info.flowersoft.theotown.theotown.components.traffic.traincontroller;

import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Train;
import info.flowersoft.theotown.theotown.util.DataAccessor;
import info.flowersoft.theotown.theotown.util.Saveable;

/**
 * Created by Lobby on 10.06.2016.
 */
public abstract class TrainController implements Saveable {

    protected City city;

    protected TrainSpawner spawner;

    protected List<Train> trains;

    public TrainController(City city, TrainSpawner spawner) {
        this.city = city;
        this.spawner = spawner;
        trains = new ArrayList<>();
    }

    public void registerTrain(Train train) {
        trains.add(train);
    }

    public void unregisterTrain(Train train) {
        trains.remove(train);
    }

    public abstract void onTarget(Train train);

    public abstract void onSpawned(Train train);

    public abstract void onFoundWay(Train train);

    public abstract void onFoundNoWay(Train train);

    public abstract void update();

    public void visitStation(Train train, Building station) {
        if (getLastTrainStation(train) != station) {
            onVisitStation(train, station);
            setLastTrainStation(train, station);
        }
    }

    public abstract void onVisitStation(Train train, Building station);

    protected void setLastTrainStation(Train train, Building station) {
        int x = station.getX();
        int y = station.getY();
        long data = train.getData();

        data = DataAccessor.write(data, 1, 62, 1);
        data = DataAccessor.write(data, 10, 52, x);
        data = DataAccessor.write(data, 10, 42, y);

        train.setData(data);
    }

    protected Building getLastTrainStation(Train train) {
        Building lastStation = null;
        long data = train.getData();

        if (DataAccessor.read(data, 1, 62) == 1) {
            int x = (int) DataAccessor.read(data, 10, 52);
            int y = (int) DataAccessor.read(data, 10, 42);
            lastStation = city.getTile(x, y).building;
            if (lastStation != null && (lastStation.getBuildingType() != BuildingType.RAILWAY_STATION ||
                    lastStation.getX() != x || lastStation.getY() != y)) {
                lastStation = null;
            }
        }

        return lastStation;
    }
}

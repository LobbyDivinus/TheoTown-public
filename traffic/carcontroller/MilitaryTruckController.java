package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.List;

import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.draft.RoadDraft;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;

/**
 * Created by lobby on 14.04.2017.
 */

public class MilitaryTruckController extends OperationCarController {

    private CarDraft carDraft;

    private BuildingDraft buildingDraft;

    public MilitaryTruckController(CarSpawner spawner) {
        super(spawner, -1);

        carDraft = (CarDraft) Drafts.ALL.get("$mltry_truck00");
        buildingDraft = (BuildingDraft) Drafts.ALL.get("$mltry_depot00");
    }

    @Override
    public void register(Car car) {
        super.register(car);
        car.flags |= Car.FLAG_MILITARY;
    }

    @Override
    protected int getSoundID(Car car) {
        return 0;
    }

    @Override
    public boolean onWorkDone(Car car, int parcelX, int parcelY) {
        return false;
    }

    @Override
    protected List<Building> getStations() {
        return city.getBuildings().getBuildingsOfDraft(buildingDraft);
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        return carDraft;
    }

    @Override
    protected int getStationRadius(Building building) {
        return 16;
    }

    @Override
    public String getName() {
        return "MilitaryTruckController";
    }
}

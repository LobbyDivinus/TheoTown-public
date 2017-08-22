package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.io.IOException;
import java.util.List;

import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;

/**
 * Created by lobby on 14.04.2017.
 */

public class TankController extends OperationCarController {

    private CarDraft tankDraft;
    private CarDraft militaryTruckDraft;

    private BuildingDraft tankBaseDraft;
    private BuildingDraft depotDraft;

    private boolean patrol = false;

    public TankController(CarSpawner spawner) {
        super(spawner, -1);

        tankDraft = (CarDraft) Drafts.ALL.get("$tank00");
        tankBaseDraft = (BuildingDraft) Drafts.ALL.get("$mltry_tankbase00");

        militaryTruckDraft = (CarDraft) Drafts.ALL.get("$mltry_truck00");
        depotDraft = (BuildingDraft) Drafts.ALL.get("$mltry_depot00");
    }

    @Override
    public void register(Car car) {
        super.register(car);
        car.flags |= Car.FLAG_MILITARY;
    }

    @Override
    protected boolean loadTag(JsonReader src, String name) throws IOException {
        switch (name) {
            case "on patrol":
                patrol = src.nextBoolean();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
        super.save(dest);

        dest.name("on patrol").value(patrol);
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
        return city.getBuildings().getBuildingsOfType(BuildingType.MILITARY);
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        if (building.getDraft() == tankBaseDraft) {
            return tankDraft;
        } else {
            return militaryTruckDraft;
        }
    }

    @Override
    protected int getStationRadius(Building building) {
        return patrol ? 64 : 16;
    }

    @Override
    public String getName() {
        return "TankController";
    }

    public boolean onPatrol() {
        return patrol;
    }

    public void setPatrol(boolean state) {
        patrol = state;
    }

    @Override
    protected int getMaxCarCountOf(Building building) {
        if (building.getDraft() == tankBaseDraft) {
            return 8;
        } else if (building.getDraft() == depotDraft) {
            return 6;
        }
        return 0;
    }
}

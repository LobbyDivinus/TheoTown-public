package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.List;

import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;

/**
 * Created by lobby on 13.01.2017.
 */

public class HearseCarController extends OperationCarController {

    private CarDraft draft;

    public HearseCarController(CarSpawner spawner) {
        super(spawner, -1);

        draft = (CarDraft) Drafts.ALL.get("$hearse00");
    }

    @Override
    protected int getSoundID(Car car) {
        return 0;
    }

    @Override
    protected List<Building> getStations() {
        return city.getBuildings().getBuildingsOfBaseType(BuildingType.BODY_DISPOSAL);
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        return draft;
    }

    @Override
    public boolean onWorkDone(Car car, int parcelX, int parcelY) {
        return false;
    }

    @Override
    protected int getMaxCarCountOf(Building building) {
        return 1;
    }

    @Override
    public String getName() {
        return "HearseCarController";
    }
}

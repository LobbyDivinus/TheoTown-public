package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.List;

import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceType;
import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;

/**
 * Created by Lobby on 12.12.2016.
 */

public class GarbageTruckController extends OperationCarController {

    private CarDraft carDraft;

    public GarbageTruckController(CarSpawner spawner) {
        super(spawner, -1);

        carDraft = (CarDraft) Drafts.ALL.get("$garbagetruck00");
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
        return city.getBuildings().getBuildingsOfType(BuildingType.WASTE_DISPOSAL);
    }

    @Override
    protected int getMaxCarCountOf(Building building) {
        return (building.getDraftId().equals("$landfill00")
                || building.getDraftId().equals("$wasteincinerator00")) ? 4 : 0;
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        return carDraft;
    }

    @Override
    public String getName() {
        return "GarbageTruckController";
    }
}

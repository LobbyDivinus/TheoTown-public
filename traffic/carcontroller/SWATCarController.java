package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.List;

import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceType;
import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;

/**
 * Created by Lobby on 06.11.2016.
 */

public class SWATCarController extends OperationCarController {

    private CarDraft carDraft = (CarDraft) Drafts.ALL.get("$carswat00");

    public SWATCarController(CarSpawner spawner) {
        super(spawner, ActionPlaceType.SWAT);
    }

    @Override
    public void register(Car car) {
        super.register(car);
        car.flags |= Car.FLAG_SWAT;
    }

    @Override
    protected int getSoundID(Car car) {
        return Resources.SOUND_CAR_POLICE;
    }

    @Override
    public boolean onWorkDone(Car car, int parcelX, int parcelY) {
        return true;
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        return carDraft;
    }

    @Override
    protected List<Building> getStations() {
        return city.getBuildings().getBuildingsOfType(BuildingType.SWAT);
    }

    @Override
    public String getName() {
        return "SWATCarController";
    }
}

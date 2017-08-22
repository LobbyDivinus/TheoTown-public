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
 * Created by Lobby on 28.11.2015.
 */
public class MedicCarController extends OperationCarController {

    private CarDraft carDraft = (CarDraft) Drafts.ALL.get("$carmedic00");

    public MedicCarController(CarSpawner spawner) {
        super(spawner, ActionPlaceType.MEDIC);
    }

    @Override
    protected int getSoundID(Car car) {
        return Resources.SOUND_CAR_MEDIC;
    }

    @Override
    public boolean onWorkDone(Car car, int parcelX, int parcelY) {
        return true;
    }

    @Override
    protected List<Building> getStations() {
        return city.getBuildings().getBuildingsOfType(BuildingType.MEDIC);
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        return carDraft;
    }

    @Override
    public String getName() {
        return "MedicCarController";
    }
}

package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.components.DefaultCatastrophe;
import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceType;
import info.flowersoft.theotown.theotown.components.disaster.FireDisaster;
import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.draft.RoadDraft;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;

/**
 * Created by Lobby on 17.11.2015.
 */
public class FireEngineController extends OperationCarController {

    private CarDraft carDraft = (CarDraft) Drafts.ALL.get("$carfirebrigade00");

    public FireEngineController(CarSpawner spawner) {
        super(spawner, ActionPlaceType.FIRE);
    }

    @Override
    protected int getSoundID(Car car) {
        return Resources.SOUND_CAR_FIRE;
    }

    @Override
    public boolean onWorkDone(Car car, int parcelX, int parcelY) {
        DefaultCatastrophe catastrophe = (DefaultCatastrophe) city.getComponent(ComponentType.CATASTROPHE);
        FireDisaster fireDisaster = catastrophe.getDisaster(FireDisaster.class);

        List<Building> burningAndReachableBuildings = new ArrayList<>();
        for (Building building : fireDisaster.getBurningBuildings()) {
            int distance = city.getDistance().get(building, parcelX / 2, parcelY / 2);
            if (distance <= 6) {
                burningAndReachableBuildings.add(building);
            }
        }

        if (!burningAndReachableBuildings.isEmpty()) {
            if (Resources.RND.nextFloat() < 0.1f) {
                int size = burningAndReachableBuildings.size();
                int i = Resources.RND.nextInt(size);
                fireDisaster.removeFire(burningAndReachableBuildings.get(i));
                return size == 1;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        return carDraft;
    }

    @Override
    protected List<Building> getStations() {
        return city.getBuildings().getBuildingsOfType(BuildingType.FIRE_BRIGADE);
    }

    @Override
    public String getName() {
        return "FireEngineController";
    }
}

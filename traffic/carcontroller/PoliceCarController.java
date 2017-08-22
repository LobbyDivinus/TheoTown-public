package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.components.DefaultCatastrophe;
import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceType;
import info.flowersoft.theotown.theotown.components.disaster.CrimeDisaster;
import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;

/**
 * Created by Lobby on 28.11.2015.
 */
public class PoliceCarController extends OperationCarController {

    private CarDraft carDraft = (CarDraft) Drafts.ALL.get("$carpolice00");

    public PoliceCarController(CarSpawner spawner) {
        super(spawner, ActionPlaceType.POLICE);
    }

    @Override
    public void register(Car car) {
        super.register(car);
        car.flags |= Car.FLAG_POLICE;
    }

    @Override
    protected int getSoundID(Car car) {
        return Resources.SOUND_CAR_POLICE;
    }

    @Override
    public boolean onWorkDone(Car car, int parcelX, int parcelY) {
        DefaultCatastrophe catastrophe = (DefaultCatastrophe) city.getComponent(ComponentType.CATASTROPHE);
        CrimeDisaster crimeDisaster = catastrophe.getDisaster(CrimeDisaster.class);

        List<Building> reachableCrimeBuildings = new ArrayList<>();
        for (Building building : crimeDisaster.getCrimeBuildings()) {
            int distance = city.getDistance().get(building, parcelX / 2, parcelY / 2);
            if (distance <= 6) {
                reachableCrimeBuildings.add(building);
            }
        }

        if (!reachableCrimeBuildings.isEmpty()) {
            if (Resources.RND.nextFloat() < 0.1f) {
                int size = reachableCrimeBuildings.size();
                int i = Resources.RND.nextInt(size);
                crimeDisaster.removeCrime(reachableCrimeBuildings.get(i));
                return size == 1;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    protected List<Building> getStations() {
        return city.getBuildings().getBuildingsOfType(BuildingType.POLICE);
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        return carDraft;
    }

    @Override
    public String getName() {
        return "PoliceCarController";
    }

}

package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.draft.Draft;
import info.flowersoft.theotown.theotown.map.BuildingSampler;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;

/**
 * Created by lobby on 25.04.2017.
 */

public class GenericCarController extends OperationCarController {

    private BuildingDraft buildingDraft;

    private BuildingDraft.CarSpawnerDraft carSpawnerDraft;

    private List<BuildingDraft> targetBuildings;

    public GenericCarController(CarSpawner spawner, BuildingDraft buildingDraft, BuildingDraft.CarSpawnerDraft carSpawnerDraft) {
        super(spawner, -1);

        this.buildingDraft = buildingDraft;
        this.carSpawnerDraft = carSpawnerDraft;

        if (carSpawnerDraft.targetIds != null && carSpawnerDraft.targetIds.length > 0) {
            targetBuildings = new ArrayList<>();
            for (int i = 0; i < carSpawnerDraft.targetIds.length; i++) {
                String id = carSpawnerDraft.targetIds[i];
                if (id == null || id.isEmpty()) {
                    targetBuildings.add(buildingDraft);
                } else {
                    Draft targetDraft = Drafts.ALL.get(id);
                    if (targetDraft instanceof BuildingDraft) {
                        targetBuildings.add((BuildingDraft) targetDraft);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "GenericCarController(" + buildingDraft.id + "." + carSpawnerDraft.id + ")";
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
    protected void configureSampler(BuildingSampler sampler) {
        super.configureSampler(sampler);
        if (targetBuildings != null) {
            sampler.setBuildings(city.getBuildings().getBuildingsOfDraft(targetBuildings.get(Resources.RND.nextInt(targetBuildings.size()))));
        }
    }

    @Override
    protected CarDraft getCarDraft(Building building) {
        CarDraft[] cars = carSpawnerDraft.cars;
        return cars[Resources.RND.nextInt(cars.length)];
    }

    @Override
    protected int getStationRadius(Building building) {
        return carSpawnerDraft.radius;
    }

    @Override
    protected int getMaxCarCountOf(Building building) {
        return carSpawnerDraft.count;
    }

}

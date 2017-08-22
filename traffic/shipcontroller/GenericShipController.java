package info.flowersoft.theotown.theotown.components.traffic.shipcontroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.ShipDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Ship;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.stapel2d.util.ProbabilitySelector;
import info.flowersoft.theotown.theotown.util.ListSampler;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by Lobby on 14.04.2016.
 */
public class GenericShipController extends ShipController {

    private BuildingDraft buildingDraft;

    private List<ShipDraft> shipDrafts = new ArrayList<>();

    public GenericShipController(City city, ShipSpawner spawner, BuildingDraft buildingDraft) {
        super(city, spawner);
        this.buildingDraft = buildingDraft;
        for (int i = 0; i < buildingDraft.ships.length; i++) {
            shipDrafts.add(buildingDraft.ships[i]);
        }
    }

    @Override
    public void load(JsonReader src) throws IOException {
        while (src.hasNext()) {
            src.skipValue();
        }
    }

    @Override
    public void save(JsonWriter dest) throws IOException {

    }

    @Override
    public void onTarget(Ship ship) {
        if (city.isValid(ship.getX(), ship.getY()) && getMaxCount() >= ships.size()) {
            int[] posDir = new int[] {1, 2, 4, 8, 0};
            ProbabilitySelector<Integer> selector = new ProbabilitySelector<>(Resources.RND);

            int x = ship.getX() + Direction.differenceX(ship.getDir());
            int y = ship.getY() + Direction.differenceY(ship.getDir());
            for (int dir : posDir) {
                int nx = x + Direction.differenceX(dir);
                int ny = y + Direction.differenceY(dir);
                if (isSuitableForShip(nx, ny) && dir != Direction.opposite(ship.getDir())) {
                    selector.insert(dir, dir == ship.getDir() ? 8 : 1);
                }
            }
            if (selector.hasResult()) {
                ship.setTarget(selector.getResult());
            } else {
                ship.setInvalid();
            }
        } else {
            ship.setInvalid();
        }
    }

    @Override
    public void update() {
        int maxCount = getMaxCount();
        if (maxCount > ships.size()) {
            int[] pos = sampleSpawningPlace();
            if (pos != null) {
                int x = pos[0];
                int y = pos[1];
                ShipDraft draft = shipDrafts.get(Resources.RND.nextInt(shipDrafts.size()));
                spawner.spawn(draft, x, y, 0);
            }
        }
    }

    private int[] sampleSpawningPlace() {
        ProbabilitySelector<Integer> selector = new ProbabilitySelector<>(Resources.RND);

        Building port = new ListSampler<>(getSpawnerBuildings()).sample(Resources.RND);
        if (port != null && port.isWorking()) {
            for (int x = port.getX() - 1; x < port.getX() + port.getWidth() + 1; x++) {
                for (int y = port.getY() - 1; y < port.getY() + port.getHeight() + 1; y++) {
                    if ((x < port.getX() || y < port.getY() || x >= port.getX() + port.getWidth()
                            || y >= port.getY() + port.getHeight()) && isSuitableForShip(x, y)) {
                        selector.insert(x + y * city.getWidth() , port.getDraft().shipCount);
                    }
                }
            }
        }

        if (selector.hasResult()) {
            int pos = selector.getResult();
            return new int[] {pos % city.getWidth(), pos / city.getWidth()};
        } else {
            return null;
        }
    }

    private int getMaxCount() {
        int count = 0;

        for (Building building : new SafeListAccessor<>(getSpawnerBuildings())) {
            if (building.isWorking()) count += building.getDraft().shipCount;
        }

        return count;
    }

    private List<Building> getSpawnerBuildings() {
        return city.getBuildings().getBuildingsOfDraft(buildingDraft);
    }

    @Override
    public String getId() {
        return "generic " + buildingDraft.id + " controller";
    }
}

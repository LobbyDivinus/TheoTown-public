package info.flowersoft.theotown.theotown.components.traffic.shipcontroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.ShipDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Ship;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.stapel2d.util.ProbabilitySelector;
import info.flowersoft.theotown.theotown.util.DataAccessor;
import info.flowersoft.theotown.theotown.util.ListSampler;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by Lobby on 24.04.2016.
 */
public class FreighterShipController extends ShipController {
    public FreighterShipController(City city, ShipSpawner spawner) {
        super(city, spawner);
    }

    @Override
    public void load(JsonReader src) throws IOException {
        while (src.hasNext()) src.skipValue();
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
    }

    @Override
    public void onTarget(Ship ship) {
        int x = ship.getX() + Direction.differenceX(ship.getDir());
        int y = ship.getY() + Direction.differenceY(ship.getDir());
        int[] targetPos = getShipTarget(ship);
        int[] finalTargetPos = getShipFinalTarget(ship);
        int dx = targetPos[0] - ship.getX();
        int dy = targetPos[1] - ship.getY();
        int distance = Math.abs(dx) + Math.abs(dy);
        int lastDir = ship.getDir();
        int history = getShipHistory(ship);
        Building target = city.getTile(targetPos[0], targetPos[1]).building;
        Building finalTarget = city.getTile(finalTargetPos[0], finalTargetPos[1]).building;

        if (target == null || finalTarget == null || history > 128) {
            ship.setInvalid();
            return;
        }

        if (distance <= 4) {
            if (target != finalTarget) {
                target = getNextTarget(target, finalTarget);
                if (target == null) {
                    ship.setInvalid();
                    return;
                }
            } else {
                ship.setInvalid();
                return;
            }
        }

        if (dx != 0 || dy != 0) {
            if (Math.abs(dx) > Math.abs(dy)) {
                dx = dx / Math.abs(dx);
                dy = 0;
            } else {
                dx = 0;
                dy = dy / Math.abs(dy);
            }
            int prefDir = Direction.fromDifferential(dx, dy);

            ProbabilitySelector<Integer> selector = new ProbabilitySelector<>(Resources.RND);
            for (int dir : new int[] {0, 1, 2, 4, 8}) {
                if (isSuitableForShip(ship.getDraft(), x, y, dir)) {
                    if (dir == prefDir) {
                        selector.insert(dir, 16);
                    } else if (dir == 0 && lastDir != 0) {
                        selector.insert(dir, 1);
                    } else if (dir != 0 && dir == lastDir) {
                        selector.insert(dir, 4);
                    } else if (dir != 0 && dir == Direction.opposite(lastDir)) {
                        selector.insert(dir, 0.25f);
                    } else if (dir != 0) {
                        selector.insert(dir, 1);
                    }
                }
            }

            if (selector.hasResult()) {
                ship.setTarget(selector.getResult());
                setShipHistory(ship, history + 1);
                return;
            }
        }
        ship.setInvalid();
    }

    private int getDistance(Building start, Building target) {
        for (int i = 0; i < 4; i++) {
            int sx = getMidX(start, i);
            int sy = getMidY(start, i);
            for (int j = 0; j < 4; j++) {
                int tx = getMidX(target, j);
                int ty = getMidY(target, j);
                int distance = getDistance(sx, sy, tx, ty);
                if (distance < Integer.MAX_VALUE) {
                    return distance;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private int getDistance(int x0, int y0, int x1, int y1) {
        if (isSuitableForShip(x0, y0) && isSuitableForShip(x1, y1)) {
            int x = x0;
            int y = y0;
            int distance =  1;
            while (x != x1 || y != y1) {
                int dx = x1 - x;
                int dy = y1 - y;
                if (Math.abs(dx) > Math.abs(dy)) {
                    dx = dx / Math.abs(dx);
                    dy = 0;
                } else {
                    dx = 0;
                    dy = dy / Math.abs(dy);
                }
                x += dx;
                y += dy;
                if (!isSuitableForShip(x, y)) {
                    return Integer.MAX_VALUE;
                }
                distance++;
            }
            return distance;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    private int getMidX(Building building, int idx) {
        if (idx == 0) {
            return building.getX() + building.getWidth();
        } else if (idx == 1) {
            return building.getX() + building.getWidth() / 2;
        } else if (idx == 2) {
            return building.getX() - 1;
        } else {
            return building.getX() + building.getWidth() / 2;
        }
    }

    private int getMidY(Building building, int idx) {
        if (idx == 0) {
            return building.getY() + building.getHeight() / 2;
        } else if (idx == 1) {
            return building.getY() + building.getHeight();
        } else if (idx == 2) {
            return building.getY() + building.getHeight() / 2;
        } else {
            return building.getY() - 1;
        }
    }

    @Override
    public void update() {
        if (getMaxCount() > ships.size()) {
            ListSampler<Building> sampler = new ListSampler<>(getPiers());
            Building start = sampler.sample(Resources.RND);
            Building target = sampler.sample(Resources.RND);
            if (start != target && start != null && target != null) {
                ShipDraft draft = (ShipDraft) Drafts.ALL.get("$shipcontainer00");
                int [] startConfig = sampleStartConfig(draft, start);
                Building nextTarget = getNextTarget(start, target);
                if (startConfig != null && nextTarget != null) {
                    Ship ship = spawner.spawn(draft, startConfig[0], startConfig[1], startConfig[2]);
                    setShipTarget(ship, nextTarget.getX(), nextTarget.getY());
                    setShipFinalTarget(ship, target.getX(), target.getY());
                }
            }
        }
    }

    private Building getNextTarget(Building current, Building target) {
        if (getDistance(current, target) < Integer.MAX_VALUE) {
            return target;
        } else {
            final Map<Building, Integer> distances = new HashMap<>();
            Map<Building, Building> parents = new HashMap<>();
            PriorityQueue<Building> queue = new PriorityQueue<>(11, new Comparator<Building>() {
                @Override
                public int compare(Building lhs, Building rhs) {
                    return distances.get(rhs) - distances.get(lhs);
                }
            });

            final List<Building> buoys = new ArrayList<>();
            for (Building building : new SafeListAccessor<>(city.getBuildings().getBuildingsOfType(BuildingType.BUOY))) {
                buoys.add(building);
            }

            for (Building buoy : buoys) {
                distances.put(buoy, getDistance(current, buoy));
                queue.add(buoy);
                parents.put(buoy, null);
            }

            while (!queue.isEmpty()) {
                Building buoy = queue.poll();
                if (distances.get(buoy) < Integer.MAX_VALUE) {
                    buoys.remove(buoy);
                    if (getDistance(buoy, target) < Integer.MAX_VALUE) {
                        Building parent = buoy;
                        while (parents.get(parent) != null) {
                            parent = parents.get(parent);
                        }
                        return parent;
                    } else {
                        for (Building other : buoys) {
                            distances.put(other, getDistance(buoy, other));
                            queue.remove(other);
                            queue.add(other);
                        }
                    }
                } else {
                    break;
                }
            }
        }
        return null;
    }

    private int[] sampleStartConfig(ShipDraft draft, Building pier) {
        for (int x = pier.getX() - 1; x <= pier.getX() + pier.getWidth(); x++) {
            for (int y = pier.getY() - 1; y <= pier.getY() + pier.getHeight(); y++) {
                for (int dir = 1; dir <= 8; dir *= 2) {
                    if (isSuitableForShip(draft, x, y, dir)) {
                        return new int[] {x, y, dir};
                    }
                }
            }
        }
        return null;
    }

    private int getMaxCount() {
        return getPiers().size() - 1;
    }

    private List<Building> getPiers() {
        return city.getBuildings().getBuildingsOfType(BuildingType.HARBOR_PIER);
    }

    private void setShipTarget(Ship ship, int x, int y) {
        long data = ship.getData();
        data = DataAccessor.write(data, 12, 0, x);
        data = DataAccessor.write(data, 12, 12, y);
        ship.setData(data);
    }

    private void setShipFinalTarget(Ship ship, int x, int y) {
        long data = ship.getData();
        data = DataAccessor.write(data, 12, 24, x);
        data = DataAccessor.write(data, 12, 36, y);
        ship.setData(data);
    }

    private int[] getShipTarget(Ship ship) {
        long data = ship.getData();
        return new int[] {
                (int) DataAccessor.read(data, 12, 0),
                (int) DataAccessor.read(data, 12, 12)};
    }

    private int[] getShipFinalTarget(Ship ship) {
        long data = ship.getData();
        return new int[] {
                (int) DataAccessor.read(data, 12, 24),
                (int) DataAccessor.read(data, 12, 36)};
    }

    private void setShipHistory(Ship ship, int history) {
        long data = ship.getData();
        ship.setData(DataAccessor.write(data, 12, 48, history));
    }

    private int getShipHistory(Ship ship) {
        long data = ship.getData();
        return (int) DataAccessor.read(data, 12, 48);
    }

    @Override
    public String getId() {
        return "freighter controller";
    }
}

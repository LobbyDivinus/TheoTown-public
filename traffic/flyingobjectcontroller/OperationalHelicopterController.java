package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import info.flowersoft.theotown.theotown.components.DefaultTraffic;
import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceController;
import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceHandler;
import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.MapArea;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;
import info.flowersoft.theotown.theotown.util.DataAccessor;

/**
 * Created by Lobby on 12.08.2016.
 */
public abstract class OperationalHelicopterController extends SimpleHelicopterController implements ActionPlaceHandler {

    protected int actionPlaceType;

    protected ActionPlaceController actionPlaceController;

    public OperationalHelicopterController(City city, FlyingObjectSpawner spawner, int type) {
        super(city, spawner);
        actionPlaceType = type;
    }

    @Override
    public void prepare() {
        super.prepare();
        actionPlaceController = ((DefaultTraffic) city.getComponent(ComponentType.TRAFFIC)).getActionPlaceController();
        actionPlaceController.registerHandler(this, actionPlaceType);
    }

    @Override
    protected void finishedFlightCommand(FlyingObject flyingObject) {
        super.onTarget(flyingObject);

        if (isImportant(flyingObject)) {
            actionPlaceController.solverReached(actionPlaceType);
        }
    }

    @Override
    public void update() {
        super.update();

        List<Building> buildings = getBuildings();
        int size = buildings != null ? buildings.size() : 0;

        if (flyingObjects.size() < size && buildings != null) {
            Set<Building> set = new HashSet<>(buildings);
            for (int i = 0; i < flyingObjects.size(); i++) {
                Building building = getBuildingData(flyingObjects.get(i));
                if (building != null) {
                    set.remove(building);
                }
            }
            if (!set.isEmpty()) {
                Iterator<Building> iterator = set.iterator();
                Building source = null;
                while (iterator.hasNext() && (source = set.iterator().next()).inConstruction()) {
                    source = null;
                }

                if (source != null) {
                    FlyingObjectDraft helicopterDraft = getDraft(source);
                    int x = source.getX() + getInnerX(source);
                    int y = source.getY() + getInnerY(source);
                    FlyingObject flyingObject = create(helicopterDraft, x, y);
                    setBuildingData(flyingObject, source);
                }
            }
        } else if (flyingObjects.size() > size) {
            if (buildings != null) {
                for (int i = 0; i < flyingObjects.size(); i++) {
                    Building building = getBuildingData(flyingObjects.get(i));
                    if (building == null || !buildings.contains(building)) {
                        spawner.remove(flyingObjects.get(i));
                        break;
                    }
                }
            } else {
                spawner.remove(flyingObjects.get(0));
            }
        }
    }

    protected abstract FlyingObjectDraft getDraft(Building building);

    protected abstract List<Building> getBuildings();

    protected int getInnerX(Building building) {
        return building.getWidth() / 2;
    }

    protected int getInnerY(Building building) {
        return building.getHeight() / 2;
    }

    protected final void setBuildingData(FlyingObject flyingObject, Building building) {
        int x = building.getX();
        int y = building.getY();
        long data = flyingObject.data;
        data = DataAccessor.write(data, 10, 32, x);
        data = DataAccessor.write(data, 10, 42, y);
        flyingObject.data = data;
    }

    protected final Building getBuildingData(FlyingObject flyingObject) {
        long data = flyingObject.data;
        int x = (int) DataAccessor.read(data, 10, 32);
        int y = (int) DataAccessor.read(data, 10, 42);
        if (city.isValid(x, y)) {
            return city.getTile(x, y).building;
        } else {
            return null;
        }
    }

    protected final boolean isImportant(FlyingObject flyingObject) {
        return DataAccessor.read(flyingObject.data, 1, 52) == 1;
    }

    protected final void setImportant(FlyingObject flyingObject, boolean important) {
        flyingObject.data = DataAccessor.write(flyingObject.data, 1, 52, important ? 1 : 0);
    }

    protected void resetTarget(FlyingObject flyingObject) {
        flyToBase(flyingObject);
    }

    protected void flyToBase(FlyingObject flyingObject) {
        Building base = getBuildingData(flyingObject);
        if (base != null) {
            flyTo(flyingObject, base.getX() + getInnerX(base), base.getY() + getInnerY(base), true);
        }
    }

    @Override
    public void notifyAction(MapArea target, int type) {
        int x = (int) target.getCenterX();
        int y = (int) target.getCenterY();
        for (int i = 0; i < flyingObjects.size(); i++) {
            flyTo(flyingObjects.get(i), x, y, false);
            setImportant(flyingObjects.get(i), true);
            actionPlaceController.registerSolver(type);
        }
    }

    @Override
    public void notifyNoAction(int type) {
        for (int i = 0; i < flyingObjects.size(); i++) {
            setImportant(flyingObjects.get(i), false);
            resetTarget(flyingObjects.get(i));
        }
    }
}

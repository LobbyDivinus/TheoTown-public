package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import java.util.List;

import info.flowersoft.theotown.theotown.components.DefaultCatastrophe;
import info.flowersoft.theotown.theotown.components.disaster.MeteoriteDisaster;
import info.flowersoft.theotown.theotown.components.disaster.UfoDisaster;
import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.components.Date;
import info.flowersoft.theotown.theotown.map.components.Disaster;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.util.DataAccessor;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by lobby on 24.04.2017.
 */

public class RocketController extends FlyingObjectController {

    private BuildingDraft baseDraft;
    private FlyingObjectDraft rocketDraft;

    private Date date;

    private boolean causedDisaster;

    public RocketController(City city, FlyingObjectSpawner spawner) {
        super(city, spawner);

        baseDraft = (BuildingDraft) Drafts.ALL.get("$mltry_missilesilo00");
        rocketDraft = (FlyingObjectDraft) Drafts.ALL.get("$rocket_mltry00");

        date = (Date) city.getComponent(ComponentType.DATE);
    }

    @Override
    public void onTarget(FlyingObject flyingObject) {
        flyingObject.flyTo(0, 1, Math.round(1.2f * (flyingObject.getNextHeight() + 36)) - 34);

        if (flyingObject.getHeight() > 2048) {
            spawner.remove(flyingObject);
            onDespawnRocket();
        }
    }

    @Override
    public void onSpawned(FlyingObject flyingObject) {
        flyingObject.setSpeed(1);
    }

    @Override
    public void update() {
        List<Building> buildings = city.getBuildings().getBuildingsOfDraft(baseDraft);
        int animationTime = date.getAnimationTime();

        for (Building building : new SafeListAccessor<>(buildings)) {
            if (building.isWorking()) {
                int x = building.getX() + building.getWidth() / 2;
                int y = building.getY() + building.getHeight() / 2;
                Tile tile = city.getTile(x, y);
                if (tile.flyingObject == null) {
                    int timeShift = building.getAnimationTimeShift(animationTime);
                    if (building.getFrame() == 0) {
                        if (Resources.RND.nextFloat() < 0.05f) {
                            building.setFrame(1);
                            building.setAnimationTimeShift(animationTime, 0);
                        }
                    } else if (timeShift > 10000) {
                        building.setFrame(0);
                        building.setAnimationTimeShift(animationTime, 0);
                    } else if (timeShift > 2000) {
                        create(building, x, y);
                    }
                }
            }
        }
    }

    private void onDespawnRocket() {
        if (Resources.RND.nextFloat() < 0.001f) {
            DefaultCatastrophe catastrophe = (DefaultCatastrophe) city.getComponent(ComponentType.CATASTROPHE);
            if (!catastrophe.isActive()) {
                if (Resources.RND.nextFloat() < 0.9f) {
                    catastrophe.getDisaster(MeteoriteDisaster.class).issue();
                } else {
                    catastrophe.getDisaster(UfoDisaster.class).issue();
                }
                causedDisaster = true;
            }
        }
    }

    public boolean hasCausedDisasterAndReset() {
        boolean caused = causedDisaster;
        causedDisaster = false;
        return caused;
    }

    private FlyingObject create(Building building, int x, int y) {
        FlyingObject rocket = spawner.spawn(rocketDraft, x, y, 0, -36);
        setState(rocket, x, y);
        return rocket;
    }

    private void setState(FlyingObject rocket, int x, int y) {
        long data = rocket.data;
        data = DataAccessor.write(data, 10, 54, x);
        data = DataAccessor.write(data, 10, 44, y);
        rocket.data = data;
    }

    private int getStateX(FlyingObject rocket) {
        return (int) DataAccessor.read(rocket.data, 10, 54);
    }

    private int getStateY(FlyingObject rocket) {
        return (int) DataAccessor.read(rocket.data, 10, 44);
    }

    private Building getRocketBase(FlyingObject rocket) {
        int x = getStateX(rocket);
        int y = getStateY(rocket);

        if (city.isValid(x, y)) {
            return city.getTile(x, y).building;
        } else {
            return null;
        }
    }

    @Override
    public String getTag() {
        return "rocket";
    }
}

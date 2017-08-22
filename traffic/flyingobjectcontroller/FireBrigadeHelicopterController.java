package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceType;
import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.draft.UpgradeDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by lobby on 07.01.2017.
 */

public class FireBrigadeHelicopterController extends OperationalHelicopterController {

    private BuildingDraft buildingDraft;
    private UpgradeDraft neededUpgrade;
    private List<Building> buildings = new ArrayList<>();

    public FireBrigadeHelicopterController(City city, FlyingObjectSpawner spawner) {
        super(city, spawner, ActionPlaceType.FIRE);
    }

    @Override
    protected FlyingObjectDraft getDraft(Building building) {
        return (FlyingObjectDraft) Drafts.ALL.get("$helifirefighter00");
    }

    @Override
    protected List<Building> getBuildings() {
        if (buildingDraft == null) {
            buildingDraft = (BuildingDraft) Drafts.ALL.get("$firedepartment02");
            neededUpgrade = (UpgradeDraft) Drafts.ALL.get("$firedepartment02_heli00");
        }
        buildings.clear();

        List<Building> buildingSource = city.getBuildings().getBuildingsOfDraft(buildingDraft);
        if (buildingSource != null) {
            for (Building building : new SafeListAccessor<>(buildingSource)) {
                if (building.hasUpgrade(neededUpgrade)) buildings.add(building);
            }
        }

        return buildings;
    }

    @Override
    public String getTag() {
        return "fire brigade helicopter";
    }
}

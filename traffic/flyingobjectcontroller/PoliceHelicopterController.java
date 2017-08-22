package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import java.util.List;

import info.flowersoft.theotown.theotown.components.actionplace.ActionPlaceType;
import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.resources.Drafts;

/**
 * Created by Lobby on 12.08.2016.
 */
public class PoliceHelicopterController extends OperationalHelicopterController {

    public PoliceHelicopterController(City city, FlyingObjectSpawner spawner) {
        super(city, spawner, ActionPlaceType.POLICE);
    }

    @Override
    protected FlyingObjectDraft getDraft(Building building) {
        return (FlyingObjectDraft) Drafts.ALL.get("$helipolice00");
    }

    @Override
    protected List<Building> getBuildings() {
        return null;
    }

    @Override
    public String getTag() {
        return "police helicopter";
    }
}

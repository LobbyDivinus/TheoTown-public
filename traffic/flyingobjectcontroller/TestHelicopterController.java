package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.components.Tool;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Settings;

/**
 * Created by Lobby on 12.08.2016.
 */
public class TestHelicopterController extends SimpleHelicopterController {

    boolean initiated;

    public TestHelicopterController(City city, FlyingObjectSpawner spawner) {
        super(city, spawner);
    }

    @Override
    protected void finishedFlightCommand(FlyingObject flyingObject) {

    }

    @Override
    public void update() {
        super.update();

        boolean on = Settings.experimentalFeatures && !Settings.energySaving;
        if (on) {
            if (!initiated && city.getDefaultTool() != null) {
                if (flyingObjects.isEmpty()) {
                    FlyingObjectDraft draft = Drafts.FLYINGS.get(0);
                    create(draft, 16, 16);
                }

                city.getDefaultTool().registerClickListener(
                        new Tool.ClickListener() {
                            @Override
                            public void onClick(Tool tool, int x, int y, Tile tile) {
                                if (!flyingObjects.isEmpty()) {
                                    flyTo(flyingObjects.get(0), x, y, city.getTileHeight(x, y) == 0 && !city.getTile(x, y).ground.isWater());
                                }
                            }
                        });
                initiated = true;
            }
        } else {
            if (!flyingObjects.isEmpty()) {
                spawner.remove(flyingObjects.get(0));
            }
        }
    }

    @Override
    public String getTag() {
        return "test helicopter";
    }

}

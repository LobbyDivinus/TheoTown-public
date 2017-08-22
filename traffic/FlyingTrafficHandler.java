package info.flowersoft.theotown.theotown.components.traffic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.AirplaneController;
import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.FireBrigadeHelicopterController;
import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.RocketController;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;
import info.flowersoft.theotown.theotown.stapel2d.util.IntList;
import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.PoliceHelicopterController;
import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.SWATHelicopterController;
import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.FlyingObjectController;
import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.FlyingObjectSpawner;
import info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller.TestHelicopterController;
import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Drawer;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.components.Date;
import info.flowersoft.theotown.theotown.stapel2d.util.CyclicWorker;

/**
 * Created by Lobby on 05.08.2016.
 */
public class FlyingTrafficHandler extends TrafficHandler {

    private List<FlyingObject> flyingObjects;

    private List<FlyingObjectController> controllers;

    private Date date;

    private FlyingObject lastRemovedObject;

    public FlyingTrafficHandler(City city) {
        super(city);

        flyingObjects = new ArrayList<>();
        controllers = new ArrayList<>();

        controllers.add(new TestHelicopterController(city, newSpawner()));
        controllers.add(new PoliceHelicopterController(city, newSpawner()));
        controllers.add(new SWATHelicopterController(city, newSpawner()));
        controllers.add(new FireBrigadeHelicopterController(city, newSpawner()));
        controllers.add(new RocketController(city, newSpawner()));
        controllers.add(new AirplaneController(city, newSpawner()));
    }

    private FlyingObjectSpawner newSpawner() {
        return getSpawner(controllers.size());
    }

    @Override
    public void load(JsonReader src) throws IOException {
        Map<String, Integer> tagToId = new HashMap<>();
        Map<String, FlyingObjectController> tagToController = new HashMap<>();
        for (int i = 0; i < controllers.size(); i++) {
            FlyingObjectController controller = controllers.get(i);
            tagToId.put(controller.getTag(), i);
            if (tagToController.put(controller.getTag(), controller) != null) {
                throw new IllegalStateException("Two flying object controllers with tag " + controller.getTag());
            }
        }

        IntList controllerMapping = null;

        while (src.hasNext()) {
            switch (src.nextName()) {
                case "controller mapping":
                    controllerMapping = new IntList();
                    src.beginArray();
                    while (src.hasNext()) {
                        String tag = src.nextString();
                        Integer id = tagToId.get(tag);
                        if (id != null) {
                            controllerMapping.add(id);
                        } else {
                            controllerMapping.add(-1);
                        }
                    }
                    src.endArray();
                    break;
                case "controllers":
                    src.beginObject();
                    while (src.hasNext()) {
                        String tag = src.nextString();
                        FlyingObjectController controller = tagToController.get(tag);
                        if (controller != null) {
                            src.beginObject();
                            controller.load(src);
                            src.endObject();
                        } else {
                            src.skipValue();
                        }
                    }
                    src.endObject();
                    break;
                case "flyingObjects":
                    src.beginArray();
                    while (src.hasNext()) {
                        src.beginObject();
                        FlyingObject flyingObject = new FlyingObject(src);
                        src.endObject();
                        if (controllerMapping != null) {
                            flyingObject.setControllerId(controllerMapping.get(flyingObject.getControllerId()));
                        }
                        if (flyingObject.getControllerId() >= 0) {
                            flyingObjects.add(flyingObject);
                            register(flyingObject);
                        }
                    }
                    src.endArray();
                    break;
                default:
                    src.skipValue();
                    break;
            }
        }
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
        dest.name("controller mapping").beginArray();
        for (int i = 0; i < controllers.size(); i++) {
            FlyingObjectController controller = controllers.get(i);
            dest.value(controller.getTag());
        }
        dest.endArray();

        dest.name("controllers").beginObject();
        for (int i = 0; i < controllers.size(); i++) {
            FlyingObjectController controller = controllers.get(i);
            dest.name(controller.getTag()).beginObject();
            controller.save(dest);
            dest.endObject();
        }
        dest.endObject();

        dest.name("flyingObjects").beginArray();
        for (FlyingObject flyingObject : flyingObjects) {
            dest.beginObject();
            flyingObject.save(dest);
            dest.endObject();
        }
        dest.endArray();
    }

    private FlyingObjectSpawner getSpawner(final int controllerId) {
        return new FlyingObjectSpawner() {
            @Override
            public FlyingObject spawn(FlyingObjectDraft draft, int x, int y, int dir, int height) {
                return FlyingTrafficHandler.this.spawn(draft, x, y, dir, height, controllerId);
            }

            @Override
            public void remove(FlyingObject flyingObject) {
                FlyingTrafficHandler.this.remove(flyingObject);
            }
        };
    }

    public <T extends FlyingObjectController> T getController(Class<T> type) {
        for (int i = 0; i < controllers.size(); i++) {
            if (controllers.get(i).getClass().equals(type)) return (T) controllers.get(i);
        }
        return null;
    }

    private FlyingObject spawn(FlyingObjectDraft draft, int x, int y, int dir, int height, int controller) {
        FlyingObject flyingObject = new FlyingObject(draft, x, y, dir, height, controller);
        register(flyingObject);
        flyingObjects.add(flyingObject);
        getController(flyingObject).register(flyingObject);
        getController(flyingObject).onSpawned(flyingObject);
        return flyingObject;
    }

    private void remove(FlyingObject flyingObject) {
        unregister(flyingObject);
        getController(flyingObject).unregister(flyingObject);
        flyingObjects.remove(flyingObject);
        lastRemovedObject = flyingObject;
    }

    @Override
    public void prepare() {
        date = (Date) city.getComponent(ComponentType.DATE);

        for (FlyingObject flyingObject : flyingObjects) {
            getController(flyingObject).register(flyingObject);
        }

        addDaylyWorker(new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                synchronized (flyingObjects) {
                    for (int i = 0; i < controllers.size(); i++) {
                        controllers.get(i).update();
                    }
                }
            }
        }));

        addWorker(new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                synchronized (flyingObjects) {
                    int timeDelta = date.getAnimationTimeDelta();
                    float time = timeDelta / 1000f;
                    float progress = timeDelta / 300f;

                    for (int i = 0; i < flyingObjects.size(); i++) {
                        FlyingObject flyingObject = flyingObjects.get(i);

                        flyingObject.update(time);

                        if (flyingObject.getP() >= 1) {
                            unregister(flyingObject);
                            getController(flyingObject).onTarget(flyingObject);
                            if (flyingObject != lastRemovedObject) {
                                register(flyingObject);
                            } else {
                                lastRemovedObject = null;
                            }
                        }
                    }
                }
            }
        }));

        for (int i = 0; i < controllers.size(); i++) {
            controllers.get(i).prepare();
        }
    }

    private void register(FlyingObject flyingObject) {
        int x = flyingObject.getX();
        int y = flyingObject.getY();
        if (city.isValid(x, y)) {
            city.getTile(x, y).flyingObject = flyingObject;
        }
    }

    private void unregister(FlyingObject flyingObject) {
        int x = flyingObject.getX();
        int y = flyingObject.getY();
        if (city.isValid(x, y)) {
            city.getTile(x, y).flyingObject = null;
        }
    }

    private FlyingObjectController getController(FlyingObject flyingObject) {
        return controllers.get(flyingObject.getControllerId());
    }

    public void draw(int x, int y, Tile tile, Drawer d) {
        FlyingObject flyingObject = tile.flyingObject;
        if (flyingObject != null) {
            flyingObject.draw(d);
        }
    }
}

package info.flowersoft.theotown.theotown.components.traffic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import info.flowersoft.theotown.theotown.draft.BuildingDraft;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.stapel2d.util.IntList;
import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.components.traffic.shipcontroller.FreighterShipController;
import info.flowersoft.theotown.theotown.components.traffic.shipcontroller.ShipController;
import info.flowersoft.theotown.theotown.components.traffic.shipcontroller.ShipSpawner;
import info.flowersoft.theotown.theotown.components.traffic.shipcontroller.GenericShipController;
import info.flowersoft.theotown.theotown.draft.ShipDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.Drawer;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.components.ComponentType;
import info.flowersoft.theotown.theotown.map.components.Date;
import info.flowersoft.theotown.theotown.map.objects.Ship;
import info.flowersoft.theotown.theotown.resources.Constants;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.stapel2d.util.CyclicWorker;

/**
 * Created by Lobby on 14.04.2016.
 */
public class ShipTrafficHandler extends TrafficHandler {

    private final List<Ship> ships;

    private List<ShipController> controllers;

    private Date date;

    private int ctrlId = 0;

    public ShipTrafficHandler(City city) {
        super(city);

        date = (Date) city.getComponent(ComponentType.DATE);

        ships = new ArrayList<>();

        controllers = new ArrayList<>();
        controllers.add(new FreighterShipController(city, generateSpawner()));

        for (int i = 0; i < Drafts.BUILDINGS.size(); i++) {
            BuildingDraft buildingDraft = Drafts.BUILDINGS.get(i);
            if (buildingDraft.ships != null && buildingDraft.ships.length > 0) {
                controllers.add(new GenericShipController(city, generateSpawner(), buildingDraft));
            }
        }


    }

    private ShipSpawner generateSpawner() {
        final int myId = ctrlId++;
        return new ShipSpawner() {
            @Override
            public Ship spawn(ShipDraft draft, int x, int y, int dir) {
                if (city.isValid(x, y)) {
                    int frame = Resources.RND.nextInt(draft.frames.length / 4);
                    Ship ship = new Ship(draft, x, y, myId, frame);
                    ship.setTarget(dir);
                    ships.add(ship);
                    controllers.get(myId).registerShip(ship);

                    registerShipOnMap(ship);
                    return ship;
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public void load(JsonReader src) throws IOException {
        IntList controllerMapping = null;
        Map<String, Integer> idToController = new HashMap<>();
        for (int i = 0; i < controllers.size(); i++) {
            idToController.put(controllers.get(i).getId(), i);
        }

        while (src.hasNext()) {
            String name = src.nextName();
            switch (name) {
                case "mapping":
                    controllerMapping = new IntList();
                    src.beginArray();
                    int i = 0;
                    while (src.hasNext()) {
                        String controllerId = src.nextString();
                        Integer controllerIdx = idToController.get(controllerId);
                        if (controllerIdx != null) {
                            controllerMapping.add(controllerIdx);
                        } else {
                            controllerMapping.add(-1);
                        }
                        i++;
                    }
                    src.endArray();
                    break;
                case "ships":
                    src.beginArray();
                    while (src.hasNext()) {
                        src.beginObject();
                        Ship ship = new Ship(src);
                        src.endObject();

                        if (controllerMapping != null) {
                            if (controllerMapping.get(ship.getController()) >= 0) {
                                ship.setController(controllerMapping.get(ship.getController()));
                                ships.add(ship);
                            }
                        }
                    }
                    src.endArray();
                    break;
                default:
                    Integer controllerIdx = idToController.get(name);

                    if (controllerIdx != null) {
                        src.beginObject();
                        controllers.get(controllerIdx).load(src);
                        src.endObject();
                    } else {
                        src.skipValue();
                    }
            }
        }

        Iterator<Ship> it = ships.iterator();
        while (it.hasNext()) {
            Ship ship = it.next();
            int ctrlId = ship.getController();
            if (ctrlId >= 0 && ctrlId < controllers.size()) {
                controllers.get(ctrlId).registerShip(ship);
                registerShipOnMap(ship);
            } else {
                it.remove();
            }
        }
    }

    @Override
    public void save(JsonWriter dest) throws IOException {
        synchronized (ships) {
            dest.name("mapping");
            dest.beginArray();
            for (int i = 0; i < controllers.size(); i++) {
                dest.value(controllers.get(i).getId());
            }
            dest.endArray();

            dest.name("ships");
            dest.beginArray();
            for (Ship ship : ships) {
                if (!ship.isInvalid()) {
                    dest.beginObject();
                    ship.save(dest);
                    dest.endObject();
                }
            }
            dest.endArray();

            for (int i = 0; i < controllers.size(); i++) {
                ShipController controller = controllers.get(i);
                dest.name(controller.getId());
                dest.beginObject();
                controller.save(dest);
                dest.endObject();
            }
        }
    }

    @Override
    public void prepare() {
        addDaylyWorker(new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                synchronized (ships) {
                    for (ShipController shipController : controllers) {
                        shipController.update();
                    }
                }
            }
        }));

        addWorker(new CyclicWorker(new Runnable() {
            @Override
            public void run() {
                synchronized (ships) {
                    int timeDelta = date.getAnimationTimeDelta();
                    Iterator<Ship> it = ships.iterator();

                    while (it.hasNext()) {
                        Ship ship = it.next();

                        if (ship.isInvalid()) {
                            unregisterShipFromMap(ship);
                            controllers.get(ship.getController()).unregisterShip(ship);
                            it.remove();
                        } else {
                            float p = ship.getP();
                            p += timeDelta / 4000f * (0.5f + Resources.RND.nextFloat());
                            if (p >= 1) {
                                p = 1;
                                ship.setP(p);
                                unregisterShipFromMap(ship);
                                controllers.get(ship.getController()).onTarget(ship);
                                registerShipOnMap(ship);
                            } else {
                                ship.setP(p);
                            }
                        }
                    }
                }
            }
        }));
    }

    private void registerShipOnMap(Ship ship) {
        int x = ship.getX();
        int y = ship.getY();
        int dir = ship.getDir();
        int dx = Direction.differenceX(dir);
        int dy = Direction.differenceY(dir);
        if (dir == 0) {
            dx = 1; dy = 0;
        }
        if (city.isValid(x, y)) {
            city.getTile(x, y).ship = ship;
        }

        int length = ship.getDraft().length;
        x += dx * length;
        y += dy * length;
        if (city.isValid(x, y)) {
            city.getTile(x, y).ship = ship;
        }
    }

    private void unregisterShipFromMap(Ship ship) {
        int x = ship.getX();
        int y = ship.getY();
        int dir = ship.getDir();
        int dx = Direction.differenceX(dir);
        int dy = Direction.differenceY(dir);
        if (dir == 0) {
            dx = 1; dy = 0;
        }
        if (city.isValid(x, y)) {
            city.getTile(x, y).ship = null;
        }

        int length = ship.getDraft().length;
        x += dx * length;
        y += dy * length;
        if (city.isValid(x, y)) {
            city.getTile(x, y).ship = null;
        }
    }

    public void draw(int x, int y, Tile tile, Ship ship, Drawer d) {
        ShipDraft draft = ship.getDraft();
        float p = ship.getP();
        int dir = ship.getDir();
        int rotatedDir = Direction.rotateCW(dir, d.rotation);
        int vx = city.originalToRotatedX(x, y);
        int vy = city.originalToRotatedY(x, y);
        int vsx = city.originalToRotatedX(ship.getX(), ship.getY());
        int vsy = city.originalToRotatedY(ship.getX(), ship.getY());
        float dx = p * Direction.differenceX(rotatedDir) + vsx - vx;
        float dy = p * Direction.differenceY(rotatedDir) + vsy - vy;
        if (dy > 0 || dx < 0 || dx == dy) {
            int movementX = Math.round(Constants.TILE_WIDTH * (dx + dy + 1) / 2);
            int movementY = Math.round(Constants.TILE_HEIGHT * (dx - dy) / 2) + 4;
            int frame = 4 * ship.getFrame() + (Direction.toIndex(dir) + 4 - d.rotation) % 4;
            d.draw(Resources.IMAGE_WORLD, movementX, movementY, draft.frames[frame]);
        }
    }
}

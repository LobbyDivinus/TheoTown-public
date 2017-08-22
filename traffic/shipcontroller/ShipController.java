package info.flowersoft.theotown.theotown.components.traffic.shipcontroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.util.json.JsonReader;
import info.flowersoft.theotown.theotown.util.json.JsonWriter;
import info.flowersoft.theotown.theotown.draft.ShipDraft;
import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Direction;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.objects.Ship;

/**
 * Created by Lobby on 14.04.2016.
 */
public abstract class ShipController {

    protected City city;

    protected ShipSpawner spawner;

    protected List<Ship> ships;

    public ShipController(City city, ShipSpawner spawner) {
        ships = new ArrayList<>();
        this.city = city;
        this.spawner = spawner;
    }

    public abstract void load(JsonReader src) throws IOException;

    public abstract void save(JsonWriter dest) throws IOException;

    public void registerShip(Ship ship) {
        ships.add(ship);
    }

    public void unregisterShip(Ship ship) {
        ships.remove(ship);
    }

    public abstract void onTarget(Ship ship);

    public abstract void update();

    protected boolean isSuitableForShip(int x, int y) {
        if (city.isValid(x, y)) {
            Tile tile = city.getTile(x, y);
            return tile.building == null && !tile.hasRoad() && tile.wire == null
                    && tile.ground.isWater() && !tile.ground.isBorder() && tile.ship == null;
        }
        return false;
    }

    protected boolean isSuitableForShip(ShipDraft ship, int x, int y, int dir) {
        int length = ship.length;
        int dx = Direction.differenceX(dir);
        int dy = Direction.differenceY(dir);
        if (dir == 0) {
            dx = 1; dy = 0;
        }
        for (int i = 0; i <= length; i++) {
            if (!isSuitableForShip(x + i * dx, y + i * dy)) {
                return false;
            }
        }

        return true;
    }

    public abstract String getId();
}

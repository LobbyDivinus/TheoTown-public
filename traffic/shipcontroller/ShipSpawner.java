package info.flowersoft.theotown.theotown.components.traffic.shipcontroller;

import info.flowersoft.theotown.theotown.draft.ShipDraft;
import info.flowersoft.theotown.theotown.map.objects.Ship;

/**
 * Created by Lobby on 15.04.2016.
 */
public interface ShipSpawner {
    Ship spawn(ShipDraft draft, int x, int y, int dir);
}

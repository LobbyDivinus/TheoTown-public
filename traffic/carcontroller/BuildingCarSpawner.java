package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.Way;

/**
 * Created by Lobby on 24.12.2016.
 */
public interface BuildingCarSpawner {
    void spawn(CarDraft draft, int frame, Building start, Building target, int startX, int startY,
               int startLevel, int targetX, int targetY, int targetLevel);
    void spawn(CarDraft draft, int frame, int startX, int startY, int targetX, int targetY, Way way);
}

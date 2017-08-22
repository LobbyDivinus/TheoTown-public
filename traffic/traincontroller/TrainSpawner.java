package info.flowersoft.theotown.theotown.components.traffic.traincontroller;

import info.flowersoft.theotown.theotown.draft.TrainDraft;
import info.flowersoft.theotown.theotown.map.objects.Train;

/**
 * Created by Lobby on 10.06.2016.
 */
public interface TrainSpawner {
    void spawn(TrainDraft draft, int x, int y, int dir, int level, long data);
    void driveTo(Train train, int x, int y);
    void remove(Train train);
}

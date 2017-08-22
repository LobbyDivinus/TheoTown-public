package info.flowersoft.theotown.theotown.components.traffic.flyingobjectcontroller;

import info.flowersoft.theotown.theotown.draft.FlyingObjectDraft;
import info.flowersoft.theotown.theotown.map.objects.FlyingObject;

/**
 * Created by Lobby on 05.08.2016.
 */
public interface FlyingObjectSpawner {
    FlyingObject spawn(FlyingObjectDraft draft, int x, int y, int dir, int height);
    void remove(FlyingObject flyingObject);
}

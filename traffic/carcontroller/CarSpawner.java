package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.MapArea;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.map.objects.Way;

/**
 * Created by Lobby on 11.09.2015.
 */
public interface CarSpawner {

    void spawn(MapArea start, MapArea target, CarController controller, CarDraft draft, long data);

    void spawn(MapArea start, MapArea target, CarController controller, CarDraft draft, int frame, long data);

    void spawn(CarController controller, CarDraft draft, int frame, long data, Way way);

    void driveTo(Car car, MapArea target);

    void wait(Car car);

}

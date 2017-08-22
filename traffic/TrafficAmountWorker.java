package info.flowersoft.theotown.theotown.components.traffic;

import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.map.Tile;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.map.objects.Road;
import info.flowersoft.theotown.theotown.stapel2d.util.CyclicWorker;
import info.flowersoft.theotown.theotown.stapel2d.util.MinimumSelector;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by Lobby on 24.03.2016.
 */
public class TrafficAmountWorker extends CyclicWorker implements Runnable {

    private City city;

    private int width;

    private int height;

    private int levelStride;

    private Road badRoad;

    private Car[] cars;

    public TrafficAmountWorker(City city, Car[] cars) {
        this.city = city;
        width = city.getWidth();
        height = city.getHeight();
        levelStride = 4 * width * height;
        this.cars = cars;

        setTask(this);
    }

    @Override
    public void run() {
        final MinimumSelector<Road> minimumSelector = new MinimumSelector<>();

        for (Road road : new SafeListAccessor<>(city.getRoads())) {
            int x = road.getX();
            int y = road.getY();

            int amount = 0;
            for (int i = 0; i < 4 && amount == 0; i++) {
                Car car = cars[(x + y * width) * 4 + i + road.level * levelStride];
                if (car != null && !car.paused) amount++;
            }

            float oldAmount = road.trafficAmount;
            float alpha = (amount > oldAmount) ? 0.025f : 0.1f;
            road.trafficAmount = Math.min((1 - alpha) * oldAmount + alpha * amount, 1f);

            minimumSelector.assume(road, 1 - road.trafficAmount);
        }

        if (minimumSelector.hasResult()) {
            badRoad = minimumSelector.getMinimum();
        } else {
            badRoad = null;
        }
    }

    public Road getBadRoad() {
        return badRoad;
    }
}

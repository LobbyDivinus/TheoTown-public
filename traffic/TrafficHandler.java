package info.flowersoft.theotown.theotown.components.traffic;

import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.map.City;
import info.flowersoft.theotown.theotown.stapel2d.drawing.Engine;
import info.flowersoft.theotown.theotown.stapel2d.util.CyclicWorker;
import info.flowersoft.theotown.theotown.util.Saveable;

/**
 * Created by Lobby on 14.04.2016.
 */
public abstract class TrafficHandler implements Saveable {

    protected City city;

    private List<CyclicWorker> workers;

    private List<CyclicWorker> daylyWorkers;

    public TrafficHandler(City city) {
        this.city = city;
        workers = new ArrayList<>();
        daylyWorkers = new ArrayList<>();
    }

    protected void addWorker(CyclicWorker worker) {
        workers.add(worker);
    }

    protected void addDaylyWorker(CyclicWorker worker) {
        daylyWorkers.add(worker);
    }

    public abstract void prepare();

    public void nextDay() {
        for (CyclicWorker worker : daylyWorkers) {
            worker.start();
        }
    }

    public void update() {
        for (CyclicWorker worker : workers) {
            worker.start();
        }
    }

    public void drawAfter(Engine engine) {
    }

    public void dispose() {
        joinWorkers();
    }

    protected void joinWorkers() {
        for (CyclicWorker w : workers) {
            w.join();
        }
        workers.clear();

        for (CyclicWorker w : daylyWorkers) {
            w.join();
        }
        daylyWorkers.clear();
    }

}

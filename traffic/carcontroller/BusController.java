package info.flowersoft.theotown.theotown.components.traffic.carcontroller;

import java.util.ArrayList;
import java.util.List;

import info.flowersoft.theotown.theotown.draft.BuildingType;
import info.flowersoft.theotown.theotown.draft.CarDraft;
import info.flowersoft.theotown.theotown.map.MapArea;
import info.flowersoft.theotown.theotown.map.objects.Building;
import info.flowersoft.theotown.theotown.map.objects.BusStop;
import info.flowersoft.theotown.theotown.map.objects.Car;
import info.flowersoft.theotown.theotown.map.objects.Road;
import info.flowersoft.theotown.theotown.resources.Drafts;
import info.flowersoft.theotown.theotown.resources.Resources;
import info.flowersoft.theotown.theotown.stapel2d.util.ProbabilitySelector;
import info.flowersoft.theotown.theotown.util.SafeListAccessor;

/**
 * Created by Lobby on 11.09.2015.
 */
public class BusController extends CarController {

    private boolean removeBus;

    public BusController(CarSpawner spawner) {
        super(spawner);
    }

    @Override
    public boolean onTarget(final Car bus, int parcelX, int parcelY) {
        Road targetRoad = city.getTile(parcelX / 2, parcelY / 2).getRoad(0);
        if (targetRoad != null) {
            BusStop target = targetRoad.getBusStop();
            if (target != null) {
                if (bus.way.size() > 2) {
                    spawner.wait(bus);
                    return false;
                }
                target.setLastVisit(getDate().getAbsoluteDay());
                target.removeWaiters(bus.draft.capacity);
            }
        }

        if (!removeBus) {
            final ProbabilitySelector<BusStop> selector
                    = new ProbabilitySelector<>(Resources.RND);

            for (BusStop busStop : new SafeListAccessor<>(city.getBusStops())) {
                int distance = city.getDistance().get(busStop.getX(), busStop.getY(),
                        bus.x / 2, bus.y / 2);
                if (distance > 1) {
                    int delta = (int) (getDate().getAbsoluteDay() - busStop.getLastVisit());
                    int waiting = busStop.getWaiting();

                    selector.insert(busStop, (float) waiting / distance + 0.01f);
                }
            }

            if (selector.hasResult()) {
                BusStop b = selector.getResult();
                MapArea target = new MapArea();
                target.add(b.getX(), b.getY(), b.level);

                spawner.driveTo(bus, target);

                return false;
            } else {
                return true;
            }
        } else {
            removeBus = false;
            return true;
        }
    }

    @Override
    public void update() {
        List<Building> busDepots = new ArrayList<>();
        for (Building b : new SafeListAccessor<>(city.getBuildings().getBuildingsOfType(BuildingType.BUS_DEPOT))) {
            if (b.getDraftId().equals("$busdepot00") && b.isWorking()) {
                busDepots.add(b);
            }
        }
        int maxBusCount = 16 * busDepots.size();

        if (maxBusCount > cars.size() && city.getBusStops().size() >= 2) {
            final ProbabilitySelector<BusStop> selector = new ProbabilitySelector<>(Resources.RND);
            for (BusStop busStop : new SafeListAccessor<>(city.getBusStops())) {
                selector.insert(busStop, 1);
            }

            BusStop a = selector.getResult();

            MapArea start = new MapArea();
            start.add(busDepots.get(Resources.RND.nextInt(busDepots.size())));

            MapArea target = new MapArea();
            target.add(a.getX(), a.getY(), a.level);

            CarDraft draft = (CarDraft) Drafts.ALL.get("$carbus00");

            spawner.spawn(start, target, this, draft, 0);

            removeBus = false;
        } else if (maxBusCount < cars.size()) {
            removeBus = true;
        }
    }

    @Override
    public String getName() {
        return "BusController";
    }

}

package nl.trivento.fastdata.travelclear.routes;

import nl.trivento.fastdata.travelclear.routes.entities.*;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Transaction;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MutableGraphDao extends GraphDao implements GenericMutableDao, Sink {
    private static final Logger logger = LoggerFactory.getLogger(MutableGraphDao.class);
    private List<Geo> nodes = new ArrayList<>();
    private final Session session;
    private Map<Integer, List<StopTime>> stopTimesForTrip = new HashMap<>();
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(6, 6, 0, TimeUnit.NANOSECONDS, new LinkedBlockingDeque<>());

    public MutableGraphDao(Session session) throws IOException {
        super(session);
        this.session = session;
    }

    public Geo getStopById(String id) throws IOException {
        return session.readOp(tx -> tx.geoMap().get(id));
    }

    private static final class ResolveWalkDistance implements Runnable {
        public static final EnumSet<TransportMode> WALK = EnumSet.of(TransportMode.Walk);
        private final List<Geo> nodes;
        private final Integer index;
        private final Session session;

        public ResolveWalkDistance(Session session, List<Geo> nodes, int index) {
            this.nodes = nodes;
            this.index = index;
            this.session = session;
        }

        @Override
        public void run() {
            Geo node = nodes.get(index);
            for (int otherIndex = 0; otherIndex < index; otherIndex++) {
                Geo other = nodes.get(otherIndex);

                int dist = other.walkSecondsDistance(node);
                if (dist <= 900) {
                    other.addContinuousEdge("Walk", node.getId(), WALK, dist);
                    node.addContinuousEdge("Walk", other.getId(), WALK, dist);
                }
            }
        }
    }

    private static final class AddTripSchedule implements Runnable {
        private final Session session;
        private final Map<Integer, List<StopTime>> stopTimesForTrip;

        public AddTripSchedule(Session session, Map<Integer, List<StopTime>> stopTimesForTrip) {
            this.session = session;
            this.stopTimesForTrip = stopTimesForTrip;
        }

        @Override
        public void run() {
            try {
                session.writeOp(tx -> {
                    Map<String, Geo> items = new HashMap<>();
                    stopTimesForTrip.forEach((tripId, stopTimesForTrip) -> {
                        Collections.sort(stopTimesForTrip);
                        for (int s = 0; s < stopTimesForTrip.size() - 1; s++) {
                            StopTime startTime = stopTimesForTrip.get(s);
                            Geo start = items.computeIfAbsent(startTime.getStopId(), geo -> tx.geoMap().get(geo));
                            //Trip trip = tx.tripMap().get(tripId);
                            //System.out.println("trip: " + trip.getId());
//                            for (int n = s + 1; n < stopTimesForTrip.size(); n++) {
//                                StopTime endTime = stopTimesForTrip.get(n);
//                                start.addScheduledEdge(
//                                        tripId,
//                                        endTime.getStopId(),
//                                        0,
//                                        startTime.getDepartureTime(),
//                                        endTime.getArrivalTime());
//                                items.put(start.getId(), start);
//                            }

                            StopTime endTime = stopTimesForTrip.get(s + 1);
                            start.addScheduledEdge(
                                    tripId,
                                    endTime.getStopId(),
                                    0,
                                    startTime.getDepartureTime(),
                                    endTime.getArrivalTime());
                            items.put(start.getId(), start);

//                            if (s + 2 < stopTimesForTrip.size()) {
//                                endTime = stopTimesForTrip.get(s + 2);
//                                start.addScheduledEdge(
//                                        tripId,
//                                        endTime.getStopId(),
//                                        0,
//                                        startTime.getDepartureTime(),
//                                        endTime.getArrivalTime());
//                                items.put(start.getId(), start);
//                            }
                        }
                    });
                    long start = System.currentTimeMillis();
                    tx.geoMap().putAll(items);
                    logger.info("Storing took " + (System.currentTimeMillis() - start) + " ms");
                });
            } catch (IOException e) {
                logger.error("Could not commit edges", e);
            }
        }
    }

    @Override
    public void flush() {

        if (!stopTimesForTrip.isEmpty()) {
//            stopTimesForTrip.forEach((tripId, stopTimes) ->
//                    executor.execute(new AddTripSchedule(session, tripId, stopTimes)));
            Iterator<Map.Entry<Integer, List<StopTime>>> it = stopTimesForTrip.entrySet().iterator();
            while (!stopTimesForTrip.isEmpty()) {
                Map<Integer, List<StopTime>> batch = new HashMap<>();
                for (int i = 0; i < 1000 && !stopTimesForTrip.isEmpty(); i++) {
                    Map.Entry<Integer, List<StopTime>> next = it.next();
                    batch.put(next.getKey(), next.getValue());
                    it.remove();
                }
                logger.info("Todo: " + stopTimesForTrip.size());
                //executor.execute(new AddTripSchedule(session, batch));
                new AddTripSchedule(session, batch).run();
            }
        }

        while (executor.getQueue().size() > 0) {
            logger.info("Todo: " + executor.getQueue().size());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        try {
            session.writeOp(tx -> {
                tx.geoMap().putAll(nodes.stream().collect(Collectors.toMap(Geo::getId, Function.identity())));

                for (Geo g : nodes) {
                    if (!tx.geoMap().get(g.getId()).equals(g)) {
                        throw new RuntimeException(g.toString());
                    }
                }
            });
            //session.writeOp(tx -> tx.geoMap().putAll(edged));
            //edged.putAll(nodes.stream().collect(Collectors.toMap(Geo::getId, Function.identity())));

            nodes.clear();
            session.writeOp(Transaction::commit);
        } catch (IOException e) {
            logger.error("Could not flush to DB", e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        session.close();
    }

    @Override
    public void saveEntity(Object entity) {
        if (GeoImpl.class.isAssignableFrom(entity.getClass())) {
            saveNode(GeoImpl.class.cast(entity));
        } else if (StopTime.class.isAssignableFrom(entity.getClass())) {
            saveEdge(StopTime.class.cast(entity));
        } else if (Trip.class.isAssignableFrom(entity.getClass())) {
            saveTrip(Trip.class.cast(entity));
        } else if (Route.class.isAssignableFrom(entity.getClass())) {
            saveRoute(Route.class.cast(entity));
        }
    }

    private void saveRoute(Route route) {
        try {
            session.writeOp(tx -> tx.routeMap().put(route.getId(), route));
            if (!session.readOp(tx -> tx.routeMap().get(route.getId()).equals(route))) {
                logger.info("Failed");
            }
        } catch (IOException e) {
            logger.error("Could not commit " + route, e);
        }
    }

    private void saveTrip(Trip trip) {
        try {
            session.writeOp(tx -> tx.tripMap().put(trip.getId(), trip));
        } catch (IOException e) {
            logger.error("Could not commit " + trip, e);
        }
    }

    private void saveEdge(StopTime stopTime) {
        stopTimesForTrip.computeIfAbsent(stopTime.getTripId(), id -> new Vector<>()).add(stopTime);
    }

    private void saveNode(final GeoImpl node) {
        nodes.add(node);
        executor.execute(new ResolveWalkDistance(session, nodes, nodes.size() - 1));
    }

    @Override
    public void process(EntityContainer entityContainer) {
        try {
            session.writeOp(tx -> {
                switch (entityContainer.getEntity().getType()) {
                    case Way:
                        Way way = (Way) entityContainer.getEntity();
                        List<WayNode> wayNodes = way.getWayNodes();
                        for (int i = 0; i < wayNodes.size() - 1; i++) {
                            Geo geo1 = tx.geoMap().get("osm" + way.getWayNodes().get(i).getNodeId());
                            Geo geo2 = tx.geoMap().get("osm" + way.getWayNodes().get(i + 1).getNodeId());
                            tagsToEdge(way.getTags(), Reference.direct(geo1.getId(), geo1), Reference.direct(geo2.getId(), geo2));
                        }
                    case Bound:
                        break;
                    case Node:
                        Node node = (Node) entityContainer.getEntity();
                        //System.out.println(Arrays.toString(node.getTags().toArray(new Tag[0])));
                        tx.geoMap().put("osm" + node.getId(), new GeoImpl("osm" + node.getId(), null, null, node.getLatitude(), node.getLongitude()));
                    case Relation:
                        break;
                    default:
                }
            });
        } catch (IOException e) {
            logger.error("Could not commit edge", e);
        }
    }

    private void tagsToEdge(Collection<Tag> tags, Reference<String, Geo> from, Reference<String, Geo> to) {
        Optional<Tag> highway = tags.stream().filter(tag -> tag.getKey().equals("highway")).findFirst();
        Optional<Tag> building = tags.stream().filter(tag -> tag.getKey().equals("building")).findFirst();
        if (!building.isPresent() && highway.isPresent()) {
            switch (highway.get().getValue()) {
                case "residential":
                    Integer kmh = tags.stream()
                            .filter(tag -> tag.getKey().equals("maxspeed"))
                            .map(t -> Integer.parseInt(t.getValue()))
                            .findFirst()
                            .orElse(50);

                    Integer ms = (int) (from.resolve().distance(to.resolve()) * kmh / 3.6);
                    new ContinuousEdge(null, to, EnumSet.of(TransportMode.Car, TransportMode.Taxi), ms);
                default:
                    System.out.println(Arrays.toString(tags.toArray(new Tag[0])));
            }

        }
    }

    @Override
    public void initialize(Map<String, Object> metaData) {

    }

    @Override
    public void complete() {
    }

    @Override
    public void open() {

    }

    @Override
    public void updateEntity(Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public void saveOrUpdateEntity(Object entity) {
        throw new NotImplementedException();
    }

    @Override
    public <K extends Serializable, T extends IdentityBean<K>> void removeEntity(T entity) {
        throw new NotImplementedException();
    }

    @Override
    public <T> void clearAllEntitiesForType(Class<T> type) {
        throw new NotImplementedException();
    }
}

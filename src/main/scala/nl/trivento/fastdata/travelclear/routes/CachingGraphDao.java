package nl.trivento.fastdata.travelclear.routes;

import nl.trivento.fastdata.travelclear.routes.entities.*;
import org.onebusaway.gtfs.services.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CachingGraphDao implements GenericDao {
    private final Logger logger = LoggerFactory.getLogger(CachingGraphDao.class);
    private Map<String, Geo> geos = new HashMap<>();
    private Map<Integer, Route> routes = new HashMap<>();
    private Map<Integer, Trip> trips = new HashMap<>();

    public CachingGraphDao(GenericDao internal) {
        logger.info("loading routes");
        internal.getAllEntitiesForType(Route.class).forEach(route -> routes.put(route.getId(), route));
        logger.info("loading trips");
        internal.getAllEntitiesForType(Trip.class).forEach(trip -> trips.put(trip.getId(), trip));

        logger.info("loading geo's");
        internal.getAllEntitiesForType(Geo.class).forEach(geo -> geos.put(geo.getId(), geo));

        logger.info("resolving references");
        geos.values().forEach(geo -> geo.resolveReferences(
                new Resolver() {
                    @Override
                    public Reference<Integer, Trip> resolveTrip(Reference<Integer, Trip> toResolve) {
                        return Reference.lazy(toResolve.id(), id -> trips.get(id).resolveReferences(this));
                    }

                    @Override
                    public Reference<Integer, Route> resolveRoute(Reference<Integer, Route> toResolve) {
                        return Reference.lazy(toResolve.id(), id -> routes.get(id));
                    }

                    @Override
                    public Reference<String, Geo> resolveGeo(Reference<String, Geo> toResolve) {
                        return Reference.direct(toResolve.id(), geos.get(toResolve.id()));
                    }
                }
        ));
//        true,
//                tripId -> Reference.lazy(tripId, trips.get(tripId).resolveReferences(true, id -> null, routeId -> routes.get(routeId), id -> null),
//                routeId -> routes.get(routeId),
//                geoId -> geos.get(geoId));

        logger.info("ready for use");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getAllEntitiesForType(Class<T> type) {
        if (type == Geo.class) return (Collection) geos.values();
        if (type == Route.class) return (Collection) routes.values();
        if (type == Trip.class) return (Collection) trips.values();
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getEntityForId(Class<T> type, Serializable id) {
        if (type == Geo.class) return (T) geos.get(id);
        if (type == Route.class) return (T) routes.get(id);
        if (type == Trip.class) return (T) trips.get(id);
        return null;
    }
}

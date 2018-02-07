package nl.trivento.fastdata.travelclear.routes.entities.serialization;

import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.Route;
import nl.trivento.fastdata.travelclear.routes.entities.Trip;

import java.util.Map;

public interface Transaction extends AutoCloseable {
    Map<String, Geo> geoMap();
    Map<Integer, Trip> tripMap();
    Map<Integer, Route> routeMap();
    Trip resolveTrip(int id);
    Route resolveRoute(int id);
    Geo resolveGeo(String id);

    void commit();
    void flush();
    default void close() {
        flush();
    }
}

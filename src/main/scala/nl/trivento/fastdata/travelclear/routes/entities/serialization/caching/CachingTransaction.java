package nl.trivento.fastdata.travelclear.routes.entities.serialization.caching;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Transaction;
import nl.trivento.fastdata.travelclear.routes.entities.Route;
import nl.trivento.fastdata.travelclear.routes.entities.Trip;

import java.util.Map;

public class CachingTransaction implements Transaction {
    private final Map<String, Geo> geoMapCache;
    private final Map<Integer, Trip> tripMapCache;
    private final Map<Integer, Route> routeMapCache;
    private final Transaction source;

    private <K, V> Map<K, V> cache(Map<K, V> parent, int size) {
        return new ConcurrentLinkedHashMap.Builder<K, V>()
                .concurrencyLevel(8)
                .initialCapacity(size)
                .maximumWeightedCapacity(size)
                .listener(parent::put)
                .build();
    }

    public CachingTransaction(Transaction source) {
        this.source = source;
        this.geoMapCache = cache(source.geoMap(), 100000);
        this.tripMapCache = cache(source.tripMap(), 100000);
        this.routeMapCache = cache(source.routeMap(), 100000);
    }

    @Override
    public Map<String, Geo> geoMap() {
        return geoMapCache;
    }

    @Override
    public Map<Integer, Trip> tripMap() {
        return tripMapCache;
    }

    @Override
    public Map<Integer, Route> routeMap() {
        return routeMapCache;
    }

    @Override
    public Trip resolveTrip(int id) {
        return tripMapCache.get(id);
    }

    @Override
    public Route resolveRoute(int id) {
        return routeMapCache.get(id);
    }

    @Override
    public Geo resolveGeo(String id) {
        return geoMapCache.get(id);
    }

    @Override
    public void commit() {
        source.commit();
    }

    @Override
    public void flush() {
        source.geoMap().putAll(geoMapCache);
        source.tripMap().putAll(tripMapCache);
        source.routeMap().putAll(routeMapCache);

        source.flush();
    }

    @Override
    public void close() {
        source.close();
    }
}

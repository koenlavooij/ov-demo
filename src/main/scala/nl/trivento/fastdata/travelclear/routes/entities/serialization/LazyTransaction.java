package nl.trivento.fastdata.travelclear.routes.entities.serialization;

import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.Route;
import nl.trivento.fastdata.travelclear.routes.entities.Trip;

import java.lang.reflect.Proxy;
import java.util.Map;

public abstract class LazyTransaction implements Transaction {
    @Override
    public Trip resolveTrip(int id) {
        return proxy(tripMap(), Trip.class, id);
    }

    @Override
    public Route resolveRoute(int id) {
        return proxy(routeMap(), Route.class, id);
    }

    @Override
    public Geo resolveGeo(String id) {
        return proxy(geoMap(), Geo.class, id);
    }

    private <K, V> V proxy(Map<K, V> map, Class<V> cls, K key) {
        return cls.cast(
                Proxy.newProxyInstance(
                        ClassLoader.getSystemClassLoader(),
                        new Class[] { cls },
                        (proxy, method, args) -> {
                            V real = map.get(key);
                            return method.invoke(real, args);
                        }));
    }

}

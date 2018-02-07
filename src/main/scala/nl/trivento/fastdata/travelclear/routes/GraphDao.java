package nl.trivento.fastdata.travelclear.routes;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.SessionFactory;
import org.onebusaway.gtfs.services.GenericDao;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class GraphDao implements GenericDao, AutoCloseable {
    private final Session session;

    public GraphDao(Session session) throws IOException {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    private <K, V, R> R withMap(Class<V> type, Function<Map<K, V>, R> f) {
        try {
            switch (type.getName()) {
                case "nl.trivento.fastdata.travelclear.routes.entities.Geo": {
                    return (R) session.readOp(transaction -> f.apply((Map) transaction.geoMap()));
                }
                case "nl.trivento.fastdata.travelclear.routes.entities.Route": {
                    return (R) session.readOp(transaction -> f.apply((Map) transaction.routeMap()));
                }
                case "nl.trivento.fastdata.travelclear.routes.entities.Trip": {
                    return (R) session.readOp(transaction -> f.apply((Map) transaction.tripMap()));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return null;
    }

    @Override
    public final <T> Collection<T> getAllEntitiesForType(Class<T> type) {
        try {
            return session.readOp(tx -> withMap(type, Map::values));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public final <T> T getEntityForId(Class<T> type, Serializable id) {
        try {
            return session.readOp(tx -> withMap(type, m -> m.get(id)));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void close() throws Exception {
        session.close();
    }
}

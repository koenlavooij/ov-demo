package nl.trivento.fastdata.travelclear.routes.entities.serialization.lmdb;

import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.LazyTransaction;
import nl.trivento.fastdata.travelclear.routes.entities.Route;
import nl.trivento.fastdata.travelclear.routes.entities.Trip;
import org.lmdbjava.Dbi;
import org.lmdbjava.Txn;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

class LMDBTransaction extends LazyTransaction {
    final Txn<ByteBuffer> tx;
    private final Dbi<ByteBuffer> geoDbi;
    private final Dbi<ByteBuffer> tripDbi;
    private final Dbi<ByteBuffer> routeDbi;

    private Map<String, Geo> geoMap;
    private Map<Integer, Trip> tripMap;
    private Map<Integer, Route> routeMap;

    LMDBTransaction(Txn<ByteBuffer> tx, Dbi<ByteBuffer> geoDbi, Dbi<ByteBuffer> tripDbi, Dbi<ByteBuffer> routeDbi) throws IOException {
        this.geoDbi = geoDbi;
        this.tripDbi = tripDbi;
        this.routeDbi = routeDbi;
        this.tx = tx;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        tx.close();
    }

    @Override
    public Map<String, Geo> geoMap() {
        if (geoMap == null) {
            geoMap = new LMDBMap<>(this, geoDbi, new StringSerializer(), new Geo.GeoSerializer());
        }
        return geoMap;
    }

    @Override
    public Map<Integer, Trip> tripMap() {
        if (tripMap == null) {
            tripMap = new LMDBMap<>(this, tripDbi, new IntSerializer(), new Trip.TripSerializer());
        }
        return tripMap;
    }

    @Override
    public Map<Integer, Route> routeMap() {
        if (routeMap == null) {
            routeMap = new LMDBMap<>(this, routeDbi, new IntSerializer(), new Route.RouteSerializer());
        }
        return routeMap;
    }

    @Override
    public void commit() {
        tx.commit();
    }
}

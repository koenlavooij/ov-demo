package nl.trivento.fastdata.travelclear.routes.entities.serialization.mapdb;

import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Transaction;
import nl.trivento.fastdata.travelclear.routes.entities.Route;
import nl.trivento.fastdata.travelclear.routes.entities.Trip;
import org.mapdb.DB;
import org.mapdb.HTreeMap;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapDBSession implements Session, Transaction {
    private final HTreeMap<Integer, Route> route;
    private final HTreeMap<Integer, Trip> trip;
    private final HTreeMap<String, Geo> geo;
    private final DB db;

    public MapDBSession(DB db, HTreeMap<Integer, Route> route, HTreeMap<Integer, Trip> trip, HTreeMap<String, Geo> geo) {
        this.db = db;
        this.route = route;
        this.trip = trip;
        this.geo = geo;
    }

    @Override
    public Transaction startTransaction(boolean readwrite) throws IOException {
        return this;
    }

    @Override
    public Map<String, Geo> geoMap() {
        return geo;
    }

    @Override
    public Map<Integer, Trip> tripMap() {
        return trip;
    }

    @Override
    public Map<Integer, Route> routeMap() {
        return route;
    }

    @Override
    public Trip resolveTrip(int id) {
        return trip.get(id);
    }

    @Override
    public Route resolveRoute(int id) {
        return route.get(id);
    }

    @Override
    public Geo resolveGeo(String id) {
        return geo.get(id);
    }

    @Override
    public void commit() {
        db.commit();
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public <T> T readOp(Function<Transaction, T> run) throws IOException {
        return run.apply(this);
    }

    @Override
    public void writeOp(Consumer<Transaction> run) throws IOException {
        run.accept(this);
    }
}

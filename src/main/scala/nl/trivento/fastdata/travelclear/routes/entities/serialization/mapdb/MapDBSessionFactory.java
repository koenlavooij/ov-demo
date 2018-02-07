package nl.trivento.fastdata.travelclear.routes.entities.serialization.mapdb;

import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.SessionFactory;
import nl.trivento.fastdata.travelclear.routes.entities.Route;
import nl.trivento.fastdata.travelclear.routes.entities.Trip;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.File;

public class MapDBSessionFactory implements SessionFactory {
    private final HTreeMap<Integer, Route> route;
    private final HTreeMap<Integer, Trip> trip;
    private final HTreeMap<String, Geo> geo;
    private final DB db;

    public MapDBSessionFactory(File file) {
        db = DBMaker.fileDB(file)
                .fileMmapEnable()
                .fileMmapEnableIfSupported()           // Only enable mmap on supported platforms
                .fileMmapPreclearDisable()             // Make mmap file faster
                .allocateStartSize(128 * 1024 * 1024)  // 128 MB
                .allocateIncrement(32 * 1024 * 1024)   // 32 MB
                .make();
        route = db.hashMap("route", Serializer.INTEGER, new SerializerWrapper<>(new Route.RouteSerializer(), 128)).createOrOpen();
        trip = db.hashMap("trip", Serializer.INTEGER, new SerializerWrapper<>(new Trip.TripSerializer(), 128)).createOrOpen();
        geo = db.hashMap("geo", Serializer.STRING_ASCII, new SerializerWrapper<>(new Geo.GeoSerializer(), 16_000_000)).createOrOpen();
        db.commit();
    }

    @Override
    public Session createSession() {
        return new MapDBSession(db, route, trip, geo);
    }

    @Override
    public void closeSession(Session session) {
        session.close();
    }

    @Override
    public void close() throws Exception {
        db.close();
    }
}

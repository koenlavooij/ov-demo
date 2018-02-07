package nl.trivento.fastdata.travelclear.routes.entities.serialization.lmdb;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.SessionFactory;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lmdbjava.DbiFlags.MDB_CREATE;

public class LMDBSessionFactory implements SessionFactory {
    private final Dbi<ByteBuffer> geoMap;
    private final Dbi<ByteBuffer> tripMap;
    private final Dbi<ByteBuffer> routeMap;
    private Env<ByteBuffer> env;

    public LMDBSessionFactory(File path) throws IOException {
        if (!path.exists()) {
            if (!path.mkdir()) {
                throw new IOException("Could not create db at " + path);
            }
        }

        env = Env.create()
                .setMaxReaders(1)
                .setMapSize(10_000_000_000L)
                .setMaxDbs(3)
                .open(path);

        geoMap = env.openDbi("geo", MDB_CREATE);
        tripMap = env.openDbi("trip", MDB_CREATE);
        routeMap = env.openDbi("route", MDB_CREATE);
    }
    @Override
    public Session createSession() throws IOException {
        return new LMDBSession(env, geoMap, tripMap, routeMap);
    }

    @Override
    public void closeSession(Session session) {
        session.close();
    }

    @Override
    public void close() throws Exception {
        env.close();
    }
}

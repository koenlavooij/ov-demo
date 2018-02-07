package nl.trivento.fastdata.travelclear.routes.entities.serialization.lmdb;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Transaction;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;

import java.io.IOException;
import java.nio.ByteBuffer;

public class LMDBSession implements Session {
    private final Dbi<ByteBuffer> geoMap;
    private final Dbi<ByteBuffer> tripMap;
    private final Dbi<ByteBuffer> routeMap;
    private Env<ByteBuffer> env;

    LMDBSession(Env<ByteBuffer> env, Dbi<ByteBuffer> geoMap, Dbi<ByteBuffer> tripMap, Dbi<ByteBuffer> routeMap) {
        this.env = env;
        this.geoMap = geoMap;
        this.tripMap = tripMap;
        this.routeMap = routeMap;
    }

    @Override
    public void close() {
        //
    }

    public void compact() {
        env.stat();
    }

    @Override
    public Transaction startTransaction(boolean readwrite) throws IOException {
        return new LMDBTransaction(readwrite ? env.txnWrite() : env.txnRead(), geoMap, tripMap, routeMap);
    }
}

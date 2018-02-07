package nl.trivento.fastdata.travelclear.routes.entities.serialization.caching;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Transaction;

import java.io.IOException;

public class CachingSession implements Session {
    private final Session inner;

    public CachingSession(Session inner) {
        this.inner = inner;
    }

    @Override
    public Transaction startTransaction(boolean readwrite) throws IOException {
        return new CachingTransaction(inner.startTransaction(readwrite));
    }

    @Override
    public void close() {
        //TODO flush all transactions?
        inner.close();
    }
}

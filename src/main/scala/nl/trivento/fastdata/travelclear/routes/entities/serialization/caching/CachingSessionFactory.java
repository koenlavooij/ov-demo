package nl.trivento.fastdata.travelclear.routes.entities.serialization.caching;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.SessionFactory;

import java.io.IOException;

public class CachingSessionFactory implements SessionFactory {
    private final SessionFactory inner;

    public CachingSessionFactory(SessionFactory inner) {
        this.inner = inner;
    }

    @Override
    public Session createSession() throws IOException {
        return new CachingSession(inner.createSession());
    }

    @Override
    public void closeSession(Session session) {
        session.close();
        inner.closeSession(session);
    }

    @Override
    public void close() throws Exception {
        inner.close();
    }
}

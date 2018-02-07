package nl.trivento.fastdata.travelclear.routes.entities.serialization;

import java.io.IOException;

public interface SessionFactory extends AutoCloseable {
    Session createSession() throws IOException;
    void closeSession(Session session);
}

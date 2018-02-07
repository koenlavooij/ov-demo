package nl.trivento.fastdata.travelclear.routes.entities.serialization;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Serializer<T> {
    T fromBuffer(ByteBuffer buffer) throws IOException;
    ByteBuffer toBuffer(T in, ByteBuffer buffer) throws IOException;
}

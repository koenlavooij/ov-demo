package nl.trivento.fastdata.travelclear.routes.entities.serialization.lmdb;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class IntSerializer implements Serializer<Integer> {
    @Override
    public Integer fromBuffer(ByteBuffer buffer) throws IOException {
        return buffer.getInt();
    }

    @Override
    public ByteBuffer toBuffer(Integer in, ByteBuffer buffer) throws IOException {
        buffer.putInt(in);
        return buffer;
    }
}

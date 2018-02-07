package nl.trivento.fastdata.travelclear.routes.entities.serialization.lmdb;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class StringSerializer implements Serializer<String> {
    @Override
    public String fromBuffer(ByteBuffer buffer) throws IOException {
        int l = buffer.getShort();
        byte[] data = new byte[l];
        buffer.get(data, 0, l);
        return new String(data);
    }

    @Override
    public ByteBuffer toBuffer(String in, ByteBuffer buffer) {
        byte[] data = in.getBytes();
        buffer.putShort((short) data.length);
        buffer.put(data);
        return buffer;
    }
}

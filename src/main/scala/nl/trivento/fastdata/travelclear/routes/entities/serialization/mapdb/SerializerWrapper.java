package nl.trivento.fastdata.travelclear.routes.entities.serialization.mapdb;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.EntitySerializer;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

import java.io.IOException;

public class SerializerWrapper<T> implements org.mapdb.Serializer<T> {
    private final int initializationSize;

    private final EntitySerializer<T> internal;

    public SerializerWrapper(EntitySerializer<T> internal, int initializationSize) {
        this.internal = internal;
        this.initializationSize = initializationSize;
    }

    @Override
    public void serialize(@NotNull DataOutput2 out, @NotNull T value) throws IOException {
//        ByteBuffer buffer = getBuffer();
//        internal.toBuffer(value, buffer);
//        buffer.flip();
//        out.packInt(buffer.limit());
//        out.write(buffer.array(), 0, buffer.limit());
//        out.flush();
        out.ensureAvail(initializationSize);
        internal.serialize(out, value);
    }

    @Override
    public T deserialize(@NotNull DataInput2 input, int available) throws IOException {
//        byte[] data = new byte[input.unpackInt()];
//        input.readFully(data);
//        return internal.fromBuffer(ByteBuffer.wrap(data));
        return internal.deserialize(input);
    }
}

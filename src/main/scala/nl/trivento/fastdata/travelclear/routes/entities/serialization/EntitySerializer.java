package nl.trivento.fastdata.travelclear.routes.entities.serialization;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public abstract class EntitySerializer<T> implements Serializer<T> {
    private Map<Class, Object[]> enumCache = new HashMap<>();

    private class ByteBufferBackedInputStream extends InputStream {
        protected final ByteBuffer buffer;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            buffer = buf;
        }

        @Override public int available() {
            return buffer.remaining();
        }

        @Override
        public int read() throws IOException {
            return buffer.hasRemaining() ? (buffer.get() & 0xFF) : -1;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (!buffer.hasRemaining()) return -1;
            len = Math.min(len, buffer.remaining());
            buffer.get(bytes, off, len);
            return len;
        }
    }

    private class ByteBufferBackedOutputStream extends OutputStream {
        protected final ByteBuffer buffer;

        public ByteBufferBackedOutputStream(ByteBuffer buf) {
            buffer = buf;
        }

        @Override
        public void write(int b) throws IOException {
            if (buffer.remaining() == 0) {
                throw new IOException("Buffer is full: size=" + buffer.position());
            }
            buffer.put((byte) b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (buffer.remaining() < len) {
                throw new IOException("Buffer is full, trying to write to " + len + " bytes, but only " +
                        buffer.remaining() + " bytes are available");
            }
            buffer.put(b, off, len);
        }
    }

    protected <V> void optionalWrite(DataOutput out, V value, ValueWriter<V> writer) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) {
            writer.write(out, value);
        }
    }

    protected <V> V optionalRead(DataInput in, ValueReader<V> reader) throws IOException {
        if (in.readBoolean()) {
            return reader.read(in);
        } else {
            return null;
        }
    }

    public abstract void serialize(DataOutput out, T value) throws IOException;

    public abstract T deserialize(DataInput input) throws IOException;

    @Override
    public T fromBuffer(ByteBuffer buffer) throws IOException {
        if (buffer == null) {
            return null;
        }
        return deserialize(new DataInputStream(new ByteBufferBackedInputStream(buffer)));
    }

    @Override
    public ByteBuffer toBuffer(T in, ByteBuffer buffer) throws IOException {
        serialize(new DataOutputStream(new ByteBufferBackedOutputStream(buffer)), in);
        return buffer;
    }

    @FunctionalInterface
    protected interface ValueWriter<V> {
        void write(DataOutput out, V value) throws IOException;
    }

    @FunctionalInterface
    protected interface ValueReader<V> {
        V read(DataInput in) throws IOException;
    }

    protected int enumSetToInt(EnumSet<?> set) {
        return set.stream().map(v -> 1 << v.ordinal()).reduce(0, (a, b) -> a | b);
    }

    protected <E extends Enum<E>> EnumSet<E> intToEnumSet(Class<E> cls, int v) {
        Object[] values = enumCache.computeIfAbsent(cls, c -> EnumSet.allOf(c).stream().toArray());

        EnumSet<E> result = EnumSet.noneOf(cls);
        int ordinal = 0;
        while (v > 0) {
            if ((v & 1) > 0) {
                result.add((E) values[ordinal]);
            }
            v = v >> 1;
            ordinal++;
        }

        return result;
    }
}

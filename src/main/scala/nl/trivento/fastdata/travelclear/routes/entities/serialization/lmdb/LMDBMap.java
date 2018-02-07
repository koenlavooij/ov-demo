package nl.trivento.fastdata.travelclear.routes.entities.serialization.lmdb;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.Serializer;
import org.lmdbjava.Dbi;
import org.lmdbjava.Txn;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LMDBMap<K, V> implements Map<K, V> {
    private static final int MAX_KEY_SIZE = 1024;
    private static final int MAX_VALUE_SIZE = 1024;
    private final LMDBTransaction transaction;
    private final Dbi<ByteBuffer> dbi;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;

//    class Buffers {
//        ByteBuffer valueBuffer() {
//            return ByteBuffer.allocateDirect(MAX_VALUE_SIZE);
//        }
//        ByteBuffer keyBuffer() {
//            return ByteBuffer.allocateDirect(MAX_KEY_SIZE);
//        }
//    }

    static class Buffers {
        private ByteBuffer keyBuffer = ByteBuffer.allocateDirect(MAX_KEY_SIZE);
        private ByteBuffer valueBuffer = ByteBuffer.allocateDirect(MAX_VALUE_SIZE);

        ByteBuffer valueBuffer() {
            valueBuffer.clear();
            return valueBuffer;
        }
        ByteBuffer keyBuffer() {
            keyBuffer.clear();
            return keyBuffer;
        }
    }

    private static final Map<Long, Buffers> buffers = new ConcurrentHashMap<>(16, 1, 16);

    private Buffers getBuffers() {
        return buffers.computeIfAbsent(Thread.currentThread().getId(), id -> new Buffers());
    }

    public LMDBMap(LMDBTransaction transaction, Dbi<ByteBuffer> dbi, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        this.transaction = transaction;
        this.dbi = dbi;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    @Override
    public int size() {
        return (int) dbi.stat(transaction.tx).entries;
    }

    @Override
    public boolean isEmpty() {
        return size() > 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) == null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new NotImplementedException();
    }

    private ByteBuffer key(Object key) {
        try {
            ByteBuffer keyBuffer = getBuffers().keyBuffer();
            keyBuffer.clear();
            keySerializer.toBuffer((K) key, keyBuffer);
            keyBuffer.flip();
            return keyBuffer;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ByteBuffer value(V value) {
        try {
            ByteBuffer valueBuffer = getBuffers().valueBuffer();
            valueBuffer.clear();
            valueSerializer.toBuffer(value, valueBuffer);
            valueBuffer.flip();
            return valueBuffer;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public V get(Object key) {
        try {
            return valueSerializer.fromBuffer(dbi.get(transaction.tx, key(key)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public V put(K key, V value) {
        ByteBuffer keyBuffer = key(key);
        //V result = valueSerializer.fromBuffer(dbi.get(transaction.tx, keyBuffer));
        dbi.put(transaction.tx, keyBuffer, value(value));
        return null;
    }

    @Override
    public V remove(Object key) {
        ByteBuffer keyBuffer = key(key);
        //V result = get(keyBuffer);
        dbi.delete(transaction.tx, keyBuffer);
        //return result;
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Buffers buffers = getBuffers();
        ByteBuffer keyBuffer = buffers.keyBuffer();
        ByteBuffer valueBuffer = buffers.valueBuffer();
        Txn<ByteBuffer> tx = transaction.tx;

        try {
            for (Entry<? extends K, ? extends V> e : m.entrySet()) {
                keySerializer.toBuffer(e.getKey(), (ByteBuffer) keyBuffer.clear()).flip();
                valueSerializer.toBuffer(e.getValue(), (ByteBuffer) valueBuffer.clear()).flip();

                dbi.put(tx, keyBuffer, valueBuffer);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void clear() {
        throw new NotImplementedException();
    }

    @Override
    public Set<K> keySet() {
        throw new NotImplementedException();
    }

    @Override
    public Collection<V> values() {
        throw new NotImplementedException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new NotImplementedException();
    }
}

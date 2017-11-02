package nl.trivento.fastdata.travelclear.ndovloket;

import scala.util.control.Exception;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Gzip {
    private final static int FHCRC = 2;    // Header CRC
    private final static int FEXTRA = 4;    // Extra field
    private final static int FNAME = 8;    // File name
    private final static int FCOMMENT = 16;   // File comment
    private final static int EXPECT_SIZE = 65536;

    private static final class InflatedBytes {
        byte[] data;
        int size;

        public InflatedBytes(byte[] data, int size) {
            this.data = data;
            this.size = size;
        }
    }

    private static InflatedBytes inflate(byte[] data) throws DataFormatException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.getShort(); //magic header
        buffer.get(); //should be 8
        int flags = buffer.get();
        int pos = 0;

        //skip 6 bytes
        buffer.position(10);

        if ((flags & FEXTRA) == FEXTRA) {
            buffer.position(buffer.position() + buffer.getShort());
        }
        // Skip optional file name
        if ((flags & FNAME) == FNAME) {
            while (buffer.get() != 0) { }
        }
        // Skip optional file comment
        if ((flags & FCOMMENT) == FCOMMENT) {
            while (buffer.get() != 0) { }
        }
        // Skip Check optional header CRC
        if ((flags & FHCRC) == FHCRC) {
            buffer.getShort();
        }

        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(data, buffer.position(), buffer.remaining());

            byte[] result = new byte[EXPECT_SIZE];
            int size = inflater.inflate(result);
            while (!inflater.finished()) {
                byte[] newResult = new byte[result.length * 2];
                System.arraycopy(result, 0, newResult, 0, result.length);
                size += inflater.inflate(newResult, 0, result.length);
                result = newResult;
            }
            return new InflatedBytes(result, size);
        } finally {
            inflater.end();
        }
    }

    public static String inflateToString(byte[] data) throws DataFormatException {
        InflatedBytes inflated = inflate(data);
        return new String(inflated.data, 0, inflated.size);
    }
}

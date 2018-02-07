package nl.trivento.fastdata.travelclear.routes.entities;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.EntitySerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Trip extends Resolvable<Trip> {
    public static final class TripSerializer extends EntitySerializer<Trip> {
        @Override
        public void serialize(DataOutput out, Trip value) throws IOException {
            out.writeInt(value.getId());
            out.writeInt(value.getRoute().id());
            out.writeUTF(value.getTripHeadsign());
            optionalWrite(out, value.getTripShortName(), DataOutput::writeUTF);
        }

        @Override
        public Trip deserialize(DataInput input) throws IOException {
            return new TripImpl(input.readInt(), input.readInt(), input.readUTF(), optionalRead(input, DataInput::readUTF));
        }
    }

    Integer getId();
    Reference<Integer, Route> getRoute();
    String getTripShortName();
    String getTripHeadsign();
}


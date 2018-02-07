package nl.trivento.fastdata.travelclear.routes.entities;

import nl.trivento.fastdata.travelclear.routes.entities.serialization.EntitySerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Route {
    public static final class RouteSerializer extends EntitySerializer<Route> {
        @Override
        public void serialize(DataOutput out, Route value) throws IOException {
            out.writeInt(value.getId());
            out.writeUTF(value.getShortName());
            optionalWrite(out, value.getLongName(), DataOutput::writeUTF);
            out.writeInt(value.getType());
        }

        @Override
        public Route deserialize(DataInput input) throws IOException {
            return new RouteImpl(input.readInt(), input.readUTF(), optionalRead(input, DataInput::readUTF), input.readInt());
        }
    }

    Integer getId();
    String getShortName();
    String getLongName();
    int getType();
}

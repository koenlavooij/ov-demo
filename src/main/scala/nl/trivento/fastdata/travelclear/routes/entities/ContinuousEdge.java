package nl.trivento.fastdata.travelclear.routes.entities;

import nl.trivento.fastdata.travelclear.routes.TransportMode;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.EntitySerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;

public class ContinuousEdge extends Edge implements Resolvable<ContinuousEdge> {
    public static final class ContinuousEdgeSerializer extends EntitySerializer<ContinuousEdge> {
        @Override
        public void serialize(DataOutput out, ContinuousEdge value) throws IOException {
            out.writeUTF(value.getDestId());
            optionalWrite(out, value.getName(), DataOutput::writeUTF);
            out.writeInt(enumSetToInt(value.getModes()));
            out.writeInt(value.duration);
        }

        @Override
        public ContinuousEdge deserialize(DataInput input) throws IOException {
            String destId = input.readUTF();
            String name = optionalRead(input, DataInput::readUTF);
            return new ContinuousEdge(name, Reference.empty(destId), intToEnumSet(TransportMode.class, input.readInt()),
                    input.readInt());
        }
    }
    private final int duration;

    public ContinuousEdge(String name, Reference<String, Geo> to, EnumSet<TransportMode> modes, int duration) {
        super(name, to, modes);
        this.duration = duration;
    }

    public Optional<Integer> getDuration(int time) {
        return Optional.of(duration);
    }

    @Override
    public Optional<Integer> getArrival(int time) {
        return Optional.of(time + duration);
    }

    @Override
    public boolean isEqualStint(Edge explorable) {
        return false;
    }

    @Override
    public String getResolvedName() {
        return getName();
    }

    @Override
    public String toString() {
        return "Edge{" +
                "name=" + getName() +
                ", to=" + getTo() +
                ", duration=" + timeToString(duration) +
                '}';
    }

    private String timeToString(int arrival) {
        int h = arrival / 3600;
        int m = (arrival - h * 3600) / 60;
        int s = (arrival - h * 3600 - m * 60);
        return String.format("%2d:%2d:%2d", h, m, s);
    }

    @Override
    public ContinuousEdge resolveReferences(Resolver resolver) {
        return new ContinuousEdge(
                getName(),
                resolver.resolveGeo(to),
                getModes(),
                duration
        );
    }

}

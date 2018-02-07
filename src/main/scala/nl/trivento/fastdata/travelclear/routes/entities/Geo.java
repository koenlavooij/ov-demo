package nl.trivento.fastdata.travelclear.routes.entities;

import nl.trivento.fastdata.travelclear.routes.TransportMode;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.EntitySerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Stream;

public interface Geo extends Resolvable<Geo> {
    class GeoSerializer extends EntitySerializer<Geo> {
        private final ScheduleEdge.ScheduleEdgeSerializer scheduledEdgeSerializer;
        private final ContinuousEdge.ContinuousEdgeSerializer continuousEdgeSerializer;

        public GeoSerializer() {
            scheduledEdgeSerializer = new ScheduleEdge.ScheduleEdgeSerializer();
            continuousEdgeSerializer = new ContinuousEdge.ContinuousEdgeSerializer();
        }

        @Override
        public void serialize(DataOutput out, Geo value) throws IOException {
            out.writeUTF(value.getId());
            optionalWrite(out, value.getName(), DataOutput::writeUTF);
            optionalWrite(out, value.getPlatformCode(), DataOutput::writeUTF);
            out.writeDouble(value.getLat());
            out.writeDouble(value.getLon());
            out.writeInt(value.getScheduled().size());
            out.writeInt(value.getContinuous().size());

            for (ScheduleEdge edgeEntry: value.getScheduled()) {
                scheduledEdgeSerializer.serialize(out, edgeEntry);
            }

            for (ContinuousEdge edge: value.getContinuous()) {
                continuousEdgeSerializer.serialize(out, edge);
            }
        }

        @Override
        public Geo deserialize(DataInput input) throws IOException {
            GeoImpl geo = new GeoImpl(input.readUTF(), optionalRead(input, DataInput::readUTF), optionalRead(input, DataInput::readUTF), input.readDouble(), input.readDouble());
            int scheduled = input.readInt();
            int continuous = input.readInt();

//
//            System.out.println("writing " + geo.getId());
//            if (geo.getId().equals("15617")) {
//                System.out.println("yes");
//            }

            for (int c = 0; c < scheduled; c++) {
                geo.getScheduled().add(scheduledEdgeSerializer.deserialize(input));
            }
            for (int c = 0; c < continuous; c++) {
                geo.getContinuous().add(continuousEdgeSerializer.deserialize(input));
            }

            return geo;
        }
    }

    String getId();
    String getName();
    String getDescription();
    double getLat();
    double getLon();
    String getPlatformCode();
    Collection<ScheduleEdge> getScheduled();
    Collection<ContinuousEdge> getContinuous();
    int walkSecondsDistance(Geo other);
    int distance(Geo other);
    Stream<Edge> getEdges(int time);

    void addContinuousEdge(String name, String to, EnumSet<TransportMode> transportModes, int duration);
    void addScheduledEdge(Integer tripId, String to, int price, int departureTime, int arrivalTime);
}

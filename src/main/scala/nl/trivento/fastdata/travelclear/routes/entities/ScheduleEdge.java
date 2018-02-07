package nl.trivento.fastdata.travelclear.routes.entities;

import nl.trivento.fastdata.travelclear.routes.TransportMode;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.EntitySerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

public class ScheduleEdge extends Edge implements Resolvable<ScheduleEdge> {
    public static final class ScheduleEdgeSerializer extends EntitySerializer<ScheduleEdge> {
        @Override
        public void serialize(DataOutput out, ScheduleEdge value) throws IOException {
            out.writeInt(value.getTripId());
            out.writeUTF(value.getDestId());
            out.writeInt(enumSetToInt(value.getModes()));
            out.writeInt(value.price);
            out.writeInt(value.departure);
            out.writeInt(value.arrival);
        }

        @Override
        public ScheduleEdge deserialize(DataInput input) throws IOException {
            return new ScheduleEdge(
                    Reference.empty(input.readInt()),
                    Reference.empty(input.readUTF()),
                    intToEnumSet(TransportMode.class, input.readInt()),
                    input.readInt(),
                    input.readInt(),
                    input.readInt());
        }
    }

    final Reference<Integer, Trip> trip;
    private final int price;
    private final Integer departure;
    private final Integer arrival;

    ScheduleEdge(Reference<Integer, Trip> trip, Reference<String, Geo> to,
                        EnumSet<TransportMode> modes, int price, Integer departure, Integer arrival) {
        super(trip.id().toString(), to, modes);
        this.trip = trip;
        this.price = price;
        this.departure = departure;
        this.arrival = arrival;
    }

    public Integer getTripId() {
        return trip.id();
    }

    public Trip getTrip() {
        return trip.resolve();
    }

    public int getPrice() {
        return price;
    }

    public Optional<Integer> getDuration(int time) {
        return getArrival(time).map(t -> t - time);
    }

    @Override
    public Optional<Integer> getArrival(int time) {
        return time <= departure ? Optional.of(arrival) : Optional.empty();
    }

    @Override
    public boolean isEqualStint(Edge explorable) {
        return ScheduleEdge.class.isAssignableFrom(explorable.getClass()) &&
                ScheduleEdge.class.cast(explorable).getTripId().equals(getTripId());
    }

    @Override
    public String getResolvedName() {
        return getTrip().getTripShortName() + " [" + getTrip().getTripHeadsign() + "] " + getTrip().getRoute().resolve().getShortName();
    }

    @Override
    public String toString() {
        return "ScheduleEdge{name=" + getName() +
                ", trip=" + getTripId() +
                ", price=" + price +
                ", departure=" + departure +
                ", arrival=" + arrival +
                '}';
    }

    private String timeToString(int arrival) {
        int h = arrival / 3600;
        int m = (arrival - h * 3600) / 60;
        int s = (arrival - h * 3600 - m * 60);
        return String.format("%2d:%2d:%2d", h, m, s);
    }

    @Override
    public ScheduleEdge resolveReferences(Resolver resolver) {
        return new ScheduleEdge(
                resolver.resolveTrip(trip),
                resolver.resolveGeo(to),
                getModes(),
                getPrice(),
                arrival,
                departure
        );
    }
}

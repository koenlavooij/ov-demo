package nl.trivento.fastdata.travelclear.routes.entities;

import nl.trivento.fastdata.travelclear.routes.TransportMode;
import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.serialization.mappings.LatLonFieldMappingFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Stream;

@CsvFields(filename = "stops.txt", prefix = "stop_")
public final class GeoImpl extends IdentityBean<String> implements Geo {
    @CsvField
    private String id;

    private String name;

    @CsvField(mapping = LatLonFieldMappingFactory.class)
    private double lat;

    @CsvField(mapping = LatLonFieldMappingFactory.class)
    private double lon;

    @CsvField(name="platform_code", optional = true)
    private String platformCode;

    private final Collection<ScheduleEdge> scheduled = new ArrayList<>();
    private final Collection<ContinuousEdge> continuous = new ArrayList<>();

    public GeoImpl() {

    }

    public GeoImpl(String id, String name, String platformCode, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lng;
        this.platformCode = platformCode;
    }

    public GeoImpl(GeoImpl obj) {
        this.id = obj.id;
        this.name = obj.name;
        this.lat = obj.lat;
        this.lon = obj.lon;
    }

    public Geo resolveReferences(Resolver resolver) {
        Collection<ScheduleEdge> scheduled = new ArrayList<>();
        this.scheduled.forEach(e -> scheduled.add(e.resolveReferences(resolver)));
        this.scheduled.clear();
        this.scheduled.addAll(scheduled);

        Collection<ContinuousEdge> continuous = new ArrayList<>();
        this.continuous.forEach(e -> continuous.add(e.resolveReferences(resolver)));
        this.continuous.clear();
        this.continuous.addAll(continuous);

        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        if (getPlatformCode() == null) {
            return name;
        } else {
            return name + " " + getPlatformCode();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }

    public Collection<ScheduleEdge> getScheduled() {
        return scheduled;
    }

    public Collection<ContinuousEdge> getContinuous() {
        return continuous;
    }

    @Override
    public String toString() {
        return "<GeoImpl " + name  + " " + platformCode + ">";
    }

    public int walkSecondsDistance(Geo other) {
        //sloppy
        double latd = lat - other.getLat();
        double lond = (lon - other.getLon()) * Math.cos(lat);

        //walkspeed
        return (int) (java.lang.Double.longBitsToDouble(
                (
                        (
                                java.lang.Double.doubleToLongBits(latd * latd + lond * lond) - (1L << 52)
                        ) >> 1
                ) +
                        (1L << 61)
        ) * 111000);
        //111000 m per g 5 kmh = 1.39 ms
        //return (int) Math.sqrt(latd * latd + lond * lond) * 79258;
    }

    public int distance(Geo other) {
        //sloppy
        double latd = lat - other.getLat();
        double lond = (lon - other.getLon()) * Math.cos(lat);

        //walkspeed
        return (int) (java.lang.Double.longBitsToDouble(
                (
                        (
                                java.lang.Double.doubleToLongBits(latd * latd + lond * lond) - (1L << 52)
                        ) >> 1
                ) +
                        (1L << 61)
        ) * 110000);
        //Math.sqrt(latd * latd + lond * lond) * 155000
    }

    public Stream<Edge> getEdges(int time) {
        return Stream.concat(
                continuous.stream(),
                scheduled.stream().flatMap(s -> s.getArrival(time).map(a -> Stream.of(s)).orElse(Stream.empty())));
    }

    public synchronized void addContinuousEdge(String name, String to, EnumSet<TransportMode> transportModes, int duration) {
//        GeoImpl result = new GeoImpl(id, this.name, platformCode, lat, lon);
//        result.continuous.addAll(continuous);
//        result.scheduled.putAll(scheduled);
//        result.continuous.add(new ContinuousEdge(name, to, transportModes, duration));
//        return result;
        continuous.add(new ContinuousEdge(name, Reference.empty(to), transportModes, duration));
    }

    public synchronized void addScheduledEdge(Integer tripId, String to, int price,
                                              int departureTime, int arrivalTime) {
//        GeoImpl result = new GeoImpl(id, name, platformCode, lat, lon);
//        result.continuous.addAll(continuous);
//        result.scheduled.putAll(scheduled);
//        result.scheduled.compute(trip.getRoute(), (routeId, schedule) -> {
//            if (schedule == null) {
//                return new ScheduleEdge(route, trip, next, routeTypeToTransportModes(route.getType()), price, departureTime, arrivalTime);
//            } else {
//                schedule.getSchedule().put(departureTime, arrivalTime);
//                return schedule;
//            }
//        });
//        return result;

        scheduled.add(new ScheduleEdge(Reference.empty(tripId), Reference.empty(to), EnumSet.of(TransportMode.Other), price, departureTime, arrivalTime));
    }

    private EnumSet<TransportMode> routeTypeToTransportModes(int type) {
        switch (type) {
            case 0: return EnumSet.of(TransportMode.Tram);
            case 1: return EnumSet.of(TransportMode.Metro);
            case 2: return EnumSet.of(TransportMode.Train);
            case 3: return EnumSet.of(TransportMode.Bus);
            case 4: return EnumSet.of(TransportMode.Boat);
            case 5: return EnumSet.of(TransportMode.CableCar);
            case 6: return EnumSet.of(TransportMode.Gondola);
            case 7: return EnumSet.of(TransportMode.Funicular);
            default: return EnumSet.of(TransportMode.Other);
        }
    }


}

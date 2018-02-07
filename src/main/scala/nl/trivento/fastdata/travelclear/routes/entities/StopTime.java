package nl.trivento.fastdata.travelclear.routes.entities;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.serialization.mappings.StopTimeFieldMappingFactory;

@CsvFields(filename = "stop_times.txt")
public final class StopTime extends IdentityBean<Integer> implements Comparable<StopTime> {

    private static final long serialVersionUID = 1L;

    public static final int MISSING_VALUE = -999;

    @CsvField(ignore = true)
    private int id;

    @CsvField(name = "trip_id")
    private int tripId;

    @CsvField(name = "stop_id")
    private String stopId;

    @CsvField(optional = true, mapping = StopTimeFieldMappingFactory.class)
    private int arrivalTime = MISSING_VALUE;

    @CsvField(optional = true, mapping = StopTimeFieldMappingFactory.class)
    private int departureTime = MISSING_VALUE;

    @CsvField(optional = true)
    private int timepoint = MISSING_VALUE;

    private int stopSequence;

    @CsvField(optional = true)
    private String stopHeadsign;

    @CsvField(optional = true)
    private String routeShortName;

    @CsvField(optional = true)
    private double shapeDistTraveled = MISSING_VALUE;

    public StopTime() {

    }

    public StopTime(StopTime st) {
        this.arrivalTime = st.arrivalTime;
        this.departureTime = st.departureTime;
        this.id = st.id;
        this.routeShortName = st.routeShortName;
        this.shapeDistTraveled = st.shapeDistTraveled;
        this.stopId = st.stopId;
        this.stopHeadsign = st.stopHeadsign;
        this.stopSequence = st.stopSequence;
        this.timepoint = st.timepoint;
        this.tripId = st.tripId;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public int getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(int timepoint) {
        this.timepoint = timepoint;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(int stopSequence) {
        this.stopSequence = stopSequence;
    }

    public String getStopHeadsign() {
        return stopHeadsign;
    }

    public void setStopHeadsign(String stopHeadsign) {
        this.stopHeadsign = stopHeadsign;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public double getShapeDistTraveled() {
        return shapeDistTraveled;
    }

    public void setShapeDistTraveled(double shapeDistTraveled) {
        this.shapeDistTraveled = shapeDistTraveled;
    }

    public int compareTo(StopTime o) {
        return this.getStopSequence() - o.getStopSequence();
    }

    @Override
    public String toString() {
        return "StopTime(seq=" + getStopSequence() + " stop=" + getStopId()
                + " trip=" + getTripId() + " times="
                + StopTimeFieldMappingFactory.getSecondsAsString(getArrivalTime())
                + "-"
                + StopTimeFieldMappingFactory.getSecondsAsString(getDepartureTime())
                + ")";
    }
}

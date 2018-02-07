package nl.trivento.fastdata.travelclear.routes.entities;


import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.model.IdentityBean;

import java.util.function.Function;

@CsvFields(filename = "trips.txt")
public final class TripImpl extends IdentityBean<Integer> implements Trip {
    private static final long serialVersionUID = 1L;

    @CsvField(name = "trip_id")
    private Integer id;

    @CsvField(optional = true)
    private Reference<Integer, Route> route;

    @CsvField(name = "route_id")
    private Integer routeId;

    @CsvField(optional = true)
    private String tripHeadsign;

    @CsvField(optional = true)
    private String tripShortName;

    public TripImpl() {

    }

    public TripImpl(Integer id, Integer route, String tripHeadsign, String tripShortName) {
        this.id = id;
        this.route = Reference.empty(route);
        this.tripHeadsign = tripHeadsign;
        this.tripShortName = tripShortName;
    }

    public TripImpl(Integer id) {
        this.id = id;
    }

    public TripImpl(TripImpl obj) {
        this.id = obj.id;
        this.route = obj.route;
        this.tripHeadsign = obj.tripHeadsign;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Reference<Integer, Route> getRoute() {
        return route;
    }

    public void setRouteId(Integer routeId) {
        routeId = routeId;
        this.route = Reference.empty(routeId);
    }

    public String getTripShortName() {
        return tripShortName;
    }

    public void setTripShortName(String tripShortName) {
        this.tripShortName = tripShortName;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public void setTripHeadsign(String tripHeadsign) {
        this.tripHeadsign = tripHeadsign;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "id=" + id +
                ", route=" + route +
                ", tripShortName='" + tripShortName + '\'' +
                ", tripHeadsign='" + tripHeadsign + '\'' +
                '}';
    }

    @Override
    public Trip resolveReferences(Resolver resolver) {
        route = resolver.resolveRoute(route);
        return this;
    }
}


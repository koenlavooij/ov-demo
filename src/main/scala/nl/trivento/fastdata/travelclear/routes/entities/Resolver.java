package nl.trivento.fastdata.travelclear.routes.entities;

public interface Resolver {
    Reference<Integer, Trip> resolveTrip(Reference<Integer, Trip> toResolve);
    Reference<Integer, Route> resolveRoute(Reference<Integer, Route> toResolve);
    Reference<String, Geo> resolveGeo(Reference<String, Geo> toResolve);
}

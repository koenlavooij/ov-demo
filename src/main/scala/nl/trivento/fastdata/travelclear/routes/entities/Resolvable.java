package nl.trivento.fastdata.travelclear.routes.entities;

import java.util.function.Function;

public interface Resolvable<T extends Resolvable<T>> {
    T resolveReferences(Resolver resolver);
}

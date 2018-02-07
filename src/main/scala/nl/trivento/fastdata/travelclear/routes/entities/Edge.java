package nl.trivento.fastdata.travelclear.routes.entities;

import nl.trivento.fastdata.travelclear.routes.TransportMode;

import javax.swing.text.EditorKit;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Function;

public abstract class Edge {
    final Reference<String, Geo> to;
    private final String name;
    private final EnumSet<TransportMode> modes;

    public Edge(String name, Reference<String, Geo> to, EnumSet<TransportMode> modes) {
        this.name = name;
        this.to = to;
        this.modes = modes;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "to=" + to +
                ", name='" + name + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public Geo getTo() {
        return to.resolve();
    }

    public String getDestId() {
        return to.id();
    }

    public EnumSet<TransportMode> getModes() {
        return modes;
    }

    public abstract Optional<Integer> getDuration(int time);

    public abstract Optional<Integer> getArrival(int time);

    public abstract boolean isEqualStint(Edge explorable);

    public abstract String getResolvedName();
}

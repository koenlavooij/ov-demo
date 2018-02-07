package nl.trivento.fastdata.travelclear.routes.planner;

import nl.trivento.fastdata.travelclear.routes.entities.Edge;

import java.util.*;
import java.util.stream.Collectors;

public class PathEdge {
    private final List<Edge> path;
    private final Edge explorable;
    private final int stints;
    private final int arrival;

    public PathEdge(List<Edge> path, int stints, int arrival, Edge explorable) {
        this.path = path;
        this.stints = stints;
        this.explorable = explorable;
        this.arrival = arrival;
    }

    public Integer getArrival() {
        return arrival;
    }

    public int getStints() {
        return stints;
    }

    public List<Edge> getPath() {
        return path;
    }

    public Edge getExplorable() {
        return explorable;
    }

    public static PathEdge empty() {
        return new PathEdge(Collections.emptyList(), 0, -1, null);
    }

    public String toString(int startTime) {
        if (path.isEmpty()) {
            return ": No Route";
        }

        int tm = startTime;
        int newTm = startTime;
        StringBuffer buffer = new StringBuffer();
        for (int e = 0; e < path.size(); e++) {
            newTm = path.get(e).getArrival(newTm).get();
            Edge start = path.get(e);
            while (e + 1 < path.size()) {
                if (start.isEqualStint(path.get(e + 1))) {
                    e++;
                    newTm = path.get(e + 1).getArrival(newTm).get();
                } else {
                    break;
                }
            }
            Edge end = path.get(e);
            buffer
                    .append(timeToString(tm)).append(' ')
                    .append(start.getResolvedName())
                    .append(" to ")
                    .append(end.getTo().getDescription())
                    .append('\n');

            tm = newTm;
        }

        return buffer.toString();
    }

    private String timeToString(int tm) {
        return new StringBuffer()
                .append((tm / 3600) % 24)
                .append(':')
                .append((tm / 60) % 60)
                .append(':')
                .append(tm % 60)
                .toString();
    }
}

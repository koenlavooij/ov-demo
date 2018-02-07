package nl.trivento.fastdata.travelclear.routes.planner;

import nl.trivento.fastdata.travelclear.gtfs.GtfsTime;
import nl.trivento.fastdata.travelclear.routes.TransportMode;
import nl.trivento.fastdata.travelclear.routes.entities.Edge;
import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Planner {
    private static final Logger logger = LoggerFactory.getLogger(Planner.class);
    private static final EnumSet<TransportMode> WALK_MODES = EnumSet.of(TransportMode.Walk);

    private final Geo start;
    private final Geo end;
    private final int startTime;

    public Planner(Geo start, Geo end, int startTime) {
        this.start = start;
        this.end = end;
        this.startTime = startTime;
    }

    static class Work {
        private final Set<String> visited;
        private final Geo geo;
        private final List<Edge> path;
        private final int stints;
        private final int arrival;

        Work(Geo geo, List<Edge> path, Edge newEdge, int arrival) {
            this.geo = geo;
            this.path = new ArrayList<>(path);
            this.path.add(newEdge);
            this.visited = path.stream().map(a -> a.getTo().getId()).collect(Collectors.toSet());
            this.stints = countStints(this.path);
            this.arrival = arrival;
        }

        Work(Geo geo, int arrival) {
            this.geo = geo;
            this.path = Collections.emptyList();
            this.visited = Collections.singleton(geo.getId());
            this.arrival = arrival;
            this.stints = 0;
        }

        private int countStints(List<Edge> path) {
            int stints = 1;
            for (int i = 0; i < path.size() - 1; i++) {
                Edge leg1 = path.get(i);
                Edge leg2 = path.get(i + 1);
                if (!leg1.isEqualStint(leg2) && !isWalk(leg1.getModes())) {
                    stints++;
                }
            }
            return stints;
        }

        public Geo getGeo() {
            return geo;
        }

        public List<Edge> getPath() {
            return path;
        }

        public Optional<Edge> tailEdge() {
            if (path.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(path.get(path.size() - 1));
            }
        }

        public int getStints() {
            return stints;
        }

        public int getArrival() {
            return arrival;
        }


        public String toString(int startTime) {
            if (path.isEmpty()) {
                return ": No Route";
            }
            StringBuilder buffer = new StringBuilder();

            buffer.append("Stints: ").append(stints).append('\n');

            int departure = startTime;
            for (Edge edge: path) {
                int arrival = edge.getArrival(departure).orElse(departure);
                buffer.append(GtfsTime.format(departure))
                    .append('-')
                    .append(GtfsTime.format(arrival))
                    .append(": ")
                    .append(edge.getResolvedName())
                    .append(" to ")
                    .append(edge.getTo().getName())
                    .append("\n");
                departure = arrival;
            }

            /*
            int tm = startTime;
            int newTm = startTime;
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
                        .append(GtfsTime.format(tm)).append('-')
                        .append(GtfsTime.format(newTm)).append(' ')
                        .append(start.getResolvedName())
                        .append(" to ")
                        .append(end.getTo().getDescription())
                        .append('\n');

                tm = newTm;
            }
*/

            return buffer.toString();
        }

        public boolean hasVisited(Geo to) {
            return visited.contains(to.getId());
        }
    }

    public String plan() {
        PriorityQueue<Work> todo = new PriorityQueue<>(Comparator.<Work>comparingInt(
                e ->
                        e.getStints() * 900 +
                        e.getArrival() +
                        e.getGeo().walkSecondsDistance(end)));

        logger.info("Planning " + GtfsTime.format(startTime));
        todo.add(new Work(start, startTime));

        while (!todo.isEmpty()) {
            Work work = todo.remove();
            //logger.info(">> " + work.getGeo());
//            if (!shouldVisit(visited, work.getGeo(), Optional.of(work.getArrival()))) {
//                continue;
//            }
            //visited.put(work.getGeo().getId(), work.getArrival());

            if (work.getGeo().getId().equals(end.getId())) {
                System.out.println(work.toString(startTime));
                //return work.toString(startTime);
            }

            //
            //System.out.println(work.toString(startTime));
            work.getGeo().getEdges(work.arrival)
                    //already visited?
                    //.filter(edge -> shouldVisit(visited, edge.getTo(), edge.getArrival(work.arrival)))
                    .filter(edge -> !work.hasVisited(edge.getTo()))
                    //two walks in seq?
                    .filter(edge -> work.tailEdge().map(te -> !(isWalk(te.getModes()) && isWalk(edge.getModes()))).orElse(true))
                    .forEach(edge -> edge.getArrival(work.arrival).ifPresent(arrival ->
                            todo.add(new Work(edge.getTo(), work.getPath(), edge, arrival))));
        }

        return null;
    }

    private boolean shouldVisit(Map<String, Integer> visited, Geo to, Optional<Integer> arrival) {
        Integer current = visited.get(to.getId());
        return (current == null) || arrival.map(a -> a <= current).orElse(false);
    }
//
//            boolean isWalk = isWalk(nextEdge.getExplorable().getModes());
//
//            //logger.info(nextEdge.getExplorable().toString());
//            List<Edge> extendedPath = new ArrayList<>(nextEdge.getPath());
//            extendedPath.add(nextEdge.getExplorable());
//
//            //is there a faster route to this
//            PathEdge currentBest = best.get(nextEdge.getExplorable().getDestId());
//            if (currentBest == null || nextEdge.getArrival() <= currentBest.getArrival()) {
//                best.put(nextEdge.getExplorable().getDestId(), nextEdge);
//                //int extendedWeight = nextEdge.getExplorable().getArrival(nextEdge.getWeight()); //TODO extract weight
//
//                if (nextEdge.getExplorable().getTo().equals(end)) {
//                    //Goal reached
//                    logger.info("plan: " + new PathEdge(
//                            extendedPath,
//                            nextEdge.getStints(),
//                            nextEdge.getArrival(),
//                            null
//                    ).toString(startTime));
//                    //return extendedPath;
//                } else {
//                    nextEdge.getExplorable().getArrival(nextEdge.getArrival()).ifPresent(otherGeoArrivalTime ->
//                        nextEdge.getExplorable().getTo()
//                            .getEdges(otherGeoArrivalTime)
//                            .filter(e -> !(isWalk(e.getModes()) && isWalk))
//                            .forEach(e ->
//                                e.getArrival(otherGeoArrivalTime).ifPresent(arrival ->
//                                    todo.add(
//                                        new PathEdge(
//                                            extendedPath,
//                                            nextEdge.getStints() + (e.isEqualStint(nextEdge.getExplorable()) ? 0 : 1),
//                                            arrival,
//                                            e
//                                        )
//                                    )
//                                )
//                            )
//                    );
//                }
//            }
//        }
//        return null;
//    }

    private static boolean isWalk(EnumSet<TransportMode> modes) {
        return WALK_MODES.equals(modes);
    }
}

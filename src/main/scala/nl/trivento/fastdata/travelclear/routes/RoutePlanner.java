package nl.trivento.fastdata.travelclear.routes;

import nl.trivento.fastdata.travelclear.gtfs.GtfsTime;
import nl.trivento.fastdata.travelclear.routes.entities.Edge;
import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.SessionFactory;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.mapdb.MapDBSessionFactory;
import nl.trivento.fastdata.travelclear.routes.planner.Planner;
import org.onebusaway.gtfs.services.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class RoutePlanner {
    private static final Logger logger = LoggerFactory.getLogger(RoutePlanner.class);
    private final GenericDao dao;

    public static void main(String[] args) throws Exception {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        RoutePlanner planner = new RoutePlanner();
        while (true) {
            try {
//                System.out.println("From");
//                //366259
//                String from = console.readLine();
//                //521121 stoparea:165460
//                System.out.println("To");
//                String to = console.readLine();
                String from = "417397";
                String to = "stoparea:165460";
                console.readLine();
                planner.plan(from, to);
            } catch (Exception e) {
                logger.warn("Failed to get plan", e);
            }
        }
    }

    public RoutePlanner() throws Exception {
        try (SessionFactory sessionFactory = new MapDBSessionFactory(RouteDbMaker.file)) {
            try (Session session = sessionFactory.createSession()) {
                try (GraphDao dbDao = new GraphDao(session)) {
                    dao = new CachingGraphDao(dbDao);
                }
            }
        }
    }

    private void plan(String startNode, String endNode) throws IOException {
       String plan = new Planner(
                dao.getEntityForId(Geo.class, startNode),
                dao.getEntityForId(Geo.class, endNode),
               GtfsTime.now()).plan();
        if (plan == null) {
            logger.warn("No route");
        } else {
            System.out.println(plan);
        }
    }
}

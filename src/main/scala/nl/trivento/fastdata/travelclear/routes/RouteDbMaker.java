package nl.trivento.fastdata.travelclear.routes;

import nl.trivento.fastdata.travelclear.routes.entities.Edge;
import nl.trivento.fastdata.travelclear.routes.entities.Geo;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.Session;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.SessionFactory;
import nl.trivento.fastdata.travelclear.routes.entities.serialization.mapdb.MapDBSessionFactory;
import nl.trivento.fastdata.travelclear.routes.gtfs.GtfsImporter;
import nl.trivento.fastdata.travelclear.routes.planner.Planner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class RouteDbMaker {
    private static final Logger logger = LoggerFactory.getLogger(RouteDbMaker.class);
    public static final File file = new File("graph.mapdb");

    public static void main(String[] args) throws IOException {
        new RouteDbMaker();
    }

    public RouteDbMaker() throws IOException {
        testForDb();
    }

    private void testForDb() {
        if (!file.exists() || !canOpen()) {
            if (file.exists()) {
                if (!file.delete()) {
                    logger.error("Could not remove corrupted db " + file);
                }
            }
            recreateDb();
        }
    }

    private boolean canOpen() {
        try (SessionFactory sessionFactory = createSessionFactory()) {
            return true;
        } catch (Exception e) {
            logger.error("Cannot open DB", e);
            return false;
        }
    }

    private SessionFactory createSessionFactory() {
        return new MapDBSessionFactory(file);
    }

    private void recreateDb() {
        try (SessionFactory sessionFactory = createSessionFactory()) {
            try (Session session = sessionFactory.createSession()) {
                try (MutableGraphDao dao = new MutableGraphDao(session)) {
                    //new OsmImporter().read(new File("/Users/koen/Documents/travelclear/netherlands-latest.osm.pbf"), dao);
                    new GtfsImporter().read(new File("/Users/koen/Documents/travelclear/gtfs-nl.zip"), dao);
                }
            }
        } catch (Exception e) {
            logger.error("Error with db resource", e);
        }
    }
}

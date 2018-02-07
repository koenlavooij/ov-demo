package nl.trivento.fastdata.travelclear.routes.gtfs;

import nl.trivento.fastdata.travelclear.routes.MutableGraphDao;
import nl.trivento.fastdata.travelclear.routes.entities.GeoImpl;
import nl.trivento.fastdata.travelclear.routes.entities.RouteImpl;
import nl.trivento.fastdata.travelclear.routes.entities.StopTime;
import nl.trivento.fastdata.travelclear.routes.entities.TripImpl;
import org.onebusaway.csv_entities.ZipFileCsvInputSource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipFile;

public class GtfsImporter {
    public void read(File file, MutableGraphDao dao) throws IOException {
        org.onebusaway.gtfs.serialization.GtfsReader reader = new org.onebusaway.gtfs.serialization.GtfsReader();
        reader.setEntityClasses(Arrays.asList(
                RouteImpl.class,
                TripImpl.class,
                GeoImpl.class,
                StopTime.class));

        reader.setEntityStore(dao);
        reader.setInputSource(new ZipFileCsvInputSource(new ZipFile(file)));
        reader.run();
    }
}

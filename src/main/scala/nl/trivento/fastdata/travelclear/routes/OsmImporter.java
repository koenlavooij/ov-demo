package nl.trivento.fastdata.travelclear.routes;

import crosby.binary.osmosis.OsmosisReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class OsmImporter {
    public void read(File file , MutableGraphDao dao) throws FileNotFoundException {
        OsmosisReader osmosisReader = new OsmosisReader(new FileInputStream(file));
        osmosisReader.setSink(dao);
        osmosisReader.run();
    }
}

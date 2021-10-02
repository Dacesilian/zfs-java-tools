package cz.cesal.zfs.tools;

import cz.cesal.zfs.util.SnapshotSynchronizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class App {

    private static Logger LOGGER = LogManager.getLogger(App.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        SnapshotSynchronizer s = new SnapshotSynchronizer();
        s.loadDatasetsDetails();

        LOGGER.debug("Done");
        System.exit(0);
    }

}

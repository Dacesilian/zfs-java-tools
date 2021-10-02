package cz.cesal.zfs.util;

import cz.cesal.zfs.dto.ZFSPropertySource;
import cz.cesal.zfs.Dataset;
import cz.cesal.zfs.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class SnapshotSynchronizer {

    private static Logger LOGGER = LogManager.getLogger(SnapshotSynchronizer.class);

    public SnapshotSynchronizer() {
    }

    public void loadDatasetsDetails() throws IOException, InterruptedException {
        LOGGER.info("Loading datasets details");

        LOGGER.debug("Listing all datasets");
        List<Dataset> datasets = ZFSUtil.listDatasets();

        for (Dataset ds : datasets) {
            LOGGER.debug("Querying properties for " + ds.getName());
            List<Property> properties = ZFSUtil.getProperties(ds, ZFSPropertySource.INHERITED, ZFSPropertySource.LOCAL, ZFSPropertySource.NONE);
            ds.setProperties(properties);

            for (Property p : properties) {
                LOGGER.debug(p);
            }

        }
    }

}

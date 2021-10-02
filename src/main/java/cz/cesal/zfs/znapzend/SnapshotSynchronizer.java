package cz.cesal.zfs.znapzend;

import cz.cesal.zfs.dto.ZFSProperty;
import cz.cesal.zfs.dto.ZFSPropertySource;
import cz.cesal.zfs.Dataset;
import cz.cesal.zfs.Property;
import cz.cesal.zfs.util.ZFSUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

public class SnapshotSynchronizer {

    private static Logger LOGGER = LogManager.getLogger(SnapshotSynchronizer.class);

    public SnapshotSynchronizer() {
    }

    public List<Dataset> loadDatasetsWithZnapzend() throws IOException, InterruptedException {
        LOGGER.info("Loading datasets details");

        // --- Get all datasets
        LOGGER.debug("Listing all datasets");
        List<Dataset> datasets = ZFSUtil.listDatasets();

        // --- Load properties for each dataset
        Iterator<Dataset> it = datasets.iterator();
        while (it.hasNext()) {
            Dataset ds = it.next();

            LOGGER.debug("Querying properties for " + ds.getName());
            List<Property> properties = ZFSUtil.getProperties(ds, ZFSPropertySource.INHERITED, ZFSPropertySource.LOCAL, ZFSPropertySource.NONE);
            ds.setProperties(properties);

            boolean foundZnapzendProp = false;
            for (Property p : properties) {
                if (p.getProperty().equalsIgnoreCase("org.znapzend:enabled") && p.getValue().equalsIgnoreCase("on")) {
                    foundZnapzendProp = true;
                    break;
                }
            }

            if (!foundZnapzendProp) {
                LOGGER.debug("No znapzend enabled property, not including dataset " + ds.getName());
                it.remove();
            }
        }

        return datasets;
    }

    public List<DatasetBackup> getBackupedDatasets(List<Dataset> datasets) {
        List<DatasetBackup> backups = new ArrayList<>();

        LOGGER.debug("Loaded " + datasets.size() + " datasets with Znapzend properties");
        for (Dataset dataset : datasets) {
            LOGGER.debug("Dataset name = " + dataset.getName());

            DatasetBackup bkp = new DatasetBackup();
            bkp.setName(dataset.getName());
            bkp.setProperties(dataset.getProperties());

            for (Property prop : dataset.getProperties()) {
                if (!prop.getProperty().startsWith("org.znapzend")) {
                    continue;
                }
                LOGGER.trace("--- " + prop.getProperty() + " (" + prop.getSource().name() + (prop.getSource() == ZFSPropertySource.INHERITED ? " from " + prop.getSourceFrom() : "") + ")  -->>  " + prop.getValue());

                if (prop.getProperty().matches("org\\.znapzend:dst_[a-z]+")) {
                    String backupName = prop.getProperty().replace("org.znapzend:dst_", "");
                    String destination = prop.getValue();
                    if (prop.getSource() == ZFSPropertySource.INHERITED) {
                        String dsPostfix = dataset.getName().replace(prop.getSourceFrom(), "");
                        destination += dsPostfix;
                    }
                    LOGGER.trace("--- destination " + backupName + ": " + destination);

                    BackupDestination bkpdest = new BackupDestination();

                    Matcher mx = ZFSUtil.SSH_DATASET_PATTERN.matcher(destination);
                    if (mx.matches()) {
                        bkpdest.setDataset(mx.group(3));
                        bkpdest.setRemoteHost(mx.group(2));
                        bkpdest.setRemoteUser(mx.group(1));
                        bkpdest.setType(DestinationType.REMOTE);
                    } else {
                        bkpdest.setDataset(destination);
                        bkpdest.setType(DestinationType.LOCAL);
                    }

                    bkp.getDestinations().add(bkpdest);
                }
            }

            backups.add(bkp);
        }

        return backups;
    }

}

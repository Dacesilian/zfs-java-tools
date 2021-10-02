package cz.cesal.zfs.tools;

import cz.cesal.zfs.Dataset;
import cz.cesal.zfs.dto.ZFSPropertyValue;
import cz.cesal.zfs.util.ZFSUtil;
import cz.cesal.zfs.znapzend.DatasetBackup;
import cz.cesal.zfs.znapzend.SnapshotSynchronizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class App {

    private static Logger LOGGER = LogManager.getLogger(App.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        SnapshotSynchronizer s = new SnapshotSynchronizer();
        List<Dataset> datasets = s.loadDatasetsWithZnapzend();
        List<DatasetBackup> backupedDatasets = s.getBackupedDatasets(datasets);

        for (DatasetBackup ds : backupedDatasets) {
            LOGGER.debug("--- Dataset: " + ds.getName());

            Map<Integer, List<ZFSPropertyValue>> dsSnapshots = ZFSUtil.listSnapshots(ds.getName());
            for (Map.Entry<Integer, List<ZFSPropertyValue>> entry : dsSnapshots.entrySet()) {
                System.out.println("----- " + entry.getKey() + ":");
                for (ZFSPropertyValue val : entry.getValue()) {
                    System.out.println(val);
                }
            }

            for (String destination : ds.getDestinations()) {
                LOGGER.debug("    destination: " + destination);

                Map<Integer, List<ZFSPropertyValue>> destSnapshots = ZFSUtil.listSnapshots(destination);
                for (Map.Entry<Integer, List<ZFSPropertyValue>> entry : destSnapshots.entrySet()) {
                    System.out.println("----- " + entry.getKey() + ":");
                    for (ZFSPropertyValue val : entry.getValue()) {
                        System.out.println(val);
                    }
                }

            }
        }

        LOGGER.debug("Done");
        System.exit(0);
    }

}

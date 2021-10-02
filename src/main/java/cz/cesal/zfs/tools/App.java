package cz.cesal.zfs.tools;

import cz.cesal.zfs.Dataset;
import cz.cesal.zfs.dto.ZFSProperty;
import cz.cesal.zfs.dto.ZFSPropertyValue;
import cz.cesal.zfs.util.ZFSUtil;
import cz.cesal.zfs.znapzend.BackupDestination;
import cz.cesal.zfs.znapzend.DatasetBackup;
import cz.cesal.zfs.znapzend.DestinationType;
import cz.cesal.zfs.znapzend.SnapshotSynchronizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class App {

    private static Logger LOGGER = LogManager.getLogger(App.class);
    private static Logger RESULT_COMMANDS_LOGGER = LogManager.getLogger("resultCommands");

    public static void main(String[] args) throws IOException, InterruptedException {

        SnapshotSynchronizer s = new SnapshotSynchronizer();
        List<Dataset> datasets = s.loadDatasetsWithZnapzend();
        List<DatasetBackup> backupedDatasets = s.getBackupedDatasets(datasets);

        for (DatasetBackup ds : backupedDatasets) {
            LOGGER.debug("--- Dataset: " + ds.getName());

            // --- Get latest snapshot for dataset
            Map<Integer, List<ZFSPropertyValue>> dsSnapshots = ZFSUtil.listSnapshots(ds.getName());
            List<String> datasetLatestSnapshotParts = ZFSUtil.getLatestSnapshotParts(dsSnapshots);
            if (datasetLatestSnapshotParts == null || datasetLatestSnapshotParts.isEmpty()) {
                LOGGER.debug("    No latest snapshot for " + ds.getName());
                continue;
            }
            LOGGER.debug("    Latest snapshot: " + datasetLatestSnapshotParts);

            // --- Get latest snapshot for each backup destination
            for (BackupDestination destination : ds.getDestinations()) {
                LOGGER.debug("    destination: " + destination);

                Map<Integer, List<ZFSPropertyValue>> destSnapshots = ZFSUtil.listSnapshots(destination);
                List<String> destinationLatestSnapshotParts = ZFSUtil.getLatestSnapshotParts(destSnapshots);
                if (destinationLatestSnapshotParts == null || destinationLatestSnapshotParts.isEmpty()) {
                    LOGGER.debug("       No latest snapshot for " + destination);
                    continue;
                }
                LOGGER.debug("       Latest snapshot: " + destinationLatestSnapshotParts);

                if (destinationLatestSnapshotParts.get(1).equals(datasetLatestSnapshotParts.get(1))) {
                    LOGGER.debug("       Remote snapshot is the same as local, skipping");
                } else {
                    if (destination.getType() == DestinationType.REMOTE) {
                        String resultCmd = "zfs send -c -I " + datasetLatestSnapshotParts.get(0) + "@" + destinationLatestSnapshotParts.get(1) + " " + datasetLatestSnapshotParts.get(0) + "@" + datasetLatestSnapshotParts.get(1) + " | mbuffer -q -s 128M -W 600 -m 4G -o - | pv | ssh -c aes128-gcm@openssh.com -o batchMode=yes -o ConnectTimeout=300 '" + destination.getRemoteUser() + "@" + destination.getRemoteHost() + "' '/usr/bin/mbuffer -q -s 128M -W 600 -m 4G | zfs recv -F " + destination.getDataset() + "'";
                        RESULT_COMMANDS_LOGGER.info(resultCmd);
                    }
                }
            }
        }

        LOGGER.debug("Done");
        System.exit(0);
    }

}

package cz.cesal.zfs.util;

import cz.cesal.util.CommandExecutor;
import cz.cesal.util.CommandResult;
import cz.cesal.zfs.Dataset;
import cz.cesal.zfs.Property;
import cz.cesal.zfs.dto.ZFSCommandResult;
import cz.cesal.zfs.dto.ZFSProperty;
import cz.cesal.zfs.dto.ZFSPropertySource;
import cz.cesal.zfs.dto.ZFSPropertyValue;
import cz.cesal.zfs.znapzend.BackupDestination;
import cz.cesal.zfs.znapzend.DestinationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZFSUtil {

    private static Logger LOGGER = LogManager.getLogger(ZFSUtil.class);

    private static final Pattern ZFS_GET_PATTERN = Pattern.compile("^\\s*(.+?)\\s+(.+?)\\s+(?:(?:inherited from ([^ ]+))|(?:((?:-)|(?:.+))))\\s+(.+)$", Pattern.DOTALL);
    public static final Pattern SSH_DATASET_PATTERN = Pattern.compile("([^@]+)@([^:]+):?(.+)?");

    private ZFSUtil() {
    }

    public static List<Dataset> listDatasets() throws IOException, InterruptedException {
        ZFSCommandResult res = executeCommand("zfs list -t filesystem,volume -r", ZFSProperty.name);
        if (res.getCommandResult().getExitCode() != 0) {
            throw new IOException("Bad exit code " + res.getCommandResult().getExitCode());
        }
        List<Dataset> out = new ArrayList<>();
        for (ZFSPropertyValue v : res.getAllValuesMerged()) {
            Dataset ds = new Dataset();
            ds.setName(v.getMatchedValues().get(0).trim());
            out.add(ds);
        }
        LOGGER.debug("listDatasets - returning " + out.size() + " datasets");
        return out;
    }


    public static List<Property> getProperties(Dataset dataset, ZFSPropertySource... onlySources) throws IOException, InterruptedException {
        return getProperties(dataset.getName(), onlySources);
    }

    public static List<Property> getProperties(String datasetName, ZFSPropertySource... onlySources) throws IOException, InterruptedException {
        List<String> srcList = new ArrayList<>();
        if (onlySources != null) {
            for (ZFSPropertySource s : onlySources) {
                srcList.add(s.name().toLowerCase());
            }
        }
        String cmd = "zfs get " + (srcList.isEmpty() ? "" : "-s " + String.join(",", srcList)) + " all -H -o name,property,source,value " + datasetName;
        ZFSCommandResult res = executeCommand(cmd);
        if (res.getCommandResult().getExitCode() != 0) {
            throw new IOException("Bad exit code " + res.getCommandResult().getExitCode());
        }
        List<Property> out = new ArrayList<>();
        List<String> lines = res.getCommandResult().getOutputLines();
        LOGGER.debug("getProperties - got " + lines.size() + " lines");
        for (String line : lines) {
            if (!line.startsWith("NAME")) {
                Property prop = new Property();
                prop.setLine(line);

                Matcher m = ZFS_GET_PATTERN.matcher(line);
                if (!m.matches()) {
                    throw new IOException("Line not matches: " + line);
                }

                // LOGGER.debug("Parsing property line: " + line + "  src: " + m.group(4));

                prop.setDatasetName(m.group(1));
                prop.setProperty(m.group(2));

                if (m.group(3) != null && !m.group(3).isEmpty()) {
                    prop.setSource(ZFSPropertySource.INHERITED);
                    prop.setSourceFrom(m.group(3));

                } else if (m.group(4) != null && !m.group(4).isEmpty()) {
                    String src = m.group(4);
                    if (src.equalsIgnoreCase("-")) {
                        prop.setSource(ZFSPropertySource.NONE);
                    } else {
                        prop.setSource(ZFSPropertySource.valueOf(src.toUpperCase()));
                    }
                }

                prop.setValue(m.group(5));

                out.add(prop);
            }
        }
        LOGGER.debug("getProperties - returning " + out.size() + " properties");
        return out;
    }

    public static Map<Integer, List<ZFSPropertyValue>> listSnapshots(Dataset dataset) throws IOException, InterruptedException {
        return listSnapshots(dataset.getName());
    }

    public static Map<Integer, List<ZFSPropertyValue>> listSnapshots(BackupDestination destination) throws IOException, InterruptedException {
        if (destination.getType() == DestinationType.REMOTE) {
            return listSnapshots(destination.getRemoteUser() + "@" + destination.getRemoteHost() + ":" + destination.getDataset());
        }
        return listSnapshots(destination.getDataset());
    }

    public static Map<Integer, List<ZFSPropertyValue>> listSnapshots(String datasetName) throws IOException, InterruptedException {
        ZFSCommandResult res = null;

        Matcher mx = SSH_DATASET_PATTERN.matcher(datasetName);
        ZFSProperty[] onlyProperties = {
                ZFSProperty.datasetName, ZFSProperty.creation
        };
        if (mx.matches()) {
            LOGGER.debug("listSnapshots - will use SSH to " + mx.group(2) + ", user " + mx.group(1) + ", dataset " + mx.group(3));
            res = executeCommand(mx.group(1), mx.group(2), "zfs list -t snap " + mx.group(3), onlyProperties);
        } else {
            LOGGER.debug("listSnapshots - local dataset " + datasetName);
            res = executeCommand("zfs list -t snap " + datasetName, onlyProperties);
        }
        if (res.getCommandResult().getExitCode() != 0) {
            throw new IOException("Bad exit code " + res.getCommandResult().getExitCode());
        }
        return res.getValues();
    }

    public static String getLatestSnapshotName(Map<Integer, List<ZFSPropertyValue>> snapshotsData) throws IOException {
        List<String> parts = getLatestSnapshotParts(snapshotsData);
        String lastSnapshot = parts.get(0) + "@" + parts.get(1);
        if (lastSnapshot.isBlank()) {
            throw new IOException("Snapshot name is empty");
        }
        return lastSnapshot;
    }

    public static List<String> getLatestSnapshotParts(Map<Integer, List<ZFSPropertyValue>> snapshotsData) throws IOException {
        // TODO: Sort by creation date

        List<String> matchedValues = null;
        List<ZFSPropertyValue> lastProps = snapshotsData.get(snapshotsData.entrySet().size() - 1);
        if (lastProps == null) {
            return new ArrayList<>();
        }
        for (ZFSPropertyValue val : lastProps) {
            if ((val.getProperty() == ZFSProperty.datasetName || val.getProperty() == ZFSProperty.name) && val.getMatchedValues().size() == 2) {
                matchedValues = val.getMatchedValues();
            }
        }
        if (matchedValues == null) {
            throw new IOException("Snapshot matches not found");
        }
        return matchedValues;
    }

    public static ZFSCommandResult executeCommand(String command, ZFSProperty... onlyProperties) throws IOException, InterruptedException {
        return executeCommand(null, null, command, onlyProperties);
    }

    public static ZFSCommandResult executeCommand(String sshUser, String sshHost, String command, ZFSProperty... onlyProperties) throws IOException, InterruptedException {
        if (onlyProperties != null && onlyProperties.length >= 1) {
            if (command.contains("-H") || command.contains("-o")) {
                throw new IOException("Cannot have -H/-o specified when specific properties given");
            }
            List<String> propsList = new ArrayList<>();
            for (ZFSProperty p : onlyProperties) {
                propsList.add(p.getPropertyName());
            }
            command += " -H -o " + String.join(",", propsList);
        }

        if (sshUser != null && sshHost != null && !sshHost.isEmpty()) {
            LOGGER.debug("Will execute command via SSH on " + sshHost + " user " + sshUser);
            command = String.format("ssh -o batchMode=yes -o ConnectTimeout=300 '%s' '%s'", sshUser + "@" + sshHost, command);
        }

        CommandResult res = CommandExecutor.executeCommand(command);

        ZFSCommandResult cmdRes = new ZFSCommandResult();
        cmdRes.setCommandResult(res);

        if (onlyProperties != null && onlyProperties.length >= 1) {
            int lineNum = 0;
            List<String> lines = res.getOutputLines();
            if (lines != null && !lines.isEmpty()) {
                LOGGER.debug("Processing " + lines.size() + " lines");

                List<String> regexs = new ArrayList<>();
                for (ZFSProperty p : onlyProperties) {
                    regexs.add(p.getPropertyType().getPattern().pattern());
                }
                String patternStr = String.join("\\s+", regexs);
                Pattern pattern = Pattern.compile(patternStr + "\\s*");
                LOGGER.debug("Created pattern: " + pattern.pattern());

                for (String line : lines) {
                    lineNum++;
                    Matcher m = pattern.matcher(line.trim());
                    if (!m.matches()) {
                        throw new IOException("Not matching line: " + line);
                    }

                    List<ZFSPropertyValue> props = new ArrayList<>();
                    LOGGER.trace("Matched line has " + m.groupCount() + " groups");
                    int groupNum = 0;
                    for (ZFSProperty p : onlyProperties) {

                        ZFSPropertyValue propertyValue = new ZFSPropertyValue();
                        propertyValue.setProperty(p);

                        for (int x = 1; x <= p.getPropertyType().getGroupsCount(); x++) {
                            String groupVal = m.group(groupNum + x);
                            LOGGER.trace("Prop " + p.name() + " group " + (groupNum + x) + " -> " + groupVal);
                            propertyValue.getMatchedValues().add(groupVal);
                        }
                        groupNum += p.getPropertyType().getGroupsCount();

                        props.add(propertyValue);
                    }

                    cmdRes.getValues().put(lineNum, props);
                }
            } else {
                LOGGER.debug("No lines from command received");
            }
        }

        return cmdRes;
    }

}

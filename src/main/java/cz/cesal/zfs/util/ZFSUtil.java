package cz.cesal.zfs.util;

import cz.cesal.util.CommandExecutor;
import cz.cesal.zfs.dto.ZFSCommandResult;
import cz.cesal.zfs.dto.ZFSProperty;
import cz.cesal.zfs.dto.ZFSPropertySource;
import cz.cesal.zfs.Dataset;
import cz.cesal.zfs.Property;
import cz.cesal.util.CommandResult;
import cz.cesal.util.StreamGobbler;
import cz.cesal.zfs.dto.ZFSPropertyValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZFSUtil {

    private static Logger LOGGER = LogManager.getLogger(ZFSUtil.class);

    private static final Pattern ZFS_GET_PATTERN = Pattern.compile("^\\s*(.+?)\\s+(.+?)\\s+(?:(?:inherited from ([^ ]+))|(?:((?:-)|(?:.+))))\\s+(.+)$", Pattern.DOTALL);

    private ZFSUtil() {
    }

    public static List<Dataset> listDatasets() throws IOException, InterruptedException {
        ZFSCommandResult res = executeCommand("zfs list -t filesystem,volume -r", ZFSProperty.name);
        if (res.getCommandResult().getExitCode() != 0) {
            throw new IOException("Bad exit code " + res.getCommandResult().getExitCode());
        }
        List<Dataset> out = new ArrayList<>();
        List<String> lines = res.getCommandResult().getOutputLines();
        LOGGER.debug("listDatasets - got " + lines.size() + " lines");
        for (String line : lines) {
            if (!line.equals("NAME")) {
                Dataset ds = new Dataset();
                ds.setName(line.trim());
                out.add(ds);
            }
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

    public static List<Property> listSnapshots(Dataset dataset, String... onlyColumns) throws IOException, InterruptedException {
        return listSnapshots(dataset.getName(), onlyColumns);
    }

    public static List<Property> listSnapshots(String datasetName, String... onlyColumns) throws IOException, InterruptedException {
        List<String> columnsList = new ArrayList<>();
        if (onlyColumns != null) {
            for (String s : onlyColumns) {
                columnsList.add(s.toLowerCase());
            }
        }
        String cmd = "zfs list -t snap -H " + (columnsList.isEmpty() ? "" : "-o " + String.join(",", columnsList)) + " " + datasetName;
        ZFSCommandResult res = executeCommand(cmd);
        if (res.getCommandResult().getExitCode() != 0) {
            throw new IOException("Bad exit code " + res.getCommandResult().getExitCode());
        }
        List<Property> out = new ArrayList<>();
        List<String> lines = res.getCommandResult().getOutputLines();
        LOGGER.debug("listSnapshots - got " + lines.size() + " lines");
        for (String line : lines) {
            if (!line.startsWith("NAME")) {
                Property prop = new Property();

                Matcher m = ZFS_GET_PATTERN.matcher(line);
                if (!m.matches()) {
                    throw new IOException("Line not matches: " + line);
                }

                prop.setDatasetName(m.group(1));
                prop.setProperty(m.group(2));
                prop.setValue(m.group(3));

                String src = m.group(4);
                if (src.contains("inherited from")) {
                    String ds = src.split("inherited from +")[1];
                    prop.setSource(ZFSPropertySource.INHERITED);
                    prop.setSourceFrom(ds);

                } else if (src.equalsIgnoreCase("-")) {

                    prop.setSource(ZFSPropertySource.NONE);
                } else {
                    prop.setSource(ZFSPropertySource.valueOf(src.toUpperCase()));
                }

                out.add(prop);
            }
        }
        LOGGER.debug("listSnapshots - returning " + out.size() + " snapshots");
        return out;
    }

    public static ZFSCommandResult executeCommand(String command, ZFSProperty... onlyProperties) throws IOException, InterruptedException {
        if (onlyProperties != null && onlyProperties.length >= 1) {
            if (command.contains("-H") || command.contains("-o")) {
                throw new IOException("Cannot have -H/-o specified when specific properties given");
            }
            List<String> propsList = new ArrayList<>();
            for (ZFSProperty p : onlyProperties) {
                propsList.add(p.name());
            }
            command += " -H -o " + String.join(",", propsList);
        }

        CommandResult res = CommandExecutor.executeCommand(command);

        ZFSCommandResult cmdRes = new ZFSCommandResult();
        cmdRes.setCommandResult(res);

        if (onlyProperties != null && onlyProperties.length >= 1) {
            List<String> regexs = new ArrayList<>();
            for (ZFSProperty p : onlyProperties) {
                regexs.add(p.getPropertyType().getPattern().pattern());
            }
            String patternStr = String.join("\s{1,}", regexs);
            Pattern pattern = Pattern.compile(patternStr);
            LOGGER.debug("Created pattern: " + pattern.pattern());

            List<String> lines = res.getOutputLines();
            LOGGER.debug("Processing " + lines.size() + " lines");
            for (String line : lines) {
                Matcher m = pattern.matcher(line);
                if (!m.matches()) {
                    throw new IOException("Not matching line: " + line);
                }

                LOGGER.trace("Matched line has " + m.groupCount() + " groups");
                int groupNum = 0;
                for (ZFSProperty p : onlyProperties) {

                    ZFSPropertyValue propertyValue = new ZFSPropertyValue();
                    propertyValue.setProperty(p);

                    for (int x = 1; x <= p.getPropertyType().getGroupsCount(); x++) {
                        String groupVal = m.group(groupNum + x);
                        LOGGER.debug("Prop " + p.name() + " group " + (groupNum + x) + " -> " + groupVal);
                        propertyValue.getMatchedValues().add(groupVal);
                    }
                    groupNum += p.getPropertyType().getGroupsCount();

                    cmdRes.getValues().add(propertyValue);
                }
            }
        }

        return cmdRes;
    }

}

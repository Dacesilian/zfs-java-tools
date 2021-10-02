package cz.cesal.util;

import cz.cesal.zfs.util.ZFSUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class CommandExecutor {

    private static Logger LOGGER = LogManager.getLogger(CommandExecutor.class);

    private CommandExecutor() {
    }

    public static CommandResult executeCommand(String command) throws IOException, InterruptedException {
        LOGGER.debug("Executing '" + command + "'");
        CommandResult result = new CommandResult();
        ProcessBuilder builder = new ProcessBuilder();
        // builder.command(Arrays.stream(command.split(" +")).toList());
        String[] arr = {"sh", "-c", command};
        builder.command(Arrays.stream(arr).toList());
        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), s -> result.getOutputLines().add(s));
        Executors.newSingleThreadExecutor().submit(streamGobbler);
        result.setExitCode(process.waitFor());
        return result;
    }

}

package cz.cesal.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommandResult {

    List<String> outputLines = new ArrayList<>();
    int exitCode = -1;

}

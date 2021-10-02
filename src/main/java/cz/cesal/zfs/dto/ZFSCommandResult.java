package cz.cesal.zfs.dto;

import cz.cesal.util.CommandResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
public class ZFSCommandResult {

    CommandResult commandResult;
    // Values by lines
    Map<Integer, List<ZFSPropertyValue>> values = new TreeMap<>();

    public List<ZFSPropertyValue> getAllValuesMerged() {
        List<ZFSPropertyValue> out = new ArrayList<>();
        for (Map.Entry<Integer, List<ZFSPropertyValue>> entry : values.entrySet()) {
            out.addAll(entry.getValue());
        }
        return out;
    }

}

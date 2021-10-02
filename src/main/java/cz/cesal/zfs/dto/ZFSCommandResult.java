package cz.cesal.zfs.dto;

import cz.cesal.util.CommandResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ZFSCommandResult {

    CommandResult commandResult;
    List<ZFSPropertyValue> values = new ArrayList<>();

}

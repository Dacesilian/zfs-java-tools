package cz.cesal.zfs.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ZFSPropertyValue {

    ZFSProperty property;
    List<String> matchedValues = new ArrayList<>();

}

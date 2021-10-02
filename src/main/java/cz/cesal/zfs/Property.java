package cz.cesal.zfs;

import cz.cesal.zfs.dto.ZFSPropertySource;
import lombok.Data;
import lombok.ToString;

@Data
public class Property {

    String datasetName;
    String property;
    String value;
    ZFSPropertySource source;
    String sourceFrom;
    @ToString.Exclude
    String line;

}

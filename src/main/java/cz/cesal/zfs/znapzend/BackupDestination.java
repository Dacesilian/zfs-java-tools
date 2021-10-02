package cz.cesal.zfs.znapzend;

import lombok.Data;

@Data
public class BackupDestination {

    String dataset;
    DestinationType type;
    String remoteUser;
    String remoteHost;

}

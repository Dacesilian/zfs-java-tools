package cz.cesal.zfs.znapzend;

import cz.cesal.zfs.Dataset;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DatasetBackup extends Dataset {

    List<String> destinations = new ArrayList<>();

}

package cz.cesal.zfs;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Dataset {

    String name;
    List<Property> properties = new ArrayList<>();

}

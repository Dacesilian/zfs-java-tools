package cz.cesal.zfs.dto;

import java.util.regex.Pattern;

public enum ZFSPropertyType {

    STRING(Pattern.compile("(.+?)"), 1),
    DATASET_NAME(Pattern.compile("([^ ]+)"), 1),
    DATASET_SNAPSHOT(Pattern.compile("([^ @]+)(?:@([^ ]+))?"), 2),
    NUMBER(Pattern.compile("(\\d+(?:.\\d+)?)([A-Z]+)?"), 2),
    NUMBER_OR_NONE(Pattern.compile("(?:(?:(\\d+(?:.\\d+)?)([A-Z]+)?)|(none))"), 3),
    DATETIME(Pattern.compile("([^ ]+) +([^ ]+) +(\\d+) +(\\d+):(\\d+) +(\\d{4})"), 6);

    private final Pattern pattern;
    private final int groupsCount;

    ZFSPropertyType(Pattern pattern, int groupsCount) {
        this.pattern = pattern;
        this.groupsCount = groupsCount;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getGroupsCount() {
        return groupsCount;
    }
}

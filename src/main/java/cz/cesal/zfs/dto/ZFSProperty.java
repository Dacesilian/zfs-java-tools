package cz.cesal.zfs.dto;

public enum ZFSProperty {

    available(ZFSPropertyType.NUMBER),
    compression(ZFSPropertyType.STRING),
    compressratio(ZFSPropertyType.NUMBER),
    creation(ZFSPropertyType.DATETIME),
    quota(ZFSPropertyType.NUMBER_OR_NONE),
    refquota(ZFSPropertyType.NUMBER_OR_NONE),
    type(ZFSPropertyType.STRING),
    name(ZFSPropertyType.STRING),
    datasetName("name", ZFSPropertyType.DATASET_SNAPSHOT),
    usedbychildren(ZFSPropertyType.NUMBER),
    usedbydataset(ZFSPropertyType.NUMBER),
    usedbyrefreservation(ZFSPropertyType.NUMBER),
    usedbysnapshots(ZFSPropertyType.NUMBER),
    volsize(ZFSPropertyType.NUMBER);

    private final ZFSPropertyType propertyType;
    private final String propertyName;

    ZFSProperty(ZFSPropertyType type) {
        this.propertyType = type;
        this.propertyName = this.name();
    }

    ZFSProperty(String propertyName, ZFSPropertyType type) {
        this.propertyType = type;
        this.propertyName = propertyName;
    }

    public ZFSPropertyType getPropertyType() {
        return propertyType;
    }

    public String getPropertyName() {
        return propertyName;
    }
}

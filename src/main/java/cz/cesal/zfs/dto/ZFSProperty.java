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
    usedbychildren(ZFSPropertyType.NUMBER),
    usedbydataset(ZFSPropertyType.NUMBER),
    usedbyrefreservation(ZFSPropertyType.NUMBER),
    usedbysnapshots(ZFSPropertyType.NUMBER),
    volsize(ZFSPropertyType.NUMBER);

    private final ZFSPropertyType propertyType;

    ZFSProperty(ZFSPropertyType type) {
        this.propertyType = type;
    }

    public ZFSPropertyType getPropertyType() {
        return propertyType;
    }
}

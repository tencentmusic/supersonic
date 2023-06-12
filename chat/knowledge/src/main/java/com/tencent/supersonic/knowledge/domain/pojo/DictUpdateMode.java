package com.tencent.supersonic.knowledge.domain.pojo;

public enum DictUpdateMode {

    OFFLINE_FULL("OFFLINE_FULL"),
    OFFLINE_DOMAIN("OFFLINE_DOMAIN"),
    REALTIME_ADD("REALTIME_ADD"),
    REALTIME_DELETE("REALTIME_DELETE"),
    NOT_SUPPORT("NOT_SUPPORT");

    private String value;

    DictUpdateMode(String value) {
        this.value = value;
    }

    public static DictUpdateMode of(String value) {
        for (DictUpdateMode item : DictUpdateMode.values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return DictUpdateMode.NOT_SUPPORT;
    }

    public String getValue() {
        return value;
    }

}
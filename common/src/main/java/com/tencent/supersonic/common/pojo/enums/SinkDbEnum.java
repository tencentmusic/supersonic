package com.tencent.supersonic.common.pojo.enums;

public enum SinkDbEnum {

    TDW("TDW"),

    DORIS("DORIS"),

    ICEBERY("ICEBERY"),


    NOT_SUPPORT("NOT_SUPPORT");


    private String db;

    SinkDbEnum(String db) {
        this.db = db;
    }

    public String getDb() {
        return db;
    }

    public static SinkDbEnum of(String name) {
        for (SinkDbEnum item : SinkDbEnum.values()) {
            if (item.db.equalsIgnoreCase(name)) {
                return item;
            }
        }
        return SinkDbEnum.NOT_SUPPORT;
    }
}

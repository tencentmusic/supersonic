package com.tencent.supersonic.common.pojo.enums;

public enum ConfigMode {
    DETAIL("DETAIL"), AGG("AGG"), UNKNOWN("UNKNOWN");

    private String mode;

    ConfigMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public static ConfigMode of(String agg) {
        for (ConfigMode configMode : ConfigMode.values()) {
            if (configMode.getMode().equalsIgnoreCase(agg)) {
                return configMode;
            }
        }
        return ConfigMode.UNKNOWN;
    }
}

package com.tencent.supersonic.headless.api.pojo.enums;

public enum MapModeEnum {
    STRICT(0), MODERATE(2), LOOSE(4), ALL(6);

    public int threshold;

    MapModeEnum(Integer threshold) {
        this.threshold = threshold;
    }
}

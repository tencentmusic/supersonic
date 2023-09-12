package com.tencent.supersonic.common.pojo.enums;

import org.apache.commons.lang3.StringUtils;

/***
 * nature type
 * such as : metric、dimension etc.
 */
public enum DictWordType {
    METRIC("metric"),
    DIMENSION("dimension"),
    VALUE("value"),

    DOMAIN("dm"),
    MODEL("model"),
    ENTITY("entity"),

    NUMBER("m"),

    SUFFIX("suffix");

    public static final String NATURE_SPILT = "_";
    public static final String SPACE = " ";
    private String type;

    DictWordType(String type) {
        this.type = type;
    }

    public String getType() {
        return NATURE_SPILT + type;
    }


    public static DictWordType getNatureType(String nature) {
        if (StringUtils.isEmpty(nature) || !nature.startsWith(NATURE_SPILT)) {
            return null;
        }
        for (DictWordType dictWordType : values()) {
            if (nature.endsWith(dictWordType.getType())) {
                return dictWordType;
            }
        }
        //domain
        String[] natures = nature.split(DictWordType.NATURE_SPILT);
        if (natures.length == 2 && StringUtils.isNumeric(natures[1])) {
            return DOMAIN;
        }
        //dimension value
        if (natures.length == 3 && StringUtils.isNumeric(natures[1]) && StringUtils.isNumeric(natures[2])) {
            return VALUE;
        }
        return null;
    }
}

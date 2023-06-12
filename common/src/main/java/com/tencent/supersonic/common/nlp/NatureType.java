package com.tencent.supersonic.common.nlp;

import org.apache.commons.lang3.StringUtils;

/***
 * nature type
 * such as : metric、dimension etc.
 */
public enum NatureType {
    METRIC("metric"),
    DIMENSION("dimension"),
    VALUE("value"),

    DOMAIN("dm"),
    ENTITY("entity"),

    NUMBER("m"),

    SUFFIX("suffix");
    private String type;

    public static String NATURE_SPILT = "_";

    public static String SPACE = " ";

    NatureType(String type) {
        this.type = type;
    }

    public String getType() {
        return NATURE_SPILT + type;
    }

    public static NatureType getNatureType(String nature) {
        if (StringUtils.isEmpty(nature) || !nature.startsWith(NATURE_SPILT)) {
            return null;
        }
        for (NatureType natureType : values()) {
            if (nature.endsWith(natureType.getType())) {
                return natureType;
            }
        }
        //domain
        String[] natures = nature.split(NatureType.NATURE_SPILT);
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

package com.tencent.supersonic.common.pojo.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/** * nature type such as : metric、dimension etc. */
public enum DictWordType {
    METRIC("metric"),

    DIMENSION("dimension"),

    VALUE("value"),

    DATASET("dataSet"),

    NUMBER("m"),

    TAG("tag"),

    TERM("term"),

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
        // dataSet
        String[] natures = nature.split(DictWordType.NATURE_SPILT);
        if (natures.length == 2 && StringUtils.isNumeric(natures[1])) {
            return DATASET;
        }
        // dimension value
        if (natures.length == 3 && StringUtils.isNumeric(natures[1])
                && StringUtils.isNumeric(natures[2])) {
            return VALUE;
        }
        return null;
    }

    public static DictWordType of(TypeEnums type) {
        for (DictWordType wordType : DictWordType.values()) {
            if (wordType.name().equalsIgnoreCase(type.name())) {
                return wordType;
            }
        }
        return null;
    }

    public static String getSuffixNature(TypeEnums type) {
        DictWordType wordType = of(type);
        if (Objects.nonNull(wordType)) {
            return wordType.type;
        }
        return "";
    }
}

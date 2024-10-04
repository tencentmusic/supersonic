package com.tencent.supersonic.headless.api.pojo.enums;

import java.util.Objects;

public enum TagType {
    ATOMIC, DERIVED;

    public static TagType of(String src) {
        for (TagType tagType : TagType.values()) {
            if (Objects.nonNull(src) && src.equalsIgnoreCase(tagType.name())) {
                return tagType;
            }
        }
        return null;
    }

    public static Boolean isDerived(String src) {
        TagType tagType = of(src);
        return Objects.nonNull(tagType) && tagType.equals(DERIVED);
    }

    public static TagType getType(TagDefineType tagDefineType) {
        return Objects.nonNull(tagDefineType) && TagDefineType.TAG.equals(tagDefineType)
                ? TagType.DERIVED
                : TagType.ATOMIC;
    }
}

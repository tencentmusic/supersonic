package com.tencent.supersonic.headless.common.model.enums;

import java.util.Objects;

public enum ModelSourceTypeEnum {
    FULL,
    PARTITION,
    ZIPPER;

    public static ModelSourceTypeEnum of(String src) {
        for (ModelSourceTypeEnum modelSourceTypeEnum : ModelSourceTypeEnum.values()) {
            if (Objects.nonNull(src) && src.equalsIgnoreCase(modelSourceTypeEnum.name())) {
                return modelSourceTypeEnum;
            }
        }
        return null;
    }

    public static boolean isZipper(ModelSourceTypeEnum modelSourceTypeEnum) {
        return Objects.nonNull(modelSourceTypeEnum) && ZIPPER.equals(modelSourceTypeEnum);
    }

    public static boolean isZipper(String str) {
        ModelSourceTypeEnum modelSourceTypeEnum = of(str);
        return Objects.nonNull(modelSourceTypeEnum) && isZipper(modelSourceTypeEnum);
    }
}

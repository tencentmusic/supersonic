package com.tencent.supersonic.headless.api.pojo.enums;

import java.util.Objects;

/**
 * model source table type: FULL: table without time partition PARTITION: table with partition field
 * ZIPPER: table with slowly changing dimension
 */
public enum ModelSourceType {
    FULL, PARTITION, ZIPPER;

    public static ModelSourceType of(String src) {
        for (ModelSourceType modelSourceTypeEnum : ModelSourceType.values()) {
            if (Objects.nonNull(src) && src.equalsIgnoreCase(modelSourceTypeEnum.name())) {
                return modelSourceTypeEnum;
            }
        }
        return null;
    }

    public static boolean isZipper(ModelSourceType modelSourceTypeEnum) {
        return Objects.nonNull(modelSourceTypeEnum) && ZIPPER.equals(modelSourceTypeEnum);
    }

    public static boolean isZipper(String str) {
        ModelSourceType modelSourceTypeEnum = of(str);
        return Objects.nonNull(modelSourceTypeEnum) && isZipper(modelSourceTypeEnum);
    }
}

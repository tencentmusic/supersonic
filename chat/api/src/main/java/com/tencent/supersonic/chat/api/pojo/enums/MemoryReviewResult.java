package com.tencent.supersonic.chat.api.pojo.enums;

import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import org.apache.commons.lang3.StringUtils;

public enum MemoryReviewResult {
    POSITIVE, NEGATIVE;

    public static MemoryReviewResult getMemoryReviewResult(String value) {
        String validValue = StringUtils.trim(value);
        for (MemoryReviewResult reviewRet : MemoryReviewResult.values()) {
            if (StringUtils.equalsIgnoreCase(reviewRet.name(), validValue)) {
                return reviewRet;
            }
        }
        throw new InvalidArgumentException("Invalid MemoryReviewResult type:[" + value + "]");
    }
}

package com.tencent.supersonic.common.util;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class DeepCopyUtil {

    public static <T extends Serializable> T deepCopy(T object) {
        return SerializationUtils.clone(object);
    }
}

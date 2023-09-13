package com.tencent.supersonic.knowledge.utils;


import cn.hutool.core.lang.Assert;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import org.junit.jupiter.api.Test;

class NatureHelperTest {

    @Test
    void convertToElementType() {
        SchemaElementType schemaElementType = NatureHelper.convertToElementType("_1");

        Assert.equals(schemaElementType, SchemaElementType.MODEL);
    }
}
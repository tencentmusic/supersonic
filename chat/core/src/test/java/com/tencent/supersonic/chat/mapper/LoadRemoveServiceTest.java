package com.tencent.supersonic.chat.mapper;


import com.hankcs.hanlp.algorithm.EditDistance;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class LoadRemoveServiceTest {

    @Test
    void edit() {
        int compute = EditDistance.compute("在", "在你的身边");
        Assert.assertEquals(compute, 4);
    }
}

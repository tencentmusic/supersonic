package com.tencent.supersonic.chat.core.mapper;


import com.hankcs.hanlp.algorithm.EditDistance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LoadRemoveServiceTest {

    @Test
    void edit() {
        int compute = EditDistance.compute("在", "在你的身边");
        Assertions.assertEquals(compute, 4);
    }
}

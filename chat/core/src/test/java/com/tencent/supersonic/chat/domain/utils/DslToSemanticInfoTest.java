package com.tencent.supersonic.chat.domain.utils;

import com.tencent.supersonic.semantic.api.core.enums.TimeDimensionEnum;
import java.text.MessageFormat;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * @author lex luo
 * @date 2023/6/29 16:05
 */
class DslToSemanticInfoTest {

    @Test
    void search() {

        Integer domainId = 1;
        String dayField = TimeDimensionEnum.DAY.getName();
        String startDate = "2023-04-01";
        String endDate = "2023-06-01";

        String format = MessageFormat.format(DslToSemanticInfo.SUB_TABLE, domainId, dayField, startDate, endDate);

        Assert.assertEquals(format,
                " ( select * from  t_1 where sys_imp_date >= '2023-04-01' and  sys_imp_date <= '2023-06-01' ) as  t_sub_1");

    }
}
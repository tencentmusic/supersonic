package com.tencent.supersonic.chat.corrector;

import static org.mockito.ArgumentMatchers.any;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLDateHelper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DateFieldCorrectorTest {

    @Test
    void corrector() {
        MockedStatic<DSLDateHelper> dslDateHelper = Mockito.mockStatic(DSLDateHelper.class);

        dslDateHelper.when(() -> DSLDateHelper.getReferenceDate(any())).thenReturn("2023-08-14");
        DateFieldCorrector dateFieldCorrector = new DateFieldCorrector();
        SemanticParseInfo parseInfo = new SemanticParseInfo();
        SchemaElement model = new SchemaElement();
        model.setId(2L);
        parseInfo.setModel(model);
        SemanticCorrectInfo semanticCorrectInfo = SemanticCorrectInfo.builder()
                .sql("select count(歌曲名) from 歌曲库 ")
                .parseInfo(parseInfo)
                .build();

        dateFieldCorrector.correct(semanticCorrectInfo);

        Assert.assertEquals("SELECT count(歌曲名) FROM 歌曲库 WHERE 数据日期 = '2023-08-14'", semanticCorrectInfo.getSql());

        semanticCorrectInfo = SemanticCorrectInfo.builder()
                .sql("select count(歌曲名) from 歌曲库 where 数据日期 = '2023-08-14'")
                .parseInfo(parseInfo)
                .build();

        dateFieldCorrector.correct(semanticCorrectInfo);

        Assert.assertEquals("select count(歌曲名) from 歌曲库 where 数据日期 = '2023-08-14'", semanticCorrectInfo.getSql());

    }
}

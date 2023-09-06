package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLParseResult;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq.ElementValue;
import com.tencent.supersonic.common.pojo.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class FieldNameCorrectorTest {

    @Test
    void rewriter() {

        FieldNameCorrector corrector = new FieldNameCorrector();
        CorrectionInfo correctionInfo = CorrectionInfo.builder()
                .sql("select 歌曲名 from 歌曲库 where 专辑照片 = '七里香' and 专辑名 = '流行' and 数据日期 = '2023-08-19'")
                .build();

        SemanticParseInfo parseInfo = new SemanticParseInfo();

        DSLParseResult dslParseResult = new DSLParseResult();
        LLMReq llmReq = new LLMReq();
        List<ElementValue> linking = new ArrayList<>();
        ElementValue elementValue = new ElementValue();
        elementValue.setFieldValue("流行");
        elementValue.setFieldName("歌曲风格");
        linking.add(elementValue);

        ElementValue elementValue2 = new ElementValue();
        elementValue2.setFieldValue("七里香");
        elementValue2.setFieldName("歌曲名");
        linking.add(elementValue2);

        ElementValue elementValue3 = new ElementValue();
        elementValue3.setFieldValue("周杰伦");
        elementValue3.setFieldName("歌手名");
        linking.add(elementValue3);

        ElementValue elementValue4 = new ElementValue();
        elementValue4.setFieldValue("流行");
        elementValue4.setFieldName("歌曲流派");
        linking.add(elementValue4);

        llmReq.setLinking(linking);
        dslParseResult.setLlmReq(llmReq);

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, dslParseResult);

        parseInfo.setProperties(properties);
        correctionInfo.setParseInfo(parseInfo);

        CorrectionInfo rewriter = corrector.corrector(correctionInfo);

        Assert.assertEquals("SELECT 歌曲名 FROM 歌曲库 WHERE 歌曲名 = '七里香' AND 歌曲流派 = '流行' AND 数据日期 = '2023-08-19'",
                rewriter.getSql());
    }
}
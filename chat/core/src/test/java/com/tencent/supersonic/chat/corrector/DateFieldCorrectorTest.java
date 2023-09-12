package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class DateFieldCorrectorTest {

    @Test
    void rewriter() {
        DateFieldCorrector dateFieldCorrector = new DateFieldCorrector();
        SemanticParseInfo parseInfo = new SemanticParseInfo();
        SchemaElement model = new SchemaElement();
        model.setId(2L);
        parseInfo.setModel(model);
        CorrectionInfo correctionInfo = CorrectionInfo.builder()
                .sql("select count(歌曲名) from 歌曲库 ")
                .parseInfo(parseInfo)
                .build();

        CorrectionInfo rewriter = dateFieldCorrector.corrector(correctionInfo);

        Assert.assertEquals("SELECT count(歌曲名) FROM 歌曲库 WHERE 数据日期 = '2023-08-14'", rewriter.getSql());

        correctionInfo = CorrectionInfo.builder()
                .sql("select count(歌曲名) from 歌曲库 where 数据日期 = '2023-08-14'")
                .parseInfo(parseInfo)
                .build();

        rewriter = dateFieldCorrector.corrector(correctionInfo);

        Assert.assertEquals("select count(歌曲名) from 歌曲库 where 数据日期 = '2023-08-14'", rewriter.getSql());

    }
}

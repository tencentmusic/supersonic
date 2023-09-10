package com.tencent.supersonic.chat.corrector;

import static org.mockito.Mockito.when;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaValueMap;
import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class FieldValueCorrectorTest {


    @Test
    void corrector() {

        MockedStatic<ContextUtils> mockContextUtils = Mockito.mockStatic(ContextUtils.class);

        SchemaService mockSchemaService = Mockito.mock(SchemaService.class);

        SemanticSchema mockSemanticSchema = Mockito.mock(SemanticSchema.class);

        List<SchemaElement> dimensions = new ArrayList<>();
        List<SchemaValueMap> schemaValueMaps = new ArrayList<>();
        SchemaValueMap value1 = new SchemaValueMap();
        value1.setBizName("杰伦");
        value1.setTechName("周杰伦");
        value1.setAlias(Arrays.asList("周杰倫", "Jay Chou", "周董", "周先生"));
        schemaValueMaps.add(value1);

        SchemaElement schemaElement = SchemaElement.builder()
                .bizName("singer_name")
                .name("歌手名")
                .model(2L)
                .schemaValueMaps(schemaValueMaps)
                .build();
        dimensions.add(schemaElement);

        when(mockSemanticSchema.getDimensions()).thenReturn(dimensions);
        when(mockSchemaService.getSemanticSchema()).thenReturn(mockSemanticSchema);
        mockContextUtils.when(() -> ContextUtils.getBean(SchemaService.class)).thenReturn(mockSchemaService);

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        SchemaElement model = new SchemaElement();
        model.setId(2L);
        parseInfo.setModel(model);
        SemanticCorrectInfo semanticCorrectInfo = SemanticCorrectInfo.builder()
                .sql("select count(song_name) from 歌曲库 where singer_name = '周先生'")
                .parseInfo(parseInfo)
                .build();

        FieldValueCorrector corrector = new FieldValueCorrector();
        corrector.correct(semanticCorrectInfo);

        Assert.assertEquals("SELECT count(song_name) FROM 歌曲库 WHERE singer_name = '周杰伦'", semanticCorrectInfo.getSql());

        semanticCorrectInfo.setSql("select count(song_name) from 歌曲库 where singer_name = '杰伦'");
        corrector.correct(semanticCorrectInfo);

        Assert.assertEquals("SELECT count(song_name) FROM 歌曲库 WHERE singer_name = '周杰伦'", semanticCorrectInfo.getSql());
    }
}
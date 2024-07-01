package com.tencent.supersonic.util;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary.Attribute;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.chat.knowledge.DatabaseMapResult;
import com.tencent.supersonic.headless.chat.knowledge.MapResult;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class HanlpTest {

    @Test
    public void test() {
        Nature[] nature = new Nature[2];
        nature[0] = Nature.create("_3");
        nature[1] = Nature.create("_4");
        int[] frequency = new int[2];
        frequency[0] = 100;
        frequency[1] = 200;
        String[] originals = new String[2];
        originals[0] = "AA";
        originals[1] = "Aa";
        Attribute att = new Attribute(nature, frequency, originals, 200);
        att.original = "DDDDD";
        HanlpHelper.getDynamicCustomDictionary().getTrie().set("aa", att);
        List<MapResult> mapResults = new ArrayList<>();
        DatabaseMapResult addMapResult = new DatabaseMapResult();
        addMapResult.setName("aa");
        addMapResult.setSchemaElement(new SchemaElement());
        addMapResult.setDetectWord("abc");
        mapResults.add(addMapResult);
        HanlpHelper.transLetterOriginal(mapResults);
        Assert.assertEquals(mapResults.size(), 2);
    }
}
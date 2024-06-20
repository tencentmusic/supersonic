package com.tencent.supersonic.headless.chat.knowledge.builder;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;

import java.util.ArrayList;
import java.util.List;
import org.springframework.util.CollectionUtils;

public abstract class BaseWordWithAliasBuilder extends BaseWordBuilder {

    public abstract DictWord getOneWordNature(String word, SchemaElement schemaElement, boolean isSuffix);

    public List<DictWord> getOneWordNatureAlias(SchemaElement schemaElement, boolean isSuffix) {
        List<DictWord> dictWords = new ArrayList<>();
        if (CollectionUtils.isEmpty(schemaElement.getAlias())) {
            return dictWords;
        }

        for (String alias : schemaElement.getAlias()) {
            dictWords.add(getOneWordNature(alias, schemaElement, isSuffix));
        }
        return dictWords;
    }

}

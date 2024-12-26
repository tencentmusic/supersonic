package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseMapper implements SchemaMapper {

    @Override
    public void map(ChatQueryContext chatQueryContext) {
        if (!accept(chatQueryContext)) {
            return;
        }

        String simpleName = this.getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        log.debug("before {},mapInfo:{}", simpleName,
                chatQueryContext.getMapInfo().getDataSetElementMatches());

        try {
            doMap(chatQueryContext);
            MapFilter.filter(chatQueryContext);
        } catch (Exception e) {
            log.error("work error", e);
        }

        long cost = System.currentTimeMillis() - startTime;
        log.debug("after {},cost:{},mapInfo:{}", simpleName, cost,
                chatQueryContext.getMapInfo().getDataSetElementMatches());
    }

    public abstract void doMap(ChatQueryContext chatQueryContext);

    protected boolean accept(ChatQueryContext chatQueryContext) {
        return true;
    }

    public void addToSchemaMap(SchemaMapInfo schemaMap, Long dataSetId,
            SchemaElementMatch newElementMatch) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches =
                schemaMap.getDataSetElementMatches();
        List<SchemaElementMatch> schemaElementMatches =
                dataSetElementMatches.computeIfAbsent(dataSetId, k -> new ArrayList<>());

        AtomicBoolean shouldAddNew = new AtomicBoolean(true);

        schemaElementMatches.removeIf(existingElementMatch -> {
            if (isEquals(existingElementMatch, newElementMatch)) {
                if (newElementMatch.getSimilarity() > existingElementMatch.getSimilarity()) {
                    return true;
                } else {
                    shouldAddNew.set(false);
                }
            }
            return false;
        });

        if (shouldAddNew.get()) {
            schemaElementMatches.add(newElementMatch);
        }
    }

    private static boolean isEquals(SchemaElementMatch existElementMatch,
            SchemaElementMatch newElementMatch) {
        SchemaElement existElement = existElementMatch.getElement();
        SchemaElement newElement = newElementMatch.getElement();
        if (!existElement.equals(newElement)) {
            return false;
        }
        if (SchemaElementType.VALUE.equals(newElement.getType())) {
            return existElementMatch.getWord().equalsIgnoreCase(newElementMatch.getWord());
        }
        return true;
    }

    public SchemaElement getSchemaElement(Long dataSetId, SchemaElementType elementType,
            Long elementID, SemanticSchema semanticSchema) {
        SchemaElement element = new SchemaElement();
        DataSetSchema dataSetSchema = semanticSchema.getDataSetSchemaMap().get(dataSetId);
        if (Objects.isNull(dataSetSchema)) {
            return null;
        }
        SchemaElement elementDb = dataSetSchema.getElement(elementType, elementID);
        if (Objects.isNull(elementDb)) {
            return null;
        }
        BeanUtils.copyProperties(elementDb, element);
        element.setAlias(getAlias(elementDb));
        return element;
    }

    public List<String> getAlias(SchemaElement element) {
        if (!SchemaElementType.VALUE.equals(element.getType())) {
            return element.getAlias();
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(element.getAlias())
                && StringUtils.isNotEmpty(element.getName())) {
            return element.getAlias().stream()
                    .filter(aliasItem -> aliasItem.contains(element.getName()))
                    .collect(Collectors.toList());
        }
        return element.getAlias();
    }

    public <T> List<T> getMatches(ChatQueryContext chatQueryContext, MatchStrategy matchStrategy) {
        String queryText = chatQueryContext.getRequest().getQueryText();
        List<S2Term> terms =
                HanlpHelper.getTerms(queryText, chatQueryContext.getModelIdToDataSetIds());
        terms = HanlpHelper.getTerms(terms, chatQueryContext.getRequest().getDataSetIds());
        Map<MatchText, List<T>> matchResult = matchStrategy.match(chatQueryContext, terms,
                chatQueryContext.getRequest().getDataSetIds());
        List<T> matches = new ArrayList<>();
        if (Objects.isNull(matchResult)) {
            return matches;
        }
        Optional<List<T>> first = matchResult.entrySet().stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .map(entry -> entry.getValue()).findFirst();

        if (first.isPresent()) {
            matches = first.get();
        }
        return matches;
    }
}

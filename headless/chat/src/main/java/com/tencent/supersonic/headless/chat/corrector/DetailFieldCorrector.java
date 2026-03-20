package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.jsqlparser.SqlValidHelper;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Completes SELECT fields for detail (non-aggregate) queries. When the LLM generates SQL with only
 * dimension columns (e.g., just a date field), this corrector adds all non-sensitive dimensions and
 * metrics from the dataset schema.
 *
 * This corrector runs independently of the RuleSqlCorrector enable/disable flag because field
 * completion is essential for query result completeness, not a "rule correction".
 */
@Slf4j
public class DetailFieldCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        if (SqlValidHelper.isComplexSQL(correctS2SQL)) {
            return;
        }
        List<String> aggregateFields = SqlSelectHelper.getAggregateFields(correctS2SQL);
        if (!CollectionUtils.isEmpty(aggregateFields)) {
            return;
        }
        String completed =
                completeFieldsForDetailQuery(chatQueryContext, semanticParseInfo, correctS2SQL);
        if (completed != null) {
            semanticParseInfo.getSqlInfo().setCorrectedS2SQL(completed);
        }
    }

    private String completeFieldsForDetailQuery(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo, String correctS2SQL) {
        Long dataSetId = semanticParseInfo.getDataSetId();
        if (dataSetId == null) {
            return null;
        }
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        Set<String> selectFields = new HashSet<>(SqlSelectHelper.getSelectFields(correctS2SQL));

        // Collect all metric names (including aliases) for this dataset
        Set<String> allMetricNames = semanticSchema.getMetrics(dataSetId).stream().flatMap(e -> {
            Set<String> names = new HashSet<>();
            names.add(e.getName());
            if (!CollectionUtils.isEmpty(e.getAlias())) {
                names.addAll(e.getAlias());
            }
            return names.stream();
        }).collect(Collectors.toSet());

        // Check if SELECT already contains any metric — if so, this is a targeted query, skip
        boolean hasMetricInSelect = selectFields.stream().anyMatch(allMetricNames::contains);
        if (hasMetricInSelect) {
            return null;
        }

        // No metrics in SELECT → collect non-sensitive field names to add.
        // Exclude HIGH sensitive fields to avoid triggering permission denial
        // when auto-completing detail queries.
        int sensitiveThreshold = SensitiveLevelEnum.HIGH.getCode();
        Set<String> allFieldNames = new LinkedHashSet<>();
        for (SchemaElement dim : semanticSchema.getDimensions(dataSetId)) {
            if (dim.getSensitiveLevel() < sensitiveThreshold) {
                allFieldNames.add(dim.getName());
            }
        }
        for (SchemaElement metric : semanticSchema.getMetrics(dataSetId)) {
            if (metric.getSensitiveLevel() < sensitiveThreshold) {
                allFieldNames.add(metric.getName());
            }
        }
        // Remove fields already in SELECT
        allFieldNames.removeAll(selectFields);
        if (allFieldNames.isEmpty()) {
            return null;
        }
        log.info("detail query detected, completing SELECT with {} fields: {}",
                allFieldNames.size(), allFieldNames);
        return SqlAddHelper.addFieldsToSelect(correctS2SQL, new ArrayList<>(allFieldNames));
    }
}

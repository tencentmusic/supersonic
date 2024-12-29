package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.ValueDistribution;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.service.TagQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class TagQueryServiceImpl implements TagQueryService {

    @Value("${item.value.date.before:3}")
    private Integer dayBefore;

    @Value("${s2.item.value.date.format:yyyy-MM-dd}")
    private String itemValueDateFormat;

    // 因有些数据库(如Postgresql)不支持列名大小写，所以列名统一使用小写，以兼容更多数据库
    // private final String tagValueAlias = "internalTagCount";
    private final String tagValueAlias = "internal_tag_count";
    private final String maxDateAlias = "internal_max_date";
    private final TagMetaService tagMetaService;
    private final SemanticLayerService queryService;
    private final ModelService modelService;
    private final SqlGenerateUtils sqlGenerateUtils;

    public TagQueryServiceImpl(TagMetaService tagMetaService, SemanticLayerService queryService,
            ModelService modelService, SqlGenerateUtils sqlGenerateUtils) {
        this.tagMetaService = tagMetaService;
        this.queryService = queryService;
        this.modelService = modelService;
        this.sqlGenerateUtils = sqlGenerateUtils;
    }

    @Override
    public ItemValueResp queryTagValue(ItemValueReq itemValueReq, User user) throws Exception {
        ItemValueResp itemValueResp = new ItemValueResp();
        itemValueResp.setItemId(itemValueReq.getId());
        itemValueResp.setType(SchemaElementType.TAG);
        TagResp tag = tagMetaService.getTag(itemValueReq.getId(), user);
        if (Objects.isNull(tag)) {
            return null;
        }
        checkTag(tag);
        itemValueResp.setName(tag.getName());
        itemValueResp.setBizName(tag.getBizName());
        correctDateConf(itemValueReq, tag, user);
        // tag total count
        Long totalCount = queryTagTotalCount(tag, itemValueReq, user);
        // tag value
        QuerySqlReq querySqlReq = generateReq(tag, itemValueReq);
        SemanticQueryResp semanticQueryResp = queryService.queryByReq(querySqlReq, user);
        fillTagValueInfo(itemValueResp, semanticQueryResp, totalCount);
        return itemValueResp;
    }

    private void checkTag(TagResp tag) throws Exception {
        if (Objects.nonNull(tag)
                && TagDefineType.METRIC.name().equalsIgnoreCase(tag.getTagDefineType())) {
            throw new Exception("do not support value distribution query for tag (from metric): "
                    + tag.getBizName());
        }
    }

    private void correctDateConf(ItemValueReq itemValueReq, TagResp tag, User user)
            throws Exception {
        ModelResp model = modelService.getModel(tag.getModelId());
        List<Dimension> timeDimension = model.getTimeDimension();
        if (CollectionUtils.isEmpty(timeDimension)) {
            itemValueReq.setDateConf(null);
            return;
        }
        if (Objects.nonNull(itemValueReq.getDateConf())
                && itemValueReq.getDateConf().getUnit() == 1) {
            return;
        }

        // query date info from db
        String endDate = queryTagDateFromDbBySql(timeDimension.get(0), tag, itemValueReq, user);
        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.BETWEEN);
        dateConf.setStartDate(endDate);
        dateConf.setEndDate(endDate);
        itemValueReq.setDateConf(dateConf);
    }

    private String queryTagDate(Dimension dim) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dim.getDateFormat());
        return LocalDate.now().plusDays(-dayBefore).format(formatter);
    }

    private String queryTagDateFromDbBySql(Dimension dim, TagResp tag, ItemValueReq itemValueReq,
            User user) {

        String sqlPattern = "select max(%s)  as %s from tbl where %s is not null";
        String sql = String.format(sqlPattern, itemValueReq.getDateConf().getDateField(),
                maxDateAlias, tag.getBizName());

        // 添加时间过滤信息
        log.info("[queryTagDateFromDbBySql] calculate the maximum time start");
        if (Objects.nonNull(itemValueReq) && itemValueReq.getDateConf().getUnit() > 1) {
            ModelResp model = modelService.getModel(tag.getModelId());
            if (Objects.nonNull(model)) {
                List<Dimension> timeDims = model.getTimeDimension();
                if (!CollectionUtils.isEmpty(timeDims)) {
                    String dateFormat = timeDims.get(0).getDateFormat();
                    if (StringUtils.isEmpty(dateFormat)) {
                        dateFormat = itemValueDateFormat;
                    }
                    String start = LocalDate.now().minusDays(itemValueReq.getDateConf().getUnit())
                            .format(DateTimeFormatter.ofPattern(dateFormat));
                    String end = LocalDate.now().minusDays(0)
                            .format(DateTimeFormatter.ofPattern(dateFormat));
                    sql = sql + String.format(" and ( %s > '%s' and %s <= '%s' )",
                            itemValueReq.getDateConf().getDateField(), start,
                            itemValueReq.getDateConf().getDateField(), end);
                }
            }
        }
        log.info("[queryTagDateFromDbBySql] calculate the maximum time end");
        Set<Long> modelIds = new HashSet<>();
        modelIds.add(tag.getModelId());
        QuerySqlReq querySqlReq = new QuerySqlReq();
        querySqlReq.setSql(sql);
        querySqlReq.setNeedAuth(false);
        querySqlReq.setModelIds(modelIds);
        log.info("queryTagDateFromDbBySql, QuerySqlReq:{}", querySqlReq.toCustomizedString());
        try {
            SemanticQueryResp semanticQueryResp = queryService.queryByReq(querySqlReq, user);
            if (!CollectionUtils.isEmpty(semanticQueryResp.getResultList())) {
                Object date = semanticQueryResp.getResultList().get(0).get(maxDateAlias);
                if (Objects.nonNull(date)) {
                    return date.toString();
                }
            }
        } catch (Exception e) {
            log.warn("queryTagDateFromDbBySql date info e, e:{}", e);
        }
        String dateDefault = queryTagDate(dim);
        log.info("queryTagDate by default, dateDefault:{}.", dateDefault);
        return dateDefault;
    }

    private Long queryTagTotalCount(TagResp tag, ItemValueReq itemValueReq, User user)
            throws Exception {
        String sqlPattern = "select count(1)  as %s from tbl where %s is not null %s";
        String dateFilter = getDateFilter(itemValueReq);
        String sql = String.format(sqlPattern, tagValueAlias, tag.getBizName(), dateFilter);
        Set<Long> modelIds = new HashSet<>();
        modelIds.add(tag.getModelId());
        QuerySqlReq querySqlReq = new QuerySqlReq();
        querySqlReq.setSql(sql);
        querySqlReq.setNeedAuth(false);
        querySqlReq.setModelIds(modelIds);

        SemanticQueryResp semanticQueryResp = queryService.queryByReq(querySqlReq, user);
        if (!CollectionUtils.isEmpty(semanticQueryResp.getResultList())) {
            Object total = semanticQueryResp.getResultList().get(0).get(tagValueAlias);
            if (Objects.nonNull(total)) {
                long tagCount = Long.parseLong(total.toString());
                log.info("queryTagTotalCount, tagCount:{}, tagId:{}", tagCount, tag.getId());
                return Long.parseLong(total.toString());
            }
        }
        throw new RuntimeException("queryTagTotalCount error");
    }

    private String getDateFilter(ItemValueReq itemValueReq) {
        if (Objects.isNull(itemValueReq.getDateConf())) {
            return "";
        }
        String dateWhereClause =
                sqlGenerateUtils.getDateWhereClause(itemValueReq.getDateConf(), null);
        return " and " + dateWhereClause;
    }

    private void fillTagValueInfo(ItemValueResp itemValueResp, SemanticQueryResp semanticQueryResp,
            Long totalCount) {
        List<ValueDistribution> valueDistributionList = new ArrayList<>();
        List<Map<String, Object>> resultList = semanticQueryResp.getResultList();
        if (!CollectionUtils.isEmpty(resultList)) {
            resultList.stream().forEach(line -> {
                Object tagValue = line.get(itemValueResp.getBizName());
                Long tagValueCount = Long.parseLong(line.get(tagValueAlias).toString());
                valueDistributionList.add(ValueDistribution.builder().totalCount(totalCount)
                        .valueMap(tagValue).valueCount(tagValueCount)
                        .ratio(1.0 * tagValueCount / totalCount).build());
            });
        }
        itemValueResp.setValueDistributionList(valueDistributionList);
    }

    private QuerySqlReq generateReq(TagResp tag, ItemValueReq itemValueReq) {
        String sqlPattern = "select %s, count(1)  as %s from tbl where %s is not null %s "
                + "group by %s order by %s desc";
        String sql = String.format(sqlPattern, tag.getBizName(), tagValueAlias, tag.getBizName(),
                getDateFilter(itemValueReq), tag.getBizName(), tag.getBizName());

        Set<Long> modelIds = new HashSet<>();
        modelIds.add(tag.getModelId());
        QuerySqlReq querySqlReq = new QuerySqlReq();
        querySqlReq.setSql(sql);
        querySqlReq.setNeedAuth(false);
        querySqlReq.setModelIds(modelIds);
        return querySqlReq;
    }

    private DateConf generateDateConf(ItemValueReq itemValueReq) {
        DateConf dateConf = itemValueReq.getDateConf();
        if (Objects.isNull(dateConf)) {
            dateConf = new DateConf();
            dateConf.setDateMode(DateConf.DateMode.RECENT);
            dateConf.setUnit(1);
        }
        return dateConf;
    }
}

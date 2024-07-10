package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.ItemValueConfig;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.api.pojo.response.DictTaskResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.headless.server.web.service.DimensionService;
import com.tencent.supersonic.headless.server.web.service.MetricService;
import com.tencent.supersonic.headless.server.web.service.ModelService;
import com.tencent.supersonic.headless.server.web.service.TagMetaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static com.tencent.supersonic.common.pojo.Constants.AND_UPPER;
import static com.tencent.supersonic.common.pojo.Constants.APOSTROPHE;
import static com.tencent.supersonic.common.pojo.Constants.COMMA;
import static com.tencent.supersonic.common.pojo.Constants.POUND;
import static com.tencent.supersonic.common.pojo.Constants.SPACE;

@Slf4j
@Component
public class DictUtils {
    @Value("${s2.dimension.multi.value.split:#}")
    private String dimMultiValueSplit;

    @Value("${s2.item.value.max.count:100000}")
    private Long itemValueMaxCount;

    @Value("${s2.item.value.white.frequency:999999}")
    private Long itemValueWhiteFrequency;

    // 前多少天
    @Value("${s2.item.value.date.start:1}")
    private Integer itemValueDateStart;
    @Value("${s2.item.value.date.end:1}")
    // 前多少天
    private Integer itemValueDateEnd;

    @Value("${s2.item.value.date.format:yyyy-MM-dd}")
    private String itemValueDateFormat;


    private final DimensionService dimensionService;
    private final MetricService metricService;
    private final SemanticLayerService queryService;
    private final ModelService modelService;
    private final TagMetaService tagMetaService;

    public DictUtils(DimensionService dimensionService,
                     MetricService metricService,
                     SemanticLayerService queryService,
                     ModelService modelService,
                     @Lazy TagMetaService tagMetaService) {
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.queryService = queryService;
        this.modelService = modelService;
        this.tagMetaService = tagMetaService;
    }

    public String fetchDictFileName(DictItemResp dictItemResp) {
        return String.format("dic_value_%d_%s_%s", dictItemResp.getModelId(), dictItemResp.getType().name(),
                dictItemResp.getItemId());
    }

    public DictTaskDO generateDictTaskDO(DictItemResp dictItemResp, User user, TaskStatusEnum status) {
        DictTaskDO taskDO = new DictTaskDO();
        Date createAt = new Date();
        String name = dictItemResp.fetchDictFileName();
        taskDO.setName(name);
        taskDO.setType(dictItemResp.getType().name());
        taskDO.setItemId(dictItemResp.getItemId());
        taskDO.setConfig(JsonUtil.toString(dictItemResp.getConfig()));
        taskDO.setStatus(status.getStatus());
        taskDO.setCreatedAt(createAt);
        String creator = (Objects.isNull(user) || StringUtils.isEmpty(user.getName())) ? "" : user.getName();
        taskDO.setCreatedBy(creator);
        return taskDO;
    }

    public DictConfDO generateDictConfDO(DictItemReq itemValueReq, User user) {
        DictConfDO confDO = new DictConfDO();
        BeanUtils.copyProperties(itemValueReq, confDO);
        confDO.setType(itemValueReq.getType().name());
        confDO.setConfig(JsonUtil.toString(itemValueReq.getConfig()));
        Date createAt = new Date();
        confDO.setCreatedAt(createAt);
        String creator = StringUtils.isEmpty(user.getName()) ? "" : user.getName();
        confDO.setCreatedBy(creator);
        confDO.setStatus(itemValueReq.getStatus().name());
        return confDO;
    }

    public List<DictItemResp> dictDOList2Req(List<DictConfDO> dictConfDOList) {
        List<DictItemResp> dictItemReqList = new ArrayList<>();
        dictConfDOList.stream().forEach(conf -> {
            DictItemResp dictItemResp = dictDO2Req(conf);
            if (Objects.nonNull(dictItemResp)) {
                dictItemReqList.add(dictDO2Req(conf));
            }

        });
        return dictItemReqList;
    }

    public DictItemResp dictDO2Req(DictConfDO dictConfDO) {
        DictItemResp dictItemResp = new DictItemResp();
        BeanUtils.copyProperties(dictConfDO, dictItemResp);
        dictItemResp.setType(TypeEnums.valueOf(dictConfDO.getType()));
        dictItemResp.setConfig(JsonUtil.toObject(dictConfDO.getConfig(), ItemValueConfig.class));
        dictItemResp.setStatus(StatusEnum.of(dictConfDO.getStatus()));
        if (TypeEnums.DIMENSION.equals(TypeEnums.valueOf(dictConfDO.getType()))) {
            DimensionResp dimension = dimensionService.getDimension(dictConfDO.getItemId());
            if (Objects.isNull(dimension)) {
                log.info("dimension is null, dictConfDO:{}", dictConfDO);
                return null;
            }
            dictItemResp.setModelId(dimension.getModelId());
            dictItemResp.setBizName(dimension.getBizName());
        }
        if (TypeEnums.TAG.equals(TypeEnums.valueOf(dictConfDO.getType()))) {
            TagResp tagResp = tagMetaService.getTag(dictConfDO.getItemId(), User.getFakeUser());
            dictItemResp.setModelId(tagResp.getModelId());
            dictItemResp.setBizName(tagResp.getBizName());
        }

        return dictItemResp;
    }

    public List<String> fetchItemValue(DictItemResp dictItemResp) {
        List<String> lines = new ArrayList<>();
        SemanticQueryReq semanticQueryReq = constructQueryReq(dictItemResp);
        semanticQueryReq.setNeedAuth(false);
        String bizName = dictItemResp.getBizName();
        try {
            SemanticQueryResp semanticQueryResp = queryService.queryByReq(semanticQueryReq, null);
            if (Objects.isNull(semanticQueryResp) || CollectionUtils.isEmpty(semanticQueryResp.getResultList())) {
                return lines;
            }
            Map<String, Long> valueAndFrequencyPair = new HashMap<>(2000);
            for (Map<String, Object> line : semanticQueryResp.getResultList()) {

                if (CollectionUtils.isEmpty(line) || !line.containsKey(bizName)
                        || line.get(bizName) == null || line.size() != 2) {
                    continue;
                }
                String dimValue = line.get(bizName).toString();
                Object metricObject = null;
                for (String key : line.keySet()) {
                    if (!bizName.equalsIgnoreCase(key)) {
                        metricObject = line.get(key);
                    }
                }
                if (!StringUtils.isEmpty(dimValue) && Objects.nonNull(metricObject)) {
                    Long metric = Math.round(Double.parseDouble(metricObject.toString()));
                    mergeMultivaluedValue(valueAndFrequencyPair, dimValue, metric);
                }
            }
            String nature = dictItemResp.getNature();
            constructDictLines(valueAndFrequencyPair, lines, nature);
            addWhiteValueLines(dictItemResp, lines, nature);
        } catch (Exception e) {
            log.error("dictItemResp:{},fetchItemValue error:", dictItemResp, e);
        }
        return lines;
    }

    private void addWhiteValueLines(DictItemResp dictItemResp, List<String> lines, String nature) {
        if (Objects.isNull(dictItemResp) || Objects.isNull(dictItemResp.getConfig())
                || CollectionUtils.isEmpty(dictItemResp.getConfig().getWhiteList())) {
            return;
        }
        List<String> whiteList = dictItemResp.getConfig().getWhiteList();
        whiteList.forEach(white -> {
            if (!StringUtils.isEmpty(white)) {
                white = white.replace(SPACE, POUND);
            }
            lines.add(String.format("%s %s %s", white, nature, itemValueWhiteFrequency));
        });
    }

    private void constructDictLines(Map<String, Long> valueAndFrequencyPair, List<String> lines, String nature) {
        if (CollectionUtils.isEmpty(valueAndFrequencyPair)) {
            return;
        }

        valueAndFrequencyPair.forEach((value, frequency) -> {
            if (!StringUtils.isEmpty(value)) {
                value = value.replace(SPACE, POUND);
            }
            lines.add(String.format("%s %s %s", value, nature, frequency));
        });
    }

    private void mergeMultivaluedValue(Map<String, Long> valueAndFrequencyPair, String dimValue, Long metric) {
        if (StringUtils.isEmpty(dimValue)) {
            return;
        }
        Map<String, Long> tmp = new HashMap<>();
        if (dimValue.contains(dimMultiValueSplit)) {
            Arrays.stream(dimValue.split(dimMultiValueSplit))
                    .forEach(dimValueSingle -> tmp.put(dimValueSingle, metric));
        } else {
            tmp.put(dimValue, metric);
        }

        for (String value : tmp.keySet()) {
            long metricOld = valueAndFrequencyPair.containsKey(value) ? valueAndFrequencyPair.get(value) : 0L;
            valueAndFrequencyPair.put(value, metric + metricOld);
        }
    }

    private SemanticQueryReq constructQueryReq(DictItemResp dictItemResp) {
        if (TypeEnums.DIMENSION.equals(dictItemResp.getType())) {
            return constructDimQueryReq(dictItemResp);
        }
        log.warn("constructQueryReq failed");
        return null;
    }

    private QuerySqlReq constructTagQueryReq(DictItemResp dictItemResp) {

        String sqlPattern = "select %s, %s from tbl %s group by %s order by %s desc limit %d";
        String bizName = dictItemResp.getBizName();
        String whereStr = generateWhereStr(dictItemResp);
        String where = StringUtils.isEmpty(whereStr) ? "" : "WHERE" + whereStr;
        ItemValueConfig config = dictItemResp.getConfig();
        Long limit = (Objects.isNull(config) || Objects.isNull(config.getLimit())) ? itemValueMaxCount :
                dictItemResp.getConfig().getLimit();

        // todo 自定义指标
        Set<Long> modelIds = new HashSet<>();
        String metric = "count(1)";
        if (Objects.nonNull(dictItemResp.getConfig()) && Objects.nonNull(dictItemResp.getConfig().getMetricId())) {
            Long metricId = dictItemResp.getConfig().getMetricId();
            MetricResp metricResp = metricService.getMetric(metricId);
            String metricBizName = metricResp.getBizName();
            metric = String.format("sum(%s)", metricBizName);
            modelIds.add(metricResp.getModelId());
        }

        String sql = String.format(sqlPattern, bizName, metric, where, bizName, metric, limit);
        modelIds.add(dictItemResp.getModelId());
        QuerySqlReq querySqlReq = new QuerySqlReq();
        querySqlReq.setSql(sql);
        querySqlReq.setNeedAuth(false);
        querySqlReq.setModelIds(modelIds);
        return querySqlReq;
    }

    private QuerySqlReq constructDimQueryReq(DictItemResp dictItemResp) {
        if (Objects.nonNull(dictItemResp) && Objects.nonNull(dictItemResp.getConfig())
                && Objects.nonNull(dictItemResp.getConfig().getMetricId())) {
            // 查询默认指标
            QueryStructReq queryStructReq = generateQueryStruct(dictItemResp);
            return queryStructReq.convert(true);
        }
        // count(1) 作为指标
        return constructQuerySqlReq(dictItemResp);
    }

    private QuerySqlReq constructQuerySqlReq(DictItemResp dictItemResp) {

        String sqlPattern = "select %s,count(1) from tbl %s group by %s order by count(1) desc limit %d";
        String bizName = dictItemResp.getBizName();
        String whereStr = generateWhereStr(dictItemResp);
        String where = StringUtils.isEmpty(whereStr) ? "" : "WHERE" + whereStr;
        ItemValueConfig config = dictItemResp.getConfig();
        Long limit = (Objects.isNull(config) || Objects.isNull(config.getLimit())) ? itemValueMaxCount :
                dictItemResp.getConfig().getLimit();
        String sql = String.format(sqlPattern, bizName, where, bizName, limit);
        Set<Long> modelIds = new HashSet<>();
        modelIds.add(dictItemResp.getModelId());
        QuerySqlReq querySqlReq = new QuerySqlReq();
        querySqlReq.setSql(sql);
        querySqlReq.setNeedAuth(false);
        querySqlReq.setModelIds(modelIds);

        return querySqlReq;
    }

    private QueryStructReq generateQueryStruct(DictItemResp dictItemResp) {
        QueryStructReq queryStructReq = new QueryStructReq();

        Set<Long> modelIds = new HashSet<>(Arrays.asList(dictItemResp.getModelId()));
        List<String> groups = new ArrayList<>(Arrays.asList(dictItemResp.getBizName()));
        queryStructReq.setGroups(groups);

        List<Filter> filters = generateFilters(dictItemResp);
        queryStructReq.setDimensionFilters(filters);

        List<Aggregator> aggregators = new ArrayList<>();
        Long metricId = dictItemResp.getConfig().getMetricId();
        MetricResp metric = metricService.getMetric(metricId);
        String metricBizName = metric.getBizName();
        aggregators.add(new Aggregator(metricBizName, AggOperatorEnum.SUM));
        queryStructReq.setAggregators(aggregators);
        modelIds.add(metric.getModelId());
        queryStructReq.setModelIds(modelIds);

        List<Order> orders = new ArrayList<>();
        orders.add(new Order(metricBizName, Constants.DESC_UPPER));
        queryStructReq.setOrders(orders);

        fillStructDateInfo(queryStructReq, dictItemResp);

        Long limit = Objects.isNull(dictItemResp.getConfig().getLimit()) ? itemValueMaxCount :
                dictItemResp.getConfig().getLimit();
        queryStructReq.setLimit(limit);
        queryStructReq.setNeedAuth(false);
        return queryStructReq;
    }

    private void fillStructDateInfo(QueryStructReq queryStructReq, DictItemResp dictItemResp) {

        ItemValueConfig config = dictItemResp.getConfig();

        ModelResp model = modelService.getModel(dictItemResp.getModelId());
        // 用户未进行设置
        if (Objects.isNull(config) || Objects.isNull(config.getDateConf())) {
            fillStructDateBetween(queryStructReq, model, itemValueDateStart, itemValueDateEnd);
            return;
        }

        // 全表扫描
        if (DateConf.DateMode.ALL.equals(config.getDateConf().getDateMode())) {
            return;
        }
        // 静态日期
        if (DateConf.DateMode.BETWEEN.equals(config.getDateConf().getDateMode())) {
            DateConf dateConf = new DateConf();
            BeanMapper.mapper(config.getDateConf(), dateConf);
            dateConf.setDateMode(DateConf.DateMode.BETWEEN);
            queryStructReq.setDateInfo(dateConf);
            return;
        }
        // 动态日期 包括今天日期
        if (DateConf.DateMode.RECENT.equals(config.getDateConf().getDateMode())) {
            fillStructDateBetween(queryStructReq, model, config.getDateConf().getUnit() - 1, 0);
            return;
        }
        return;
    }

    private void fillStructDateBetween(QueryStructReq queryStructReq, ModelResp model,
                                       Integer itemValueDateStart, Integer itemValueDateEnd) {
        if (Objects.nonNull(model)) {
            List<Dim> timeDims = model.getTimeDimension();
            if (!CollectionUtils.isEmpty(timeDims)) {
                DateConf dateConf = new DateConf();
                dateConf.setDateMode(DateConf.DateMode.BETWEEN);
                String format = timeDims.get(0).getDateFormat();
                String start = LocalDate.now().minusDays(itemValueDateStart)
                        .format(DateTimeFormatter.ofPattern(format));
                String end = LocalDate.now().minusDays(itemValueDateEnd)
                        .format(DateTimeFormatter.ofPattern(format));
                dateConf.setStartDate(start);
                dateConf.setEndDate(end);
                queryStructReq.setDateInfo(dateConf);
            }
        }
    }

    private List<Filter> generateFilters(DictItemResp dictItemResp) {
        List<Filter> filters = new ArrayList<>();
        if (Objects.isNull(dictItemResp)) {
            return new ArrayList<>();
        }
        String whereStr = generateWhereStr(dictItemResp);
        if (StringUtils.isEmpty(whereStr)) {
            return new ArrayList<>();
        }
        Filter filter = new Filter("", FilterOperatorEnum.SQL_PART, whereStr);
        filters.add(filter);
        return filters;
    }

    public String generateWhereStr(DictItemResp dictItemResp) {
        StringJoiner joiner = new StringJoiner(SPACE + AND_UPPER + SPACE);

        String bizName = dictItemResp.getBizName();
        ItemValueConfig config = dictItemResp.getConfig();
        if (Objects.nonNull(config)) {
            if (!CollectionUtils.isEmpty(config.getBlackList())) {
                StringJoiner joinerBlack = new StringJoiner(COMMA);
                config.getBlackList().stream().forEach(black -> joinerBlack.add(APOSTROPHE + black + APOSTROPHE));
                joiner.add(String.format("(%s not in (%s))", bizName, joinerBlack.toString()));
            }

            if (!CollectionUtils.isEmpty(config.getRuleList())) {
                config.getRuleList().stream().forEach(rule -> joiner.add("(" + rule + ")"));
            }
        }

        String dateFilter = generateDictDateFilter(dictItemResp);
        if (StringUtils.isNotEmpty(dateFilter)) {
            joiner.add(dateFilter);
        }
        return joiner.toString();
    }

    public String defaultDateFilter() {
        String format = itemValueDateFormat;
        String start = LocalDate.now().minusDays(itemValueDateStart)
                .format(DateTimeFormatter.ofPattern(format));
        String end = LocalDate.now().minusDays(itemValueDateEnd)
                .format(DateTimeFormatter.ofPattern(format));
        return String.format("( %s >= '%s' and %s <= '%s' )", TimeDimensionEnum.DAY.getName(), start,
                TimeDimensionEnum.DAY.getName(), end);
    }

    private String generateDictDateFilter(DictItemResp dictItemResp) {
        ItemValueConfig config = dictItemResp.getConfig();
        // 未进行设置
        if (Objects.isNull(config) || Objects.isNull(config.getDateConf())) {
            return defaultDateFilter();
        }
        // 全表扫描
        if (DateConf.DateMode.ALL.equals(config.getDateConf().getDateMode())) {
            return "";
        }
        // 静态日期
        if (DateConf.DateMode.BETWEEN.equals(config.getDateConf().getDateMode())) {
            return String.format("( %s >= '%s' and %s <= '%s' )",
                    TimeDimensionEnum.DAY.getName(),
                    config.getDateConf().getStartDate(),
                    TimeDimensionEnum.DAY.getName(),
                    config.getDateConf().getEndDate());
        }
        // 动态日期
        if (DateConf.DateMode.RECENT.equals(config.getDateConf().getDateMode())) {
            return generateDictDateFilterRecent(dictItemResp);
        }

        return "";
    }

    private String generateDictDateFilterRecent(DictItemResp dictItemResp) {
        ModelResp model = modelService.getModel(dictItemResp.getModelId());
        if (Objects.nonNull(model)) {
            List<Dim> timeDims = model.getTimeDimension();
            if (!CollectionUtils.isEmpty(timeDims)) {
                String dateFormat = timeDims.get(0).getDateFormat();
                if (StringUtils.isEmpty(dateFormat)) {
                    dateFormat = itemValueDateFormat;
                }
                String start = LocalDate.now().minusDays(dictItemResp.getConfig().getDateConf().getUnit())
                        .format(DateTimeFormatter.ofPattern(dateFormat));
                String end = LocalDate.now().minusDays(0)
                        .format(DateTimeFormatter.ofPattern(dateFormat));
                return String.format("( %s > '%s' and %s <= '%s' )", TimeDimensionEnum.DAY.getName(), start,
                        TimeDimensionEnum.DAY.getName(), end);
            }
        }
        return "";
    }

    public DictTaskResp taskDO2Resp(DictTaskDO dictTaskDO) {
        DictTaskResp resp = new DictTaskResp();
        BeanUtils.copyProperties(dictTaskDO, resp);
        resp.setTaskStatus(dictTaskDO.getStatus());
        resp.setType(TypeEnums.valueOf(dictTaskDO.getType()));
        resp.setConfig(JsonUtil.toObject(dictTaskDO.getConfig(), ItemValueConfig.class));
        return resp;
    }
}

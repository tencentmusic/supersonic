package com.tencent.supersonic.headless.server.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.ValueDistribution;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryTagReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.service.TagQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tencent.supersonic.common.pojo.Constants.DESC_UPPER;

@Service
public class TagQueryServiceImpl implements TagQueryService {

    @Value("${item.value.date.before:1}")
    private Integer dayBefore;

    private final String tagValueAlias = "internalTagCount";
    private final TagMetaService tagMetaService;
    private final QueryService queryService;
    private final ModelService modelService;

    public TagQueryServiceImpl(TagMetaService tagMetaService, QueryService queryService,
                               ModelService modelService) {
        this.tagMetaService = tagMetaService;
        this.queryService = queryService;
        this.modelService = modelService;
    }

    @Override
    public ItemValueResp queryTagValue(ItemValueReq itemValueReq, User user) throws Exception {
        ItemValueResp itemValueResp = new ItemValueResp();
        itemValueResp.setItemId(itemValueReq.getItemId());
        itemValueResp.setType(SchemaElementType.TAG);
        TagResp tag = tagMetaService.getTag(itemValueReq.getItemId(), user);
        itemValueResp.setName(tag.getName());
        itemValueResp.setBizName(tag.getBizName());
        correctDateConf(itemValueReq, tag);
        // tag total count
        Long totalCount = queryTagTotalCount(tag, itemValueReq, user);
        // tag value
        QueryTagReq queryTagReq = generateTagReq(tag, itemValueReq);
        SemanticQueryResp semanticQueryResp = queryService.queryByReq(queryTagReq, user);
        fillTagValueInfo(itemValueResp, semanticQueryResp, totalCount);
        return itemValueResp;
    }

    private void correctDateConf(ItemValueReq itemValueReq, TagResp tag) {
        if (Objects.nonNull(itemValueReq.getDateConf())) {
            return;
        }
        ModelResp model = modelService.getModel(tag.getModelId());
        List<Dim> timeDimension = model.getTimeDimension();
        if (CollectionUtils.isEmpty(timeDimension)) {
            return;
        }

        Dim dim = timeDimension.get(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dim.getDateFormat());
        String endDate = LocalDate.now().plusDays(-dayBefore).format(formatter);
        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.BETWEEN);
        dateConf.setStartDate(endDate);
        dateConf.setEndDate(endDate);
        itemValueReq.setDateConf(dateConf);
    }

    private Long queryTagTotalCount(TagResp tag, ItemValueReq itemValueReq, User user) throws Exception {

        QueryTagReq queryTagReq = new QueryTagReq();
        queryTagReq.addModelId(tag.getModelId());
        queryTagReq.setLimit(1L);
        List<Aggregator> aggregators = new ArrayList<>();
        aggregators.add(new Aggregator(tag.getBizName(), AggOperatorEnum.COUNT, tagValueAlias));
        queryTagReq.setAggregators(aggregators);
        DateConf dateConf = generateDateConf(itemValueReq);
        queryTagReq.setDateInfo(dateConf);

        SemanticQueryResp semanticQueryResp = queryService.queryByReq(queryTagReq, user);
        if (!CollectionUtils.isEmpty(semanticQueryResp.getResultList())) {
            Object total = semanticQueryResp.getResultList().get(0).get(tagValueAlias);
            if (Objects.nonNull(total)) {
                return Long.parseLong(total.toString());
            }
        }
        throw new RuntimeException("queryTagTotalCount error");
    }

    private void fillTagValueInfo(ItemValueResp itemValueResp, SemanticQueryResp semanticQueryResp, Long totalCount) {
        List<ValueDistribution> valueDistributionList = new ArrayList<>();
        List<Map<String, Object>> resultList = semanticQueryResp.getResultList();
        if (!CollectionUtils.isEmpty(resultList)) {
            resultList.stream().forEach(line -> {
                Object tagValue = line.get(itemValueResp.getBizName());
                Long tagValueCount = Long.parseLong(line.get(tagValueAlias).toString());
                valueDistributionList.add(ValueDistribution.builder()
                        .totalCount(totalCount)
                        .valueMap(tagValue)
                        .valueCount(tagValueCount)
                        .ratio(1.0 * tagValueCount / totalCount).build());
            });
        }
        itemValueResp.setValueDistributionList(valueDistributionList);
    }

    private QueryTagReq generateTagReq(TagResp tag, ItemValueReq itemValueReq) {
        QueryTagReq queryTagReq = new QueryTagReq();
        queryTagReq.addModelId(tag.getModelId());
        queryTagReq.setGroups(new ArrayList<>(Arrays.asList(tag.getBizName())));
        queryTagReq.setLimit(itemValueReq.getLimit());

        List<Aggregator> aggregators = new ArrayList<>();
        aggregators.add(new Aggregator(tag.getBizName(), AggOperatorEnum.COUNT, tagValueAlias));
        queryTagReq.setAggregators(aggregators);

        List<Order> orders = new ArrayList<>();
        orders.add(new Order(String.format("count(%s)", tag.getBizName()), DESC_UPPER));
        queryTagReq.setOrders(orders);

        DateConf dateConf = generateDateConf(itemValueReq);
        queryTagReq.setDateInfo(dateConf);

        return queryTagReq;
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
package com.tencent.supersonic.headless.query.parser.convert;

import com.tencent.supersonic.headless.api.model.enums.ModelSourceTypeEnum;
import com.tencent.supersonic.headless.api.model.response.ModelResp;
import com.tencent.supersonic.headless.api.query.request.MetricReq;
import com.tencent.supersonic.headless.api.query.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.query.request.QueryStructReq;
import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.parser.HeadlessConverter;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.query.utils.QueryStructUtils;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * correct the Query parameters when the model source type is zipper
 */
@Component("ZipperModelConverter")
@Slf4j
public class ZipperModelConverter implements HeadlessConverter {

    private final QueryStructUtils queryStructUtils;
    private final Catalog catalog;

    public ZipperModelConverter(QueryStructUtils queryStructUtils,
            Catalog catalog) {
        this.queryStructUtils = queryStructUtils;
        this.catalog = catalog;
    }

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.isNull(queryStatement.getQueryStructReq())) {
            return false;
        }
        QueryStructReq queryStructCmd = queryStatement.getQueryStructReq();
        List<ModelResp> modelRespList = catalog.getModelList(queryStructCmd.getModelIds());
        if (!CollectionUtils.isEmpty(modelRespList)) {
            // all data sources are zipper tables
            long zipperCnt = modelRespList.stream().filter(m -> ModelSourceTypeEnum.isZipper(m.getSourceType()))
                    .count();
            return modelRespList.size() == zipperCnt;
        }
        return false;
    }

    @Override
    public void converter(Catalog catalog, QueryStatement queryStatement) throws Exception {
        QueryStructReq queryStructCmd = queryStatement.getQueryStructReq();
        ParseSqlReq sqlCommend = queryStatement.getParseSqlReq();
        MetricReq metricCommand = queryStatement.getMetricReq();
        doSingleZipperSource(queryStructCmd, sqlCommend, metricCommand);
    }

    protected void doSingleZipperSource(QueryStructReq queryStructCmd, ParseSqlReq sqlCommend,
            MetricReq metricCommand) {
        // all data sources are zipper tables
        // request  time field rewrite to start_ end_
        if (!sqlCommend.getSql().isEmpty()) {
            String sqlNew = queryStructUtils.generateZipperWhere(queryStructCmd, sqlCommend);
            log.info("doSingleZipperSource before[{}] after[{}]", sqlCommend.getSql(), sqlNew);
            sqlCommend.setSql(sqlNew);
        } else {
            String where = queryStructUtils.generateZipperWhere(queryStructCmd);
            if (!where.isEmpty() && Objects.nonNull(metricCommand)) {
                log.info("doSingleZipperSource before[{}] after[{}]", metricCommand.getWhere(), where);
                metricCommand.setWhere(where);
            }
        }
    }
}

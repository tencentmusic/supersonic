package com.tencent.supersonic.headless.core.parser.convert;

import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.enums.ModelSourceType;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.core.parser.HeadlessConverter;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.QueryStructUtils;
import com.tencent.supersonic.headless.server.service.Catalog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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
            long zipperCnt = modelRespList.stream().filter(m -> ModelSourceType.isZipper(m.getSourceType()))
                    .count();
            return modelRespList.size() == zipperCnt;
        }
        return false;
    }

    @Override
    public void converter(Catalog catalog, QueryStatement queryStatement) throws Exception {
        QueryStructReq queryStructCmd = queryStatement.getQueryStructReq();
        ParseSqlReq sqlCommend = queryStatement.getParseSqlReq();
        MetricQueryReq metricCommand = queryStatement.getMetricReq();
        doSingleZipperSource(queryStructCmd, sqlCommend, metricCommand);
    }

    protected void doSingleZipperSource(QueryStructReq queryStructCmd, ParseSqlReq sqlCommend,
            MetricQueryReq metricCommand) {
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

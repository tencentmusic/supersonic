package com.tencent.supersonic.headless.server.web.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.FileUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import com.tencent.supersonic.headless.api.pojo.request.BatchDownloadReq;
import com.tencent.supersonic.headless.api.pojo.request.DownloadMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.utils.DataTransformUtils;
import com.tencent.supersonic.headless.server.pojo.DataDownload;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.web.service.DimensionService;
import com.tencent.supersonic.headless.server.web.service.DownloadService;
import com.tencent.supersonic.headless.server.web.service.MetricService;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
public class DownloadServiceImpl implements DownloadService {

    private static final String internMetricCol = "指标名称";

    private static final long downloadSize = 10000;

    private MetricService metricService;

    private DimensionService dimensionService;

    private SemanticLayerService queryService;

    public DownloadServiceImpl(MetricService metricService,
            DimensionService dimensionService, SemanticLayerService queryService) {
        this.metricService = metricService;
        this.dimensionService = dimensionService;
        this.queryService = queryService;
    }

    @Override
    public void downloadByStruct(DownloadMetricReq downloadMetricReq,
                                 User user, HttpServletResponse response) throws Exception {
        String fileName = String.format("%s_%s.xlsx", "supersonic", DateUtils.format(new Date(), DateUtils.FORMAT));
        File file = FileUtils.createTmpFile(fileName);
        try {
            QueryStructReq queryStructReq = metricService.convert(downloadMetricReq);
            SemanticQueryResp queryResult = queryService.queryByReq(queryStructReq.convert(true), user);
            DataDownload dataDownload = buildDataDownload(queryResult, queryStructReq, downloadMetricReq.isTransform());
            EasyExcel.write(file).sheet("Sheet1").head(dataDownload.getHeaders()).doWrite(dataDownload.getData());
        } catch (RuntimeException e) {
            EasyExcel.write(file).sheet("Sheet1").head(buildErrMessageHead())
                    .doWrite(buildErrMessageData(e.getMessage()));
            return;
        }
        downloadFile(response, file, fileName);
    }

    @Override
    public void batchDownload(BatchDownloadReq batchDownloadReq, User user,
            HttpServletResponse response) throws Exception {
        String fileName = String.format("%s_%s.xlsx", "supersonic", DateUtils.format(new Date(), DateUtils.FORMAT));
        File file = FileUtils.createTmpFile(fileName);
        List<Long> metricIds = batchDownloadReq.getMetricIds();
        if (CollectionUtils.isEmpty(metricIds)) {
            return;
        }
        batchDownload(batchDownloadReq, user, file);
        downloadFile(response, file, fileName);
    }

    public void batchDownload(BatchDownloadReq batchDownloadReq, User user, File file) throws Exception {
        List<Long> metricIds = batchDownloadReq.getMetricIds();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(metricIds);
        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
        Map<String, List<MetricResp>> metricMap = getMetricMap(metricResps);
        List<Long> dimensionIds = metricResps.stream()
                .map(metricResp -> metricService.getDrillDownDimension(metricResp.getId()))
                .flatMap(Collection::stream)
                .map(DrillDownDimension::getDimensionId).collect(Collectors.toList());
        metaFilter.setIds(dimensionIds);
        Map<Long, DimensionResp> dimensionRespMap = dimensionService.getDimensions(metaFilter)
                .stream().collect(Collectors.toMap(DimensionResp::getId, d -> d));
        ExcelWriter excelWriter = EasyExcel.write(file).build();
        int sheetCount = 1;
        for (List<MetricResp> metrics : metricMap.values()) {
            if (CollectionUtils.isEmpty(metrics)) {
                continue;
            }
            MetricResp metricResp = metrics.get(0);
            List<DimensionResp> dimensions = getMetricRelaDimensions(metricResp, dimensionRespMap);
            for (MetricResp metric : metrics) {
                try {
                    QueryStructReq queryStructReq = buildDownloadReq(dimensions, metric, batchDownloadReq);
                    QuerySqlReq querySqlReq = queryStructReq.convert();
                    querySqlReq.setNeedAuth(true);
                    SemanticQueryResp queryResult = queryService.queryByReq(querySqlReq, user);
                    DataDownload dataDownload = buildDataDownload(queryResult,
                            queryStructReq, batchDownloadReq.isTransform());
                    WriteSheet writeSheet = EasyExcel.writerSheet("Sheet" + sheetCount)
                            .head(dataDownload.getHeaders()).build();
                    excelWriter.write(dataDownload.getData(), writeSheet);
                } catch (RuntimeException e) {
                    EasyExcel.write(file).sheet("Sheet1").head(buildErrMessageHead())
                            .doWrite(buildErrMessageData(e.getMessage()));
                    return;
                }
            }
            sheetCount++;
        }
        excelWriter.finish();
    }

    private List<List<String>> buildErrMessageHead() {
        List<List<String>> headers = Lists.newArrayList();
        headers.add(Lists.newArrayList("异常提示"));
        return headers;
    }

    private List<List<String>> buildErrMessageData(String errMsg) {
        List<List<String>> data = Lists.newArrayList();
        data.add(Lists.newArrayList(errMsg));
        return data;
    }

    private List<List<String>> buildHeader(SemanticQueryResp semanticQueryResp) {
        List<List<String>> header = Lists.newArrayList();
        for (QueryColumn column : semanticQueryResp.getColumns()) {
            header.add(Lists.newArrayList(column.getName()));
        }
        return header;
    }

    private List<List<String>> buildHeader(List<QueryColumn> queryColumns, List<String> dateList) {
        List<List<String>> headers = Lists.newArrayList();
        for (QueryColumn queryColumn : queryColumns) {
            if (SemanticType.DATE.name().equals(queryColumn.getShowType())) {
                continue;
            }
            headers.add(Lists.newArrayList(queryColumn.getName()));
        }
        for (String date : dateList) {
            headers.add(Lists.newArrayList(date));
        }
        headers.add(Lists.newArrayList(internMetricCol));
        return headers;
    }

    private List<List<String>> buildData(SemanticQueryResp semanticQueryResp) {
        List<List<String>> data = new ArrayList<>();
        for (Map<String, Object> row : semanticQueryResp.getResultList()) {
            List<String> rowData = new ArrayList<>();
            for (QueryColumn column : semanticQueryResp.getColumns()) {
                rowData.add(String.valueOf(row.get(column.getNameEn())));
            }
            data.add(rowData);
        }
        return data;
    }

    private List<List<String>> buildData(List<List<String>> headers, Map<String, String> nameMap,
            List<Map<String, Object>> dataTransformed, String metricName) {
        List<List<String>> data = Lists.newArrayList();
        for (Map<String, Object> map : dataTransformed) {
            List<String> row = Lists.newArrayList();
            for (List<String> header : headers) {
                String head = header.get(0);
                if (internMetricCol.equals(head)) {
                    continue;
                }
                Object object = map.getOrDefault(nameMap.getOrDefault(head, head), "");
                if (object == null) {
                    row.add("");
                } else {
                    row.add(String.valueOf(object));
                }
            }
            row.add(metricName);
            data.add(row);
        }
        return data;
    }

    private DataDownload buildDataDownload(SemanticQueryResp queryResult,
                                           QueryStructReq queryStructReq, boolean isTransform) {
        List<QueryColumn> metricColumns = queryResult.getMetricColumns();
        List<QueryColumn> dimensionColumns = queryResult.getDimensionColumns();
        if (isTransform && !CollectionUtils.isEmpty(metricColumns)) {
            QueryColumn metric = metricColumns.get(0);
            List<String> groups = queryStructReq.getGroups();
            List<Map<String, Object>> dataTransformed = DataTransformUtils.transform(queryResult.getResultList(),
                    metric.getNameEn(), groups, queryStructReq.getDateInfo());
            List<List<String>> headers = buildHeader(dimensionColumns, queryStructReq.getDateInfo().getDateList());
            List<List<String>> data = buildData(headers, getDimensionNameMap(dimensionColumns),
                    dataTransformed, metric.getName());
            return DataDownload.builder().headers(headers).data(data).build();
        } else {
            List<List<String>> data = buildData(queryResult);
            List<List<String>> header = buildHeader(queryResult);
            return DataDownload.builder().data(data).headers(header).build();
        }
    }

    private QueryStructReq buildDownloadReq(List<DimensionResp> dimensionResps, MetricResp metricResp,
                                               BatchDownloadReq batchDownloadReq) {
        DateConf dateConf = batchDownloadReq.getDateInfo();
        Set<Long> modelIds = dimensionResps.stream().map(DimensionResp::getModelId).collect(Collectors.toSet());
        modelIds.add(metricResp.getModelId());
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.setGroups(dimensionResps.stream()
                .map(DimensionResp::getBizName).collect(Collectors.toList()));
        queryStructReq.getGroups().add(0, getTimeDimension(dateConf));
        Aggregator aggregator = new Aggregator();
        aggregator.setColumn(metricResp.getBizName());
        queryStructReq.setAggregators(Lists.newArrayList(aggregator));
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setModelIds(modelIds);
        queryStructReq.setLimit(downloadSize);
        return queryStructReq;
    }

    private String getTimeDimension(DateConf dateConf) {
        if (Constants.MONTH.equals(dateConf.getPeriod())) {
            return TimeDimensionEnum.MONTH.getName();
        } else if (Constants.WEEK.equals(dateConf.getPeriod())) {
            return TimeDimensionEnum.WEEK.getName();
        } else {
            return TimeDimensionEnum.DAY.getName();
        }
    }

    private Map<String, List<MetricResp>> getMetricMap(List<MetricResp> metricResps) {
        for (MetricResp metricResp : metricResps) {
            List<DrillDownDimension> drillDownDimensions = metricService.getDrillDownDimension(metricResp.getId());
            RelateDimension relateDimension = RelateDimension.builder()
                    .drillDownDimensions(drillDownDimensions).build();
            metricResp.setRelateDimension(relateDimension);
        }
        return metricResps.stream().collect(Collectors.groupingBy(MetricResp::getRelaDimensionIdKey));
    }

    private Map<String, String> getDimensionNameMap(List<QueryColumn> queryColumns) {
        return queryColumns.stream().collect(Collectors.toMap(QueryColumn::getName, QueryColumn::getNameEn));
    }

    private List<DimensionResp> getMetricRelaDimensions(MetricResp metricResp,
            Map<Long, DimensionResp> dimensionRespMap) {
        if (metricResp.getRelateDimension() == null
                || CollectionUtils.isEmpty(metricResp.getRelateDimension().getDrillDownDimensions())) {
            return Lists.newArrayList();
        }
        return metricResp.getRelateDimension().getDrillDownDimensions()
                .stream().map(drillDownDimension -> dimensionRespMap.get(drillDownDimension.getDimensionId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void downloadFile(HttpServletResponse response, File file, String filename) {
        try {
            byte[] buffer = readFileToByteArray(file);
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            response.addHeader("Content-Length", "" + file.length());
            try (OutputStream outputStream = new BufferedOutputStream(response.getOutputStream())) {
                response.setContentType("application/octet-stream");
                outputStream.write(buffer);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("failed to download file", e);
        }
    }

    private byte[] readFileToByteArray(File file) throws IOException {
        try (InputStream fis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return buffer;
        }
    }

}



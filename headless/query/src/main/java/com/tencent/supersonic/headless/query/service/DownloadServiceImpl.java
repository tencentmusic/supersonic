package com.tencent.supersonic.headless.query.service;

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
import com.tencent.supersonic.headless.common.model.enums.SemanticTypeEnum;
import com.tencent.supersonic.headless.common.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.common.model.response.DimSchemaResp;
import com.tencent.supersonic.headless.common.model.response.DimensionResp;
import com.tencent.supersonic.headless.common.model.response.MetricResp;
import com.tencent.supersonic.headless.common.model.response.MetricSchemaResp;
import com.tencent.supersonic.headless.common.model.response.ModelSchemaResp;
import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.common.query.pojo.DataDownload;
import com.tencent.supersonic.headless.common.query.request.BatchDownloadReq;
import com.tencent.supersonic.headless.common.query.request.DownloadStructReq;
import com.tencent.supersonic.headless.model.domain.ModelService;
import com.tencent.supersonic.headless.query.utils.DataTransformUtils;
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

    private ModelService modelService;

    private QueryService queryService;

    public DownloadServiceImpl(ModelService modelService, QueryService queryService) {
        this.modelService = modelService;
        this.queryService = queryService;
    }

    @Override
    public void downloadByStruct(DownloadStructReq downloadStructReq,
                                 User user, HttpServletResponse response) throws Exception {
        String fileName = String.format("%s_%s.xlsx", "supersonic", DateUtils.format(new Date(), DateUtils.FORMAT));
        File file = FileUtils.createTmpFile(fileName);
        try {
            QueryResultWithSchemaResp queryResult = queryService.queryByStructWithAuth(downloadStructReq, user);
            DataDownload dataDownload = buildDataDownload(queryResult, downloadStructReq);
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
        List<ModelSchemaResp> modelSchemaRespList = modelService.fetchModelSchema(new ModelSchemaFilterReq());
        Map<String, List<MetricSchemaResp>> metricSchemaMap = getMetricSchemaMap(modelSchemaRespList, metricIds);
        Map<Long, DimSchemaResp> dimensionRespMap = getDimensionMap(modelSchemaRespList);
        ExcelWriter excelWriter = EasyExcel.write(file).build();
        int sheetCount = 1;
        for (List<MetricSchemaResp> metrics : metricSchemaMap.values()) {
            if (CollectionUtils.isEmpty(metrics)) {
                continue;
            }
            MetricSchemaResp metricSchemaResp = metrics.get(0);
            List<DimSchemaResp> dimensions = getMetricRelaDimensions(metricSchemaResp, dimensionRespMap);
            for (MetricSchemaResp metric : metrics) {
                try {
                    DownloadStructReq downloadStructReq = buildDownloadStructReq(dimensions, metric, batchDownloadReq);
                    QueryResultWithSchemaResp queryResult = queryService.queryByStructWithAuth(downloadStructReq, user);
                    DataDownload dataDownload = buildDataDownload(queryResult, downloadStructReq);
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

    private List<List<String>> buildHeader(QueryResultWithSchemaResp queryResultWithSchemaResp) {
        List<List<String>> header = Lists.newArrayList();
        for (QueryColumn column : queryResultWithSchemaResp.getColumns()) {
            header.add(Lists.newArrayList(column.getName()));
        }
        return header;
    }

    private List<List<String>> buildHeader(List<QueryColumn> queryColumns, List<String> dateList) {
        List<List<String>> headers = Lists.newArrayList();
        for (QueryColumn queryColumn : queryColumns) {
            if (SemanticTypeEnum.DATE.name().equals(queryColumn.getShowType())) {
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

    private List<List<String>> buildData(QueryResultWithSchemaResp queryResultWithSchemaResp) {
        List<List<String>> data = new ArrayList<>();
        for (Map<String, Object> row : queryResultWithSchemaResp.getResultList()) {
            List<String> rowData = new ArrayList<>();
            for (QueryColumn column : queryResultWithSchemaResp.getColumns()) {
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

    private DataDownload buildDataDownload(QueryResultWithSchemaResp queryResult, DownloadStructReq downloadStructReq) {
        List<QueryColumn> metricColumns = queryResult.getMetricColumns();
        List<QueryColumn> dimensionColumns = queryResult.getDimensionColumns();
        if (downloadStructReq.isTransform() && !CollectionUtils.isEmpty(metricColumns)) {
            QueryColumn metric = metricColumns.get(0);
            List<String> groups = downloadStructReq.getGroups();
            List<Map<String, Object>> dataTransformed = DataTransformUtils.transform(queryResult.getResultList(),
                    metric.getNameEn(), groups, downloadStructReq.getDateInfo());
            List<List<String>> headers = buildHeader(dimensionColumns, downloadStructReq.getDateInfo().getDateList());
            List<List<String>> data = buildData(headers, getDimensionNameMap(dimensionColumns),
                    dataTransformed, metric.getName());
            return DataDownload.builder().headers(headers).data(data).build();
        } else {
            List<List<String>> data = buildData(queryResult);
            List<List<String>> header = buildHeader(queryResult);
            return DataDownload.builder().data(data).headers(header).build();
        }
    }

    private DownloadStructReq buildDownloadStructReq(List<DimSchemaResp> dimensionResps, MetricResp metricResp,
                                                     BatchDownloadReq batchDownloadReq) {
        DateConf dateConf = batchDownloadReq.getDateInfo();
        Set<Long> modelIds = dimensionResps.stream().map(DimSchemaResp::getModelId).collect(Collectors.toSet());
        modelIds.add(metricResp.getModelId());
        DownloadStructReq downloadStructReq = new DownloadStructReq();
        downloadStructReq.setGroups(dimensionResps.stream()
                .map(DimSchemaResp::getBizName).collect(Collectors.toList()));
        downloadStructReq.getGroups().add(0, getTimeDimension(dateConf));
        Aggregator aggregator = new Aggregator();
        aggregator.setColumn(metricResp.getBizName());
        downloadStructReq.setAggregators(Lists.newArrayList(aggregator));
        downloadStructReq.setDateInfo(dateConf);
        downloadStructReq.setModelIds(modelIds);
        downloadStructReq.setLimit(downloadSize);
        downloadStructReq.setIsTransform(batchDownloadReq.isTransform());
        return downloadStructReq;
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

    private Map<String, List<MetricSchemaResp>> getMetricSchemaMap(List<ModelSchemaResp> modelSchemaRespList,
                                                                    List<Long> metricIds) {
        return modelSchemaRespList.stream().flatMap(modelSchemaResp
                        -> modelSchemaResp.getMetrics().stream())
                .filter(metricSchemaResp -> metricIds.contains(metricSchemaResp.getId()))
                .collect(Collectors.groupingBy(MetricSchemaResp::getRelaDimensionIdKey));
    }

    private Map<Long, DimSchemaResp> getDimensionMap(List<ModelSchemaResp> modelSchemaRespList) {
        return modelSchemaRespList.stream().flatMap(modelSchemaResp
                        -> modelSchemaResp.getDimensions().stream())
                .collect(Collectors.toMap(DimensionResp::getId, dimensionResp -> dimensionResp));
    }

    private Map<String, String> getDimensionNameMap(List<QueryColumn> queryColumns) {
        return queryColumns.stream().collect(Collectors.toMap(QueryColumn::getName, QueryColumn::getNameEn));
    }

    private List<DimSchemaResp> getMetricRelaDimensions(MetricSchemaResp metricSchemaResp,
                                                        Map<Long, DimSchemaResp> dimensionRespMap) {
        if (metricSchemaResp.getRelateDimension() == null
                || CollectionUtils.isEmpty(metricSchemaResp.getRelateDimension().getDrillDownDimensions())) {
            return Lists.newArrayList();
        }
        return metricSchemaResp.getRelateDimension().getDrillDownDimensions()
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



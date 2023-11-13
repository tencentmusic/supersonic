package com.tencent.supersonic.semantic.query.service;

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
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.pojo.DataDownload;
import com.tencent.supersonic.semantic.api.query.request.BatchDownloadReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.query.utils.DataTransformUtils;
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
import java.util.stream.Collectors;


@Slf4j
@Service
public class DownloadServiceImpl implements DownloadService {

    private ModelService modelService;

    private QueryService queryService;

    public DownloadServiceImpl(ModelService modelService, QueryService queryService) {
        this.modelService = modelService;
        this.queryService = queryService;
    }

    @Override
    public void downloadByStruct(QueryStructReq queryStructReq,
                                 User user, HttpServletResponse response) throws Exception {
        QueryResultWithSchemaResp queryResultWithSchemaResp = queryService.queryByStruct(queryStructReq, user);
        List<List<String>> data = new ArrayList<>();
        List<List<String>> header = org.assertj.core.util.Lists.newArrayList();
        for (QueryColumn column : queryResultWithSchemaResp.getColumns()) {
            header.add(org.assertj.core.util.Lists.newArrayList(column.getName()));
        }
        for (Map<String, Object> row : queryResultWithSchemaResp.getResultList()) {
            List<String> rowData = new ArrayList<>();
            for (QueryColumn column : queryResultWithSchemaResp.getColumns()) {
                rowData.add(String.valueOf(row.get(column.getNameEn())));
            }
            data.add(rowData);
        }
        String fileName = String.format("%s_%s.xlsx", "supersonic", DateUtils.format(new Date(), DateUtils.FORMAT));
        File file = FileUtils.createTmpFile(fileName);
        EasyExcel.write(file).sheet("Sheet1").head(header).doWrite(data);
        downloadFile(response, file, fileName);
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
                DataDownload downloadData = getSingleMetricDownloadData(metric, dimensions,
                        batchDownloadReq.getDateInfo(), user);
                WriteSheet writeSheet = EasyExcel.writerSheet("Sheet" + sheetCount)
                        .head(downloadData.getHeaders()).build();
                excelWriter.write(downloadData.getData(), writeSheet);
            }
            sheetCount++;
        }
        excelWriter.finish();
    }

    public DataDownload getSingleMetricDownloadData(MetricSchemaResp metricSchemaResp, List<DimSchemaResp> dimensions,
                                                    DateConf dateConf, User user) throws Exception {
        QueryResultWithSchemaResp queryResult = getQueryResult(dimensions, metricSchemaResp, dateConf, user);
        List<String> groups = dimensions.stream().map(DimensionResp::getBizName).collect(Collectors.toList());
        List<String> dateList = getDateList(dateConf);
        List<Map<String, Object>> dataTransformed = DataTransformUtils.transform(queryResult.getResultList(), dateList,
                metricSchemaResp.getBizName(), groups);
        List<List<String>> headers = buildHeader(dimensions, dateList);
        List<List<String>> data = buildData(headers, getDimensionNameMap(dimensions),
                dataTransformed, metricSchemaResp);
        return DataDownload.builder().headers(headers).data(data).build();
    }

    private List<List<String>> buildHeader(List<DimSchemaResp> dimensionResps, List<String> dateList) {
        List<List<String>> headers = Lists.newArrayList();
        for (DimensionResp dimensionResp : dimensionResps) {
            headers.add(Lists.newArrayList(dimensionResp.getName()));
        }
        for (String date : dateList) {
            headers.add(Lists.newArrayList(date));
        }
        headers.add(Lists.newArrayList("指标名"));
        return headers;
    }

    private List<List<String>> buildData(List<List<String>> headers, Map<String, String> nameMap,
                                         List<Map<String, Object>> dataTransformed, MetricSchemaResp metricSchemaResp) {
        List<List<String>> data = Lists.newArrayList();
        for (Map<String, Object> map : dataTransformed) {
            List<String> row = Lists.newArrayList();
            for (List<String> header : headers) {
                String head = header.get(0);
                if ("指标名".equals(head)) {
                    continue;
                }
                row.add(map.getOrDefault(nameMap.getOrDefault(head, head), "").toString());
            }
            row.add(metricSchemaResp.getName());
            data.add(row);
        }
        return data;
    }

    private List<String> getDateList(DateConf dateConf) {
        String startDateStr = dateConf.getStartDate();
        String endDateStr = dateConf.getEndDate();
        return DateUtils.getDateList(startDateStr, endDateStr, dateConf.getPeriod());
    }

    private QueryResultWithSchemaResp getQueryResult(List<DimSchemaResp> dimensionResps, MetricResp metricResp,
                                                     DateConf dateConf, User user) throws Exception {
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.setGroups(dimensionResps.stream().map(DimSchemaResp::getBizName).collect(Collectors.toList()));
        queryStructReq.getGroups().add(0, getTimeDimension(dateConf));
        Aggregator aggregator = new Aggregator();
        aggregator.setColumn(metricResp.getBizName());
        queryStructReq.setAggregators(Lists.newArrayList(aggregator));
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setModelId(metricResp.getModelId());
        return queryService.queryByStruct(queryStructReq, user);
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

    private Map<String, String> getDimensionNameMap(List<DimSchemaResp> dimensionResps) {
        return dimensionResps.stream().collect(Collectors.toMap(DimensionResp::getName, SchemaItem::getBizName));
    }

    private List<DimSchemaResp> getMetricRelaDimensions(MetricSchemaResp metricSchemaResp,
                                                        Map<Long, DimSchemaResp> dimensionRespMap) {
        return metricSchemaResp.getRelateDimension().getDrillDownDimensions()
                .stream().map(drillDownDimension -> dimensionRespMap.get(drillDownDimension.getDimensionId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}



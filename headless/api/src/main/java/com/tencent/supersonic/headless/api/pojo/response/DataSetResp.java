package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.DataSetDetail;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DataSetResp extends SchemaItem {

    private Long domainId;

    private DataSetDetail dataSetDetail;

    private String alias;

    private String filterSql;

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();

    private QueryConfig queryConfig;

    public List<Long> getAllMetrics() {
        return getDataSetModelConfigs().stream().map(DataSetModelConfig::getMetrics)
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<Long> getAllDimensions() {
        return getDataSetModelConfigs().stream().map(DataSetModelConfig::getDimensions)
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<Long> getAllModels() {
        return getDataSetModelConfigs().stream().map(DataSetModelConfig::getId)
                .collect(Collectors.toList());
    }

    public List<Long> getAllTags() {
        return getDataSetModelConfigs().stream().map(DataSetModelConfig::getTagIds)
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<Long> getAllIncludeAllModels() {
        return getDataSetModelConfigs().stream().filter(DataSetModelConfig::isIncludesAll)
                .map(DataSetModelConfig::getId)
                .collect(Collectors.toList());
    }

    private List<DataSetModelConfig> getDataSetModelConfigs() {
        if (dataSetDetail == null || CollectionUtils.isEmpty(dataSetDetail.getDataSetModelConfigs())) {
            return Lists.newArrayList();
        }
        return dataSetDetail.getDataSetModelConfigs();
    }

}

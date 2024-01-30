package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.ViewDetail;
import com.tencent.supersonic.headless.api.pojo.ViewModelConfig;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ViewResp extends SchemaItem {

    private Long domainId;

    private ViewDetail viewDetail;

    private String alias;

    private String filterSql;

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();

    private QueryConfig queryConfig;

    public List<Long> getAllMetrics() {
        return getViewModelConfigs().stream().map(ViewModelConfig::getMetrics)
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<Long> getAllDimensions() {
        return getViewModelConfigs().stream().map(ViewModelConfig::getDimensions)
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<Long> getAllModels() {
        return getViewModelConfigs().stream().map(ViewModelConfig::getId)
                .collect(Collectors.toList());
    }

    public List<Long> getAllIncludeAllModels() {
        return getViewModelConfigs().stream().filter(ViewModelConfig::isIncludesAll)
                .map(ViewModelConfig::getId)
                .collect(Collectors.toList());
    }

    private List<ViewModelConfig> getViewModelConfigs() {
        if (viewDetail == null || CollectionUtils.isEmpty(viewDetail.getViewModelConfigs())) {
            return Lists.newArrayList();
        }
        return viewDetail.getViewModelConfigs();
    }

}

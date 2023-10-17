package com.tencent.supersonic.semantic.api.model.response;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.RelateDimension;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Data
@ToString(callSuper = true)
public class MetricResp extends SchemaItem {

    private Long modelId;

    private Long domainId;

    private String modelName;

    //ATOMIC DERIVED
    private String type;

    private MetricTypeParams typeParams;

    private String dataFormatType;

    private DataFormat dataFormat;

    private String alias;

    private List<String> tags;

    private RelateDimension relateDimension;

    private boolean hasAdminRes = false;

    private String defaultAgg;

    public void setTag(String tag) {
        if (StringUtils.isBlank(tag)) {
            tags = Lists.newArrayList();
        } else {
            tags = Arrays.asList(tag.split(","));
        }
    }

    public Set<Long> getNecessaryDimensionIds() {
        if (relateDimension == null || CollectionUtils.isEmpty(relateDimension.getDrillDownDimensions())) {
            return Sets.newHashSet();
        }
        return relateDimension.getDrillDownDimensions().stream().filter(DrillDownDimension::isNecessary)
               .map(DrillDownDimension::getDimensionId).collect(Collectors.toSet());
    }
}

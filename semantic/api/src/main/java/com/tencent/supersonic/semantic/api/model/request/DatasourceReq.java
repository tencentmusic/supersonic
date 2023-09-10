package com.tencent.supersonic.semantic.api.model.request;


import com.google.common.collect.Lists;
import com.tencent.supersonic.semantic.api.model.enums.DimensionTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.CollectionUtils;


@Data
public class DatasourceReq extends SchemaItem {

    private Long databaseId;

    private String queryType;

    private String sqlQuery;

    private String tableQuery;

    private Long modelId;

    private List<Identify> identifiers;

    private List<Dim> dimensions;

    private List<Measure> measures;



    public List<Dim> getTimeDimension() {
        if (CollectionUtils.isEmpty(dimensions)) {
            return Lists.newArrayList();
        }
        return dimensions.stream()
                .filter(dim -> DimensionTypeEnum.time.name().equalsIgnoreCase(dim.getType()))
                .collect(Collectors.toList());
    }

}

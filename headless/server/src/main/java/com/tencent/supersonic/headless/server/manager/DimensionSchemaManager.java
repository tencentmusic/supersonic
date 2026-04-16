package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.schema.DimensionSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** manager to handle the dimension */
@Slf4j
@Service
public class DimensionSchemaManager {

    public static List<DimensionSchema> convertToSchemas(List<DimensionResp> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return new ArrayList<>();
        }
        return dimensions.stream()
                .filter(dimension -> !dimension.getType().name()
                        .equalsIgnoreCase(IdentifyType.primary.name()))
                .map(DimensionSchemaManager::convertToSchema).collect(Collectors.toList());
    }

    public static DimensionSchema convertToSchema(DimensionResp dimension) {
        DimensionSchema dimensionSchema = new DimensionSchema();
        BeanUtils.copyProperties(dimension, dimensionSchema);
        dimensionSchema.setName(dimension.getBizName());
        dimensionSchema.setOwners(dimension.getCreatedBy());
        return dimensionSchema;
    }
}

package com.tencent.supersonic.semantic.materialization.domain.utils;


import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementFrequencyEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementTypeEnum;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationElementResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Component
public class MaterializationZipperUtils implements MaterializationUtils {

    private String split = "_";
    private String createPatter = "CREATE TABLE IF NOT EXISTS `#{tableName}` (\n"
            + "  `end_sys_imp_date` date NOT NULL COMMENT '日期',\n"
            + "  `id` int(11) NOT NULL COMMENT 'id',\n"
            + "  `start_sys_imp_date` date NULL,\n"
            + "   #{columnInfo}\n"
            + "   ) ENGINE=OLAP\n"
            + "UNIQUE KEY(`end_sys_imp_date`, `id`)\n"
            + "COMMENT 'OLAP'\n"
            + "PARTITION BY RANGE(`end_sys_imp_date`)\n"
            + "(PARTITION p99991230 VALUES [('9999-12-30'), ('9999-12-31')))\n"
            + "DISTRIBUTED BY HASH(`id`) BUCKETS 9\n"
            + "PROPERTIES (\n"
            + "\"replication_allocation\" = \"tag.location.default: 1\",\n"
            + "\"is_being_synced\" = \"false\",\n"
            + "\"colocate_with\" = \"#{colocateGroup}\",\n"
            + "\"storage_format\" = \"V2\",\n"
            + "\"enable_unique_key_merge_on_write\" = \"true\",\n"
            + "\"light_schema_change\" = \"true\",\n"
            + "\"disable_auto_compaction\" = \"false\",\n"
            + "\"enable_single_replica_compaction\" = \"false\"\n"
            + ");";


    private final DimensionService dimensionService;

    public MaterializationZipperUtils(DimensionService dimensionService) {
        this.dimensionService = dimensionService;
    }

    @Override
    public String generateCreateSql(MaterializationResp materializationResp) {
        List<MaterializationElementResp> materializationElementRespList = materializationResp
                .getMaterializationElementRespList();
        if (CollectionUtils.isEmpty(materializationElementRespList)) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(",");
        Map<Long, DimensionResp> dimIdAndDim = dimensionService.getDimensions(materializationResp.getModelId())
                .stream().collect(Collectors.toMap(DimensionResp::getId, value -> value, (v1, v2) -> v2));
        materializationElementRespList.stream()
                .filter(element -> TypeEnums.DIMENSION.equals(element.getType()) && ElementFrequencyEnum.LOW.equals(
                        element.getFrequency()))
                .forEach(element -> {
                            String type = "varchar(10000)";
                            if (Objects.nonNull(element.getElementType()) && ElementTypeEnum.DATE.equals(
                                    element.getElementType())) {
                                type = "date";
                            }
                            if (dimIdAndDim.containsKey(element.getId())) {
                                if (DataTypeEnums.ARRAY.equals(dimIdAndDim.get(element.getId()).getDataType())) {
                                    type = "array<varchar(10000)>";
                                }

                            }
                            String description = element.getDescription().replace("'", "")
                                    .replace("\"", "");
                            joiner.add(
                                    String.format(" `%s` %s COMMENT '%s'",
                                            element.getBizName(), type, description));
                }
                );

        if (Strings.isEmpty(joiner.toString())) {
            return "";
        }

        String colocateGroup = generateColocateGroup(materializationResp);

        return createPatter.replace("#{tableName}", materializationResp.getDestinationTable())
                .replace("#{columnInfo}", joiner.toString())
                .replace("#{colocateGroup}", colocateGroup);
    }

    private String generateColocateGroup(MaterializationResp materializationResp) {
        String name = materializationResp.getName();
        if (Strings.isNotEmpty(name) && name.contains(split)) {
            return name.split(split)[0];
        }
        return "";
    }
}
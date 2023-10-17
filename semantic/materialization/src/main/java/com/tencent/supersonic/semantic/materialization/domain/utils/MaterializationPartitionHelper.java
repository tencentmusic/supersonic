package com.tencent.supersonic.semantic.materialization.domain.utils;


import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementFrequencyEnum;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationElementResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.StringJoiner;

public class MaterializationPartitionHelper implements MaterializationUtils {
    private String split = "_";
    private String createPatter = "CREATE TABLE `#{tableName}` (\n"
            + "  `dayno` date NOT NULL COMMENT '日期',\n"
            + "  `id` int(11) NOT NULL COMMENT 'id',\n"
            + "   #{columnInfo}\n"
            + "  ) ENGINE=OLAP\n"
            + "UNIQUE KEY(`dayno`, `id`)\n"
            + "COMMENT 'OLAP'\n"
            + "PARTITION BY RANGE(`dayno`)\n"
            + "(PARTITION p20230820 VALUES [('2023-08-20'), ('2023-08-21')),\n"
            + "PARTITION p20230821 VALUES [('2023-08-21'), ('2023-08-22')),\n"
            + "PARTITION p20230827 VALUES [('2023-08-27'), ('2023-08-28')))\n"
            + "DISTRIBUTED BY HASH(`id`) BUCKETS 36\n"
            + "PROPERTIES (\n"
            + "\"replication_allocation\" = \"tag.location.default: 1\",\n"
            + "\"is_being_synced\" = \"false\",\n"
            + "\"colocate_with\" = \"#{colocateGroup}\",\n"
            + "\"storage_format\" = \"V2\",\n"
            + "\"enable_unique_key_merge_on_write\" = \"true\",\n"
            + "\"light_schema_change\" = \"true\",\n"
            + "\"disable_auto_compaction\" = \"false\",\n"
            + "\"enable_single_replica_compaction\" = \"false\"\n"
            + ")";

    @Override
    public String generateCreateSql(MaterializationResp materializationResp) {
        List<MaterializationElementResp> materializationElementRespList = materializationResp
                .getMaterializationElementRespList();
        if (CollectionUtils.isEmpty(materializationElementRespList)) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(",");
        materializationElementRespList.stream()
                .filter(element -> !ElementFrequencyEnum.LOW.equals(element.getFrequency()))
                .forEach(element -> {
                            String type = "double";
                            if (TypeEnums.DIMENSION.equals(element.getType())) {
                                type = "varchar(10000)";
                            }
                            String description = element.getDescription().replace("'", "").replace("\"", "");
                            joiner.add(
                                    String.format(" %s %s COMMENT '%s'", element.getBizName(), type, description));
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
package com.tencent.supersonic.semantic.api.model.pojo;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Data
@ToString(callSuper = true)
public class SchemaItem extends RecordInfo {

    private static String aliasSplit = ",";
    private Long id;

    private String name;

    private String bizName;

    private String description;

    private Integer status = StatusEnum.ONLINE.getCode();

    private TypeEnums typeEnum;

    private Integer sensitiveLevel = SensitiveLevelEnum.LOW.getCode();


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SchemaItem that = (SchemaItem) o;
        return Objects.equal(id, that.id) && Objects.equal(name, that.name)
                && Objects.equal(bizName, that.bizName) && Objects.equal(
                description, that.description) && Objects.equal(status, that.status)
                && typeEnum == that.typeEnum && Objects.equal(sensitiveLevel, that.sensitiveLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), id, name, bizName, description, status, typeEnum, sensitiveLevel);
    }

    public static List<String> getAliasList(String alias) {
        if (StringUtils.isEmpty(alias)) {
            return new ArrayList<>();
        }
        return Arrays.asList(alias.split(aliasSplit));
    }
}

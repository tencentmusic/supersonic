package com.tencent.supersonic.headless.api.pojo;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@ToString(callSuper = true)
public class SchemaItem extends RecordInfo {

    private static String aliasSplit = ",";

    protected Long id;

    protected String name;

    protected String bizName;

    protected String description;

    protected Integer status;

    protected TypeEnums typeEnum;

    protected Integer sensitiveLevel = SensitiveLevelEnum.LOW.getCode();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchemaItem that = (SchemaItem) o;
        return Objects.equal(id, that.id) && Objects.equal(name, that.name)
                && Objects.equal(bizName, that.bizName) && typeEnum == that.typeEnum;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, bizName, typeEnum);
    }

    public static List<String> getAliasList(String alias) {
        if (StringUtils.isEmpty(alias)) {
            return new ArrayList<>();
        }
        return Arrays.asList(alias.split(aliasSplit));
    }
}

package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString(callSuper = true)
public class QueryFilter implements Serializable {

    private String bizName;

    private String name;

    private FilterOperatorEnum operator = FilterOperatorEnum.EQUALS;

    private Object value;

    private Long elementID;

    private String function;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryFilter that = (QueryFilter) o;
        return Objects.equal(bizName, that.bizName) && Objects.equal(name, that.name)
                && operator == that.operator && Objects.equal(value, that.value)
                && Objects.equal(function, that.function);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bizName, name, operator, value, function);
    }
}

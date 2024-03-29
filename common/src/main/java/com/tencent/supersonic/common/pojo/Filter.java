package com.tencent.supersonic.common.pojo;


import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Filter {

    private Relation relation = Relation.FILTER;
    private Long id;
    private String bizName;
    private String name;
    private FilterOperatorEnum operator;
    private Object value;
    private List<Filter> children;

    public Filter(String bizName, FilterOperatorEnum operator, Object value) {
        this.bizName = bizName;
        this.operator = operator;
        this.value = value;
    }

    public Filter(Relation relation, String bizName, FilterOperatorEnum operator, Object value) {
        this.relation = relation;
        this.bizName = bizName;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"relation\":")
                .append(relation);
        sb.append(",\"bizName\":\"")
                .append(bizName).append('\"');
        sb.append(",\"name\":\"")
                .append(name).append('\"');
        sb.append(",\"operator\":")
                .append(operator);
        sb.append(",\"value\":")
                .append(value);
        sb.append(",\"children\":")
                .append(children);
        sb.append('}');
        return sb.toString();
    }

    public enum Relation {
        FILTER, OR, AND
    }
}
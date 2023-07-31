package com.tencent.supersonic.common.pojo;

import static com.tencent.supersonic.common.pojo.Constants.ASC_UPPER;

import com.google.common.base.Objects;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class Order {

    @NotBlank(message = "Invalid order column")
    private String column;

    private String direction = ASC_UPPER;

    public Order(String column, String direction) {
        this.column = column;
        this.direction = direction;
    }

    public Order() {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"column\":\"")
                .append(column).append('\"');
        sb.append(",\"direction\":\"")
                .append(direction).append('\"');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equal(column, order.column) && Objects.equal(direction,
                order.direction);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(column, direction);
    }
}

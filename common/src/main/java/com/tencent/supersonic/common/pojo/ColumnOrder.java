package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnOrder implements Serializable {

    private String col;
    private String order;

    public static ColumnOrder buildAsc(String col) {
        ColumnOrder columnOrder = new ColumnOrder();
        columnOrder.setOrder("ASC");
        columnOrder.setCol(col);
        return columnOrder;
    }

    public static ColumnOrder buildDesc(String col) {
        ColumnOrder columnOrder = new ColumnOrder();
        columnOrder.setOrder("DESC");
        columnOrder.setCol(col);
        return columnOrder;
    }
}

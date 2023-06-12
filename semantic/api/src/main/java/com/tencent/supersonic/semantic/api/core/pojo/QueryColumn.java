package com.tencent.supersonic.semantic.api.core.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryColumn {

    private String name;
    private String type;
    private String nameEn;
    private String showType;
    private Boolean authorized = true;

    public void setType(String type) {
        this.type = type == null ? null : type;
    }

    public QueryColumn(String nameEn, String type) {
        this.type = type;
        this.nameEn = nameEn;
    }
}

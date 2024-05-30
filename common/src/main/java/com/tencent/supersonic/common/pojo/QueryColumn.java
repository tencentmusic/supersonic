package com.tencent.supersonic.common.pojo;

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
    private String dataFormatType;
    private DataFormat dataFormat;
    private String comment;

    public QueryColumn(String nameEn, String type) {
        this.type = type;
        this.nameEn = nameEn;
    }

    public QueryColumn(String name, String type, String nameEn) {
        this.name = name;
        this.type = type;
        this.nameEn = nameEn;
        this.showType = "CATEGORY";
    }

    public void setType(String type) {
        this.type = type == null ? null : type;
    }
}

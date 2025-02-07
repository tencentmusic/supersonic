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
    private String bizName;
    private String nameEn;
    private String showType;
    private Boolean authorized = true;
    private String dataFormatType;
    private DataFormat dataFormat;
    private String comment;
    private Long modelId;

    public QueryColumn(String bizName, String type) {
        this.type = type;
        this.bizName = bizName;
        this.nameEn = bizName;
        this.name = bizName;
    }

    public QueryColumn(String name, String type, String bizName) {
        this.name = name;
        this.type = type;
        this.bizName = bizName;
        this.nameEn = bizName;
        this.showType = "CATEGORY";
    }

    public void setType(String type) {
        this.type = type == null ? null : type;
    }

    public void setBizName(String bizName) {
        this.bizName = bizName;
        this.nameEn = bizName;
    }
}

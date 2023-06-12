package com.tencent.supersonic.common.util;

import java.util.Date;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RecordInfo {


    private String createdBy;

    private String updatedBy;

    private Date createdAt;

    private Date updatedAt;


    public RecordInfo createdBy(String userName) {
        this.createdBy = userName;
        this.createdAt = new Date();
        this.updatedBy = userName;
        this.updatedAt = new Date();
        return this;
    }

    public RecordInfo updatedBy(String userName) {
        this.updatedBy = userName;
        this.updatedAt = new Date();
        return this;
    }

}

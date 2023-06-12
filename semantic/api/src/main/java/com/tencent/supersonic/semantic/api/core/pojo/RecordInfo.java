package com.tencent.supersonic.semantic.api.core.pojo;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
@AllArgsConstructor
public class RecordInfo {


    private String createdBy;

    private String updatedBy;

    private Date createdAt;

    private Date updatedAt;

}

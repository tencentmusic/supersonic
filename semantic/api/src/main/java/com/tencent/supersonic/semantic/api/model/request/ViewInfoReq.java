package com.tencent.supersonic.semantic.api.model.request;

import java.util.Date;
import lombok.Data;

@Data
public class ViewInfoReq {

    private Long id;

    private Long domainId;

    private String type;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String config;

}
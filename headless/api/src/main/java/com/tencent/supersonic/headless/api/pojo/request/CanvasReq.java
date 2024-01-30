package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

import java.util.Date;

@Data
public class CanvasReq {

    private Long id;

    private Long domainId;

    private String type;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String config;

}
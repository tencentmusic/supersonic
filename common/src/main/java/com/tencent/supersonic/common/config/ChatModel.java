package com.tencent.supersonic.common.config;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import lombok.Data;

import java.util.Date;

@Data
public class ChatModel {
    private Integer id;

    private String name;

    private String description;

    private ChatModelConfig Config;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String admin;

    private String viewer;
}

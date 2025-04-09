package com.tencent.supersonic.common.config;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import lombok.Data;

import java.util.Date;
import java.util.List;

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

    private List<String> viewers = Lists.newArrayList();

    private Integer isOpen = 0;

    public boolean isPublic() {
        return isOpen != null && isOpen == 1;
    }
}

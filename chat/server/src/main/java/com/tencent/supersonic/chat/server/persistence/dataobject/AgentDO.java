package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tencent.supersonic.common.config.VisualConfig;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_agent")
public class AgentDO {
    /**
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     */
    private String name;

    /**
     */
    private String description;

    /**
     * 0 offline, 1 online
     */
    private Integer status;

    /**
     */
    private String examples;

    /**
     */
    private String config;

    /**
     */
    private String createdBy;

    /**
     */
    private Date createdAt;

    /**
     */
    private String updatedBy;

    /**
     */
    private Date updatedAt;

    /**
     */
    private Integer enableSearch;

    private String llmConfig;

    private String multiTurnConfig;

    private String visualConfig;

}

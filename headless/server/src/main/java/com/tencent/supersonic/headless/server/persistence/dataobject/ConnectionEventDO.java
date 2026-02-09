package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_connection_event")
public class ConnectionEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long connectionId;
    private String eventType;
    private Date eventTime;
    private String eventData;
    private Long userId;
    private String userName;
    private Long jobId;
    private Long tenantId;
}

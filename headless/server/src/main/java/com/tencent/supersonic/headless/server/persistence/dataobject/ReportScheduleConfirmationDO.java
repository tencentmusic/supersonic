package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_report_schedule_confirmation")
public class ReportScheduleConfirmationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String confirmToken;
    private Long userId;
    private Integer chatId;
    private String actionType;
    private Long sourceQueryId;
    private Integer sourceParseId;
    private Long sourceDataSetId;
    private String payloadJson;
    private String status;
    private Date expireAt;
    private Date createdAt;
    private Long tenantId;
}

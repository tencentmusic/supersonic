package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_report_subscription")
public class ReportSubscriptionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long datasetId;
    private Long tenantId;
    private Date createdAt;
}

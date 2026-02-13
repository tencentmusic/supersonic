package com.tencent.supersonic.feishu.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_feishu_query_session")
public class FeishuQuerySessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String feishuOpenId;
    private String feishuMessageId;
    private String queryText;
    private Long queryResultId;
    private String sqlText;
    private Integer rowCount;
    private String status;
    private Long datasetId;
    private Integer agentId;
    private String errorMessage;
    private Date createdAt;
}

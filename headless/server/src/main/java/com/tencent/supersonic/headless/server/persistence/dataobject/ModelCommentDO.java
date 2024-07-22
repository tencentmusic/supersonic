package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("s2_model_comment")
public class ModelCommentDO {
    private Long id;
    private Long modelId;
    private String fieldName;
    private String fieldType;
    private String comment;
    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}

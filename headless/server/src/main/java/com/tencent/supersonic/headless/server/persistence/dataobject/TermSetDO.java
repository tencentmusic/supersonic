package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("s2_term_set")
public class TermSetDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long domainId;

    private String terms;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

}

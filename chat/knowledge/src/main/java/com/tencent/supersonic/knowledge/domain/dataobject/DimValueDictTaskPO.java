package com.tencent.supersonic.knowledge.domain.dataobject;

import java.util.Date;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;

@Data
@ToString
public class DimValueDictTaskPO {

    private Long id;

    private String name;

    private String description;

    private String command;

    private String commandMd5;

    private Integer status;

    private String createdBy;

    private Date createdAt;

    private Double progress;

    private Long elapsedMs;

    public String getCommandMd5() {
        return DigestUtils.md5Hex(command);
    }
}
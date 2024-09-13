package com.tencent.supersonic.chat.server.persistence.dataobject;

import lombok.Data;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;

@Data
@ToString
public class DictTaskDO {

    private Long id;

    private String name;

    private String description;

    private String command;

    private String commandMd5;

    private String dimIds;

    private Integer status;

    private String createdBy;

    private Date createdAt;

    private Double progress;

    private Long elapsedMs;

    public String getCommandMd5() {
        return DigestUtils.md5Hex(command);
    }
}

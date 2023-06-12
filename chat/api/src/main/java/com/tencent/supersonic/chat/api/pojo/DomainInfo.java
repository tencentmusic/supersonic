package com.tencent.supersonic.chat.api.pojo;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class DomainInfo extends DataInfo implements Serializable {

    private List<String> words;
    private String primaryEntityBizName;
}

package com.tencent.supersonic.chat.api.pojo.response;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class ModelInfo extends DataInfo implements Serializable {

    private List<String> words;
    private String primaryEntityName;
    private String primaryEntityBizName;
}

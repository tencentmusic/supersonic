package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ViewInfo extends DataInfo implements Serializable {

    private List<String> words;
    private String primaryKey;
}

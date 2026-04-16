package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class DataSetInfo extends DataInfo implements Serializable {

    private List<String> words;
    private String primaryKey;
}

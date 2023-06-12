package com.tencent.supersonic.chat.domain.pojo.semantic;

import java.io.Serializable;
import lombok.Data;

@Data
public class AggregatorInfo implements Serializable {

    private String aggregator;
    private String targetWord;

}
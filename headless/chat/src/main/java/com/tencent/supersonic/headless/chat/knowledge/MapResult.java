package com.tencent.supersonic.headless.chat.knowledge;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class MapResult implements Serializable {

    protected String name;
    protected String detectWord;
}

package com.tencent.supersonic.headless.chat.knowledge;

import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MapResult implements Serializable {

    protected String name;
    protected String detectWord;
}
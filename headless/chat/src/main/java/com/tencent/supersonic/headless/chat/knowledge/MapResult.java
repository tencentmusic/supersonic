package com.tencent.supersonic.headless.chat.knowledge;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public abstract class MapResult implements Serializable {

    protected String name;

    protected int index;
    protected String detectWord;

    public abstract String getMapKey();

    public Boolean lessSimilar(MapResult otherResult) {
        String mapKey = this.getMapKey();
        String otherMapKey = otherResult.getMapKey();
        return mapKey.equals(otherMapKey)
                && otherResult.getDetectWord().length() < otherResult.getDetectWord().length();
    }
}

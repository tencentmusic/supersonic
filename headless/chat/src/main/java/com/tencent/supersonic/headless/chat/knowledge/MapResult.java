package com.tencent.supersonic.headless.chat.knowledge;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public abstract class MapResult implements Serializable {

    protected String name;
    protected int offset;

    protected String detectWord;

    protected double similarity;

    public abstract String getMapKey();

    public Boolean lessSimilar(MapResult otherResult) {
        return this.getMapKey().equals(otherResult.getMapKey())
                && this.similarity < otherResult.similarity;
    }
}

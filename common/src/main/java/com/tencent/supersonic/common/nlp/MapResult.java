package com.tencent.supersonic.common.nlp;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MapResult implements Serializable {

    private String name;
    private List<String> natures;
    private int offset = 0;

    private double similarity;

    public MapResult() {

    }

    public MapResult(String name, List<String> natures) {
        this.name = name;
        this.natures = natures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MapResult that = (MapResult) o;
        return Objects.equals(name, that.name) && Objects.equals(natures, that.natures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, natures);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}
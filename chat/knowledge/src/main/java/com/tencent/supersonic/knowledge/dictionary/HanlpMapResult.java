package com.tencent.supersonic.knowledge.dictionary;

import com.google.common.base.Objects;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class HanlpMapResult extends MapResult {

    private List<String> natures;
    private int offset = 0;

    private double similarity;

    public HanlpMapResult(String name, List<String> natures, String detectWord) {
        this.name = name;
        this.natures = natures;
        this.detectWord = detectWord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HanlpMapResult hanlpMapResult = (HanlpMapResult) o;
        return Objects.equal(name, hanlpMapResult.name) && Objects.equal(natures, hanlpMapResult.natures);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, natures);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}
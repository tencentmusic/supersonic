package com.tencent.supersonic.chat.domain.pojo.search;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class NameNature implements Serializable {

    private String name;
    private List<String> natures;

    public NameNature() {
    }

    public NameNature(String name, List<String> natures) {
        this.name = name;
        this.natures = natures;
    }
}
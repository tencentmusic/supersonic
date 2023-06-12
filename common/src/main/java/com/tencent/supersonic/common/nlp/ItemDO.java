package com.tencent.supersonic.common.nlp;

import java.io.Serializable;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class ItemDO implements Serializable {

    private Integer domain;
    private Integer itemId;
    private String name;
    private Long useCnt = 0L;


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemDO itemDO = (ItemDO) o;
        return Objects.equals(domain, itemDO.domain) && Objects.equals(itemId, itemDO.itemId)
                && Objects.equals(name, itemDO.name);
    }
}

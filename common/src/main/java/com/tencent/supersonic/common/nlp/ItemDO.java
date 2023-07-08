package com.tencent.supersonic.common.nlp;

import com.google.common.base.Objects;
import java.io.Serializable;
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
    private String bizName;
    private Long useCnt = 0L;

    public ItemDO() {
    }

    public ItemDO(Integer domain, Integer itemId, String name, String bizName) {
        this.domain = domain;
        this.itemId = itemId;
        this.name = name;
        this.bizName = bizName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemDO itemDO = (ItemDO) o;
        return Objects.equal(domain, itemDO.domain) && Objects.equal(itemId,
                itemDO.itemId) && Objects.equal(name, itemDO.name)
                && Objects.equal(bizName, itemDO.bizName) && Objects.equal(
                useCnt, itemDO.useCnt);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(domain, itemId, name, bizName, useCnt);
    }
}

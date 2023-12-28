package com.tencent.supersonic.headless.common.model.pojo;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class AppConfig {

    private List<Item> items = Lists.newArrayList();

    public Set<Item> getAllItems() {
        Set<Item> itemSet = new HashSet<>();
        for (Item item : items) {
            itemSet.add(item);
            itemSet.addAll(item.getRelateItems());
        }
        return itemSet;
    }

}

package com.tencent.supersonic.semantic.api.model.pojo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class ItemDateFilter {

    private List<Long> itemIds;
    @NonNull
    private String type;
}
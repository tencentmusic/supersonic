package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@ToString
public class ItemDateFilter {

    private List<Long> itemIds;
    @NonNull
    private String type;
}

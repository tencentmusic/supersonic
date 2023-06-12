package com.tencent.supersonic.semantic.api.core.response;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * @author: kanedai
 * @date: 2023/3/29
 */
@Data
@AllArgsConstructor
@ToString
public class ItemDateResp {

    private String dateFormat;
    private String startDate;
    private String endDate;
    private List<String> unavailableDateList = new ArrayList<>();
}
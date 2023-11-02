package com.tencent.supersonic.common.pojo;

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
    private String datePeriod;
    private List<String> unavailableDateList = new ArrayList<>();
}
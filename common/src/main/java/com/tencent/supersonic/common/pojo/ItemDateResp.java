package com.tencent.supersonic.common.pojo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@Builder
@ToString
public class ItemDateResp {

    private String dateFormat;
    private String startDate;
    private String endDate;
    private String datePeriod;
    private List<String> unavailableDateList;
}
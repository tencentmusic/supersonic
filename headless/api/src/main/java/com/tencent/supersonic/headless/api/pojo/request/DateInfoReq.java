package com.tencent.supersonic.headless.api.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DateInfoReq {

    private String type;
    private Long itemId;
    private String dateFormat;
    private String startDate;
    private String endDate;
    private String datePeriod;
    private List<String> unavailableDateList = new ArrayList<>();

    public DateInfoReq(String type, Long itemId, String dateFormat, String startDate,
            String endDate) {
        this.type = type;
        this.itemId = itemId;
        this.dateFormat = dateFormat;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public DateInfoReq(String type, Long itemId, String dateFormat, String startDate,
            String endDate, List<String> unavailableDateList) {
        this.type = type;
        this.itemId = itemId;
        this.dateFormat = dateFormat;
        this.startDate = startDate;
        this.endDate = endDate;
        this.unavailableDateList = unavailableDateList;
    }
}

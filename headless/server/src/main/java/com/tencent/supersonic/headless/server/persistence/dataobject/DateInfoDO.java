package com.tencent.supersonic.headless.server.persistence.dataobject;

import lombok.Data;

@Data
public class DateInfoDO {

    private Long id;
    private String type;
    private Long itemId;
    private String dateFormat;
    private String startDate;
    private String endDate;
    private String unavailableDateList;
    private String createdBy;
    private String updatedBy;
    private String datePeriod;


}
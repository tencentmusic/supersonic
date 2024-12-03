package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_available_date_info")
public class DateInfoDO {

    @TableId(type = IdType.AUTO)
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

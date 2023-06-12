package com.tencent.supersonic.chat.domain.pojo.chat;


import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class QueryData {

    Long domainId = 0L;
    List<SchemaItem> metrics = new ArrayList<>();
    List<SchemaItem> dimensions = new ArrayList<>();
    List<Filter> filters = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private DateConf dateInfo;
    private Long limit;
    private Boolean nativeQuery = false;


}

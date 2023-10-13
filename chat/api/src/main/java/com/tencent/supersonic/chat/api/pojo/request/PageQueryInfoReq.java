package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;
import java.util.List;

@Data
public class PageQueryInfoReq {

    private int current;

    private int pageSize;

    private String userName;

    private List<Long> ids;
}

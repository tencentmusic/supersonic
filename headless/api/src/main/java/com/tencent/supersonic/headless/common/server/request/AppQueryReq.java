package com.tencent.supersonic.headless.common.server.request;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.PageBaseReq;
import com.tencent.supersonic.headless.common.server.enums.AppStatus;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class AppQueryReq extends PageBaseReq {

    private String name;

    private List<AppStatus> appStatus;

    private String createdBy;

    public List<Integer> getStatus() {
        if (CollectionUtils.isEmpty(appStatus)) {
            return Lists.newArrayList();
        }
        return appStatus.stream().map(AppStatus::getCode).collect(Collectors.toList());
    }

}

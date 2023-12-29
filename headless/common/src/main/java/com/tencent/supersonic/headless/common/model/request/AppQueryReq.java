package com.tencent.supersonic.headless.common.model.request;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.PageBaseReq;
import com.tencent.supersonic.headless.common.model.enums.AppStatusEnum;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class AppQueryReq extends PageBaseReq {

    private String name;

    private List<AppStatusEnum> appStatus;

    private String createdBy;

    public List<Integer> getStatus() {
        if (CollectionUtils.isEmpty(appStatus)) {
            return Lists.newArrayList();
        }
        return appStatus.stream().map(AppStatusEnum::getCode).collect(Collectors.toList());
    }

}

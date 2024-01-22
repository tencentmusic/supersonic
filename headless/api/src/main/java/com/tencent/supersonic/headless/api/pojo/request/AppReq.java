package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.headless.api.pojo.AppConfig;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Data
public class AppReq extends RecordInfo {

    private Long id;

    private String name;

    private String description;

    private AppConfig config;

    private Date endDate;

    private Integer qps;

    private List<String> owners;

    public String getOwner() {
        if (CollectionUtils.isEmpty(owners)) {
            return "";
        }
        return String.join(",", owners);
    }

}

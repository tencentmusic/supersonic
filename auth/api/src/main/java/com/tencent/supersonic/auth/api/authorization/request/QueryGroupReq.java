package com.tencent.supersonic.auth.api.authorization.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class QueryGroupReq extends PageBaseReq {

    private List<Integer> groupIds;
    private List<String> users;
}

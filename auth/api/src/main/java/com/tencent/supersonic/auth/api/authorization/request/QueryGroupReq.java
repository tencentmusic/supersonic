package com.tencent.supersonic.auth.api.authorization.request;


import com.tencent.supersonic.common.pojo.PageBaseReq;
import java.util.List;
import lombok.Data;

@Data
public class QueryGroupReq extends PageBaseReq {

    private List<Integer> groupIds;
    private List<String> users;
}

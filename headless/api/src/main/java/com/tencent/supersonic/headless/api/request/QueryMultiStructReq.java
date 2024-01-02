package com.tencent.supersonic.headless.api.request;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;

@Data
@ToString
public class QueryMultiStructReq {


    List<QueryStructReq> queryStructReqs;

    public String toCustomizedString() {
        return JSONObject.toJSONString(queryStructReqs);
    }

    public String generateCommandMd5() {
        return DigestUtils.md5Hex(this.toCustomizedString());
    }

}

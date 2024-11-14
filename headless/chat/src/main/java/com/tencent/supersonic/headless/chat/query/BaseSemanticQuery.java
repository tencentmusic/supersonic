package com.tencent.supersonic.headless.chat.query;

import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@ToString
@Data
public abstract class BaseSemanticQuery implements SemanticQuery, Serializable {

    protected SemanticParseInfo parseInfo = new SemanticParseInfo();

    @Override
    public SemanticQueryReq buildSemanticQueryReq() {
        return QueryReqBuilder.buildS2SQLReq(parseInfo.getSqlInfo(), parseInfo.getDataSetId());
    }

}

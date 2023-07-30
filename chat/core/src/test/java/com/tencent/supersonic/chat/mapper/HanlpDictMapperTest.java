package com.tencent.supersonic.chat.mapper;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.test.context.ContextTest;
import org.junit.jupiter.api.Test;

/**
 * HanlpDictMapperTest
 */
class HanlpDictMapperTest extends ContextTest {

    @Test
    void map() {
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setChatId(1);
        queryRequest.setDomainId(2L);
        queryRequest.setQueryText("supersonic按部门访问次数");
        HanlpDictMapper hanlpDictMapper = new HanlpDictMapper();
        hanlpDictMapper.map(new QueryContext(queryRequest));
    }
}
package com.tencent.supersonic.chat.application.mapper;

import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.test.context.ContextTest;
import org.junit.jupiter.api.Test;

/**
 * HanlpSchemaMapperTest
 */
class HanlpSchemaMapperTest extends ContextTest {

    @Test
    void map() {
        QueryContextReq searchCtx = new QueryContextReq();
        searchCtx.setChatId(1);
        searchCtx.setDomainId(2);
        searchCtx.setQueryText("supersonic按部门访问次数");
        HanlpSchemaMapper hanlpSchemaMapper = new HanlpSchemaMapper();
        hanlpSchemaMapper.map(searchCtx);
    }
}
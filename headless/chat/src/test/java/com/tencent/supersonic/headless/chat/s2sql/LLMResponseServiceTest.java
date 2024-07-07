package com.tencent.supersonic.headless.chat.s2sql;

import com.tencent.supersonic.headless.chat.parser.llm.LLMResponseService;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlResp;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class LLMResponseServiceTest {

    @Test
    void deduplicationSqlWeight() {
        String sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        String sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";

        LLMResp llmResp = new LLMResp();
        Map<String, LLMSqlResp> sqlWeight = new HashMap<>();
        sqlWeight.put(sql1, LLMSqlResp.builder().sqlWeight(0.20).build());
        sqlWeight.put(sql2, LLMSqlResp.builder().sqlWeight(0.80).build());

        llmResp.setSqlRespMap(sqlWeight);
        LLMResponseService llmResponseService = new LLMResponseService();
        Map<String, LLMSqlResp> deduplicationSqlResp = llmResponseService.getDeduplicationSqlResp(0, llmResp);

        Assert.assertEquals(deduplicationSqlResp.size(), 1);

        sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";

        LLMResp llmResp2 = new LLMResp();
        Map<String, LLMSqlResp> sqlWeight2 = new HashMap<>();
        sqlWeight2.put(sql1, LLMSqlResp.builder().sqlWeight(0.20).build());
        sqlWeight2.put(sql2, LLMSqlResp.builder().sqlWeight(0.80).build());

        llmResp2.setSqlRespMap(sqlWeight2);
        deduplicationSqlResp = llmResponseService.getDeduplicationSqlResp(0, llmResp2);

        Assert.assertEquals(deduplicationSqlResp.size(), 1);

        sql1 = "SELECT a,b,c,d,e FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";

        LLMResp llmResp3 = new LLMResp();
        Map<String, LLMSqlResp> sqlWeight3 = new HashMap<>();
        sqlWeight3.put(sql1, LLMSqlResp.builder().sqlWeight(0.20).build());
        sqlWeight3.put(sql2, LLMSqlResp.builder().sqlWeight(0.80).build());
        llmResp3.setSqlRespMap(sqlWeight3);
        deduplicationSqlResp = llmResponseService.getDeduplicationSqlResp(0, llmResp3);

        Assert.assertEquals(deduplicationSqlResp.size(), 2);

    }
}
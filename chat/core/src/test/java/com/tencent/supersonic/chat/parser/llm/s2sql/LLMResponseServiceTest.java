package com.tencent.supersonic.chat.parser.llm.s2sql;

import com.tencent.supersonic.chat.parser.sql.llm.LLMResponseService;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class LLMResponseServiceTest {

    @Test
    void deduplicationSqlWeight() {
        String sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        String sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";

        LLMResp llmResp = new LLMResp();
        Map<String, Double> sqlWeight = new HashMap<>();
        sqlWeight.put(sql1, 0.2D);
        sqlWeight.put(sql2, 0.8D);
        llmResp.setSqlWeight(sqlWeight);
        LLMResponseService llmResponseService = new LLMResponseService();
        Map<String, Double> deduplicationSqlWeight = llmResponseService.getDeduplicationSqlWeight(llmResp);

        Assert.assertEquals(deduplicationSqlWeight.size(), 1);

        sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";

        LLMResp llmResp2 = new LLMResp();
        Map<String, Double> sqlWeight2 = new HashMap<>();
        sqlWeight2.put(sql1, 0.2D);
        sqlWeight2.put(sql2, 0.8D);
        llmResp2.setSqlWeight(sqlWeight2);
        deduplicationSqlWeight = llmResponseService.getDeduplicationSqlWeight(llmResp2);

        Assert.assertEquals(deduplicationSqlWeight.size(), 1);

        sql1 = "SELECT a,b,c,d,e FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";

        LLMResp llmResp3 = new LLMResp();
        Map<String, Double> sqlWeight3 = new HashMap<>();
        sqlWeight3.put(sql1, 0.2D);
        sqlWeight3.put(sql2, 0.8D);
        llmResp3.setSqlWeight(sqlWeight3);
        deduplicationSqlWeight = llmResponseService.getDeduplicationSqlWeight(llmResp3);

        Assert.assertEquals(deduplicationSqlWeight.size(), 2);

    }
}
package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.persistence.repository.ChatRepository;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.config.TenantConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@DisplayName("ChatManageServiceImpl showcase fallback")
class ChatManageServiceImplTest {

    @Test
    @DisplayName("falls back to latest successful showcase data when no liked records exist")
    void queryShowCase_fallsBackWhenNoLikedRecords() {
        ChatRepository chatRepository = mock(ChatRepository.class);
        ChatQueryRepository chatQueryRepository = mock(ChatQueryRepository.class);
        MemoryService memoryService = mock(MemoryService.class);
        UserService userService = mock(UserService.class);
        TenantConfig tenantConfig = mock(TenantConfig.class);
        ChatManageServiceImpl service = new ChatManageServiceImpl(chatRepository,
                chatQueryRepository, memoryService, userService, tenantConfig);

        PageQueryInfoReq req = new PageQueryInfoReq();
        req.setCurrent(1);
        req.setPageSize(20);

        QueryResp fallbackResp = new QueryResp();
        fallbackResp.setChatId(101L);
        fallbackResp.setQuestionId(1001L);
        fallbackResp.setQueryText("最近7天销售额");
        QueryResult queryResult = new QueryResult();
        queryResult.setResponse("ok");
        fallbackResp.setQueryResult(queryResult);

        when(chatQueryRepository.queryShowCase(req, 7)).thenReturn(Collections.emptyList());
        when(chatQueryRepository.queryShowCaseFallback(req, 7))
                .thenReturn(new ArrayList<>(List.of(fallbackResp)));
        when(chatQueryRepository.getParseInfoList(List.of(1001L)))
                .thenReturn(Collections.emptyList());

        ShowCaseResp resp = service.queryShowCase(req, 7);

        assertNotNull(resp.getShowCaseMap());
        assertEquals(1, resp.getShowCaseMap().size());
        assertEquals(1, resp.getShowCaseMap().get(101L).size());
        verify(chatQueryRepository).queryShowCase(req, 7);
        verify(chatQueryRepository).queryShowCaseFallback(req, 7);
    }

    @Test
    @DisplayName("keeps entries that have only textResult (no response/queryResults)")
    void queryShowCase_keepsTextResultOnly() {
        ChatRepository chatRepository = mock(ChatRepository.class);
        ChatQueryRepository chatQueryRepository = mock(ChatQueryRepository.class);
        MemoryService memoryService = mock(MemoryService.class);
        UserService userService = mock(UserService.class);
        TenantConfig tenantConfig = mock(TenantConfig.class);
        ChatManageServiceImpl service = new ChatManageServiceImpl(chatRepository,
                chatQueryRepository, memoryService, userService, tenantConfig);

        PageQueryInfoReq req = new PageQueryInfoReq();
        req.setCurrent(1);
        req.setPageSize(20);

        QueryResp resp = new QueryResp();
        resp.setChatId(102L);
        resp.setQuestionId(1002L);
        resp.setQueryText("今天是几号");
        QueryResult qr = new QueryResult();
        qr.setTextResult("今天是2024年4月9日。");
        resp.setQueryResult(qr);

        when(chatQueryRepository.queryShowCase(req, 7)).thenReturn(new ArrayList<>(List.of(resp)));
        when(chatQueryRepository.getParseInfoList(List.of(1002L)))
                .thenReturn(Collections.emptyList());

        ShowCaseResp result = service.queryShowCase(req, 7);

        assertNotNull(result.getShowCaseMap());
        assertEquals(1, result.getShowCaseMap().size());
        assertEquals(1, result.getShowCaseMap().get(102L).size());
    }

    @Test
    @DisplayName("keeps entries that have only textSummary (no response/queryResults)")
    void queryShowCase_keepsTextSummaryOnly() {
        ChatRepository chatRepository = mock(ChatRepository.class);
        ChatQueryRepository chatQueryRepository = mock(ChatQueryRepository.class);
        MemoryService memoryService = mock(MemoryService.class);
        UserService userService = mock(UserService.class);
        TenantConfig tenantConfig = mock(TenantConfig.class);
        ChatManageServiceImpl service = new ChatManageServiceImpl(chatRepository,
                chatQueryRepository, memoryService, userService, tenantConfig);

        PageQueryInfoReq req = new PageQueryInfoReq();
        req.setCurrent(1);
        req.setPageSize(20);

        QueryResp resp = new QueryResp();
        resp.setChatId(103L);
        resp.setQuestionId(1003L);
        resp.setQueryText("总结一下销售情况");
        QueryResult qr = new QueryResult();
        qr.setTextSummary("本月销售额较上月增长12%，主要来自华东区。");
        resp.setQueryResult(qr);

        when(chatQueryRepository.queryShowCase(req, 7)).thenReturn(new ArrayList<>(List.of(resp)));
        when(chatQueryRepository.getParseInfoList(List.of(1003L)))
                .thenReturn(Collections.emptyList());

        ShowCaseResp result = service.queryShowCase(req, 7);

        assertNotNull(result.getShowCaseMap());
        assertEquals(1, result.getShowCaseMap().size());
        assertEquals(1, result.getShowCaseMap().get(103L).size());
    }
}

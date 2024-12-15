package com.tencent.supersonic.headless.server.facade.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.SqlEvaluation;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlsReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class SqlQueryApiController {

    @Autowired
    private SemanticLayerService semanticLayerService;

    @Autowired
    private ChatLayerService chatLayerService;

    @PostMapping("/sql")
    public Object queryBySql(@RequestBody QuerySqlReq querySqlReq, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        String sql = querySqlReq.getSql();
        querySqlReq.setSql(StringUtil.replaceBackticks(sql));
        chatLayerService.correct(querySqlReq, user);
        return semanticLayerService.queryByReq(querySqlReq, user);
    }

    @PostMapping("/sqls")
    public Object queryBySqls(@RequestBody QuerySqlsReq querySqlsReq, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        List<SemanticQueryReq> semanticQueryReqs = querySqlsReq.getSqls().stream().map(sql -> {
            QuerySqlReq querySqlReq = new QuerySqlReq();
            BeanUtils.copyProperties(querySqlsReq, querySqlReq);
            querySqlReq.setSql(StringUtil.replaceBackticks(sql));
            chatLayerService.correct(querySqlReq, user);
            return querySqlReq;
        }).collect(Collectors.toList());

        List<CompletableFuture<SemanticQueryResp>> futures =
                semanticQueryReqs.stream().map(querySqlReq -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return semanticLayerService.queryByReq(querySqlReq, user);
                    } catch (Exception e) {
                        log.error("querySqlReq:{},queryByReq error:", querySqlReq, e);
                        return new SemanticQueryResp();
                    }
                })).collect(Collectors.toList());
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    @PostMapping("/sqlsWithException")
    public Object queryBySqlsWithException(@RequestBody QuerySqlsReq querySqlsReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        List<SemanticQueryReq> semanticQueryReqs = querySqlsReq.getSqls().stream().map(sql -> {
            QuerySqlReq querySqlReq = new QuerySqlReq();
            BeanUtils.copyProperties(querySqlsReq, querySqlReq);
            querySqlReq.setSql(StringUtil.replaceBackticks(sql));
            chatLayerService.correct(querySqlReq, user);
            return querySqlReq;
        }).collect(Collectors.toList());
        List<SemanticQueryResp> semanticQueryRespList = new ArrayList<>();
        try {
            for (SemanticQueryReq semanticQueryReq : semanticQueryReqs) {
                SemanticQueryResp semanticQueryResp =
                        semanticLayerService.queryByReq(semanticQueryReq, user);
                semanticQueryRespList.add(semanticQueryResp);
            }
        } catch (Exception e) {
            throw new Exception(e.getCause().getMessage());
        }
        return semanticQueryRespList;
    }

    @PostMapping("/validate")
    public Object validate(@RequestBody QuerySqlReq querySqlReq, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        String sql = querySqlReq.getSql();
        querySqlReq.setSql(StringUtil.replaceBackticks(sql));
        return chatLayerService.validate(querySqlReq, user);
    }

    @PostMapping("/validateAndQuery")
    public Object validateAndQuery(@RequestBody QuerySqlsReq querySqlsReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        List<QuerySqlReq> convert = convert(querySqlsReq);
        for (QuerySqlReq querySqlReq : convert) {
            SqlEvaluation validate = chatLayerService.validate(querySqlReq, user);
            if (!validate.getIsValidated()) {
                throw new Exception(validate.getValidateMsg());
            }
        }
        return queryBySqls(querySqlsReq, request, response);
    }

    private List<QuerySqlReq> convert(QuerySqlsReq querySqlsReq) {
        return querySqlsReq.getSqls().stream().map(sql -> {
            QuerySqlReq querySqlReq = new QuerySqlReq();
            BeanUtils.copyProperties(querySqlsReq, querySqlReq);
            querySqlReq.setSql(StringUtil.replaceBackticks(sql));
            return querySqlReq;
        }).collect(Collectors.toList());
    }

}

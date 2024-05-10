package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlsReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class SqlQueryApiController {

    @Autowired
    private QueryService queryService;

    @Autowired
    private ChatQueryService chatQueryService;

    @PostMapping("/sql")
    public Object queryBySql(@RequestBody QuerySqlReq querySqlReq,
                             HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        String sql = querySqlReq.getSql();
        querySqlReq.setSql(StringUtil.replaceBackticks(sql));
        chatQueryService.correct(querySqlReq, user);
        return queryService.queryByReq(querySqlReq, user);
    }

    @PostMapping("/sqls")
    public Object queryBySqls(@RequestBody QuerySqlsReq querySqlsReq,
                              HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        List<SemanticQueryReq> semanticQueryReqs = querySqlsReq.getSqls()
                .stream().map(sql -> {
                    QuerySqlReq querySqlReq = new QuerySqlReq();
                    BeanUtils.copyProperties(querySqlsReq, querySqlReq);
                    querySqlReq.setSql(StringUtil.replaceBackticks(sql));
                    chatQueryService.correct(querySqlReq, user);
                    return querySqlReq;
                }).collect(Collectors.toList());
        List<CompletableFuture<SemanticQueryResp>> futures = semanticQueryReqs.stream()
                        .map(querySqlReq -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return queryService.queryByReq(querySqlReq, user);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return new SemanticQueryResp();
                            }
                        }))
                        .collect(Collectors.toList());
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

}

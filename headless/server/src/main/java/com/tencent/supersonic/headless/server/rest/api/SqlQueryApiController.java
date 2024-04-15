package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlsReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.core.chat.corrector.GrammarCorrector;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.utils.ComponentFactory;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class SqlQueryApiController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/sql")
    public Object queryBySql(@RequestBody QuerySqlReq querySqlReq,
                             HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        correct(querySqlReq);
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
                    querySqlReq.setSql(sql);
                    correct(querySqlReq);
                    return querySqlReq;
                }).collect(Collectors.toList());
        return queryService.queryByReqs(semanticQueryReqs, user);
    }

    private void correct(QuerySqlReq querySqlReq) {
        QueryContext queryCtx = new QueryContext();
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SqlInfo sqlInfo = new SqlInfo();
        sqlInfo.setCorrectS2SQL(querySqlReq.getSql());
        sqlInfo.setS2SQL(querySqlReq.getSql());
        semanticParseInfo.setSqlInfo(sqlInfo);

        ComponentFactory.getSemanticCorrectors().forEach(corrector -> {
            if (!(corrector instanceof GrammarCorrector)) {
                corrector.correct(queryCtx, semanticParseInfo);
            }
        });
        querySqlReq.setSql(sqlInfo.getCorrectS2SQL());
    }
}

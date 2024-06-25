package com.tencent.supersonic.headless.server.web.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryRuleResp;
import com.tencent.supersonic.headless.server.web.service.QueryRuleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/semantic/query/rule")
public class QueryRuleController {

    private final QueryRuleService queryRuleService;

    public QueryRuleController(QueryRuleService queryRuleService) {
        this.queryRuleService = queryRuleService;
    }

    /**
     * 新建查询规则
     *
     * @param queryRuleReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/create")
    public QueryRuleResp create(@RequestBody @Validated QueryRuleReq queryRuleReq,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryRuleService.addQueryRule(queryRuleReq, user);
    }

    /**
     * 编辑查询规则
     *
     * @param queryRuleReq
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/update")
    public QueryRuleResp update(@RequestBody @Validated QueryRuleReq queryRuleReq,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryRuleService.updateQueryRule(queryRuleReq, user);
    }

    /**
     * 删除查询规则
     * @param id
     * @param request
     * @param response
     * @return
     */
    @DeleteMapping("delete/{id}")
    public Boolean delete(@PathVariable("id") Long id,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryRuleService.dropQueryRule(id, user);
    }

    /**
     * 查询规则列表
     * @param request
     * @param response
     * @return
     */
    @PostMapping("query")
    public List<QueryRuleResp> query(@RequestBody @Validated QueryRuleFilter queryRuleFilter,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryRuleService.getQueryRuleList(queryRuleFilter, user);
    }

}
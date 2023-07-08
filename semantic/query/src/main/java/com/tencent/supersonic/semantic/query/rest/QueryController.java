package com.tencent.supersonic.semantic.query.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.*;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import com.tencent.supersonic.semantic.query.domain.ParserService;
import com.tencent.supersonic.semantic.query.domain.QueryService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class QueryController {

    @Autowired
    private QueryService queryService;
    @Autowired
    private ParserService parserService;


    @PostMapping("/sql")
    public Object queryBySql(@RequestBody QuerySqlReq querySqlReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        Object queryBySql = queryService.queryBySql(querySqlReq, user);
        log.info("queryBySql:{},queryBySql");
        return queryBySql;
    }

    @PostMapping("/struct")
    public Object queryByStruct(@RequestBody QueryStructReq queryStructReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryByStruct(queryStructReq, user, request);
    }

    @PostMapping("/struct/parse")
    public SqlParserResp parseByStruct(@RequestBody ParseSqlReq parseSqlReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return parserService.physicalSql(parseSqlReq);
    }

    /**
     * queryByMultiStruct
     */
    @PostMapping("/multiStruct")
    public Object queryByMultiStruct(@RequestBody QueryMultiStructReq queryMultiStructReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryByMultiStruct(queryMultiStructReq, user);
    }

    /**
     * getStatInfo
     * query the used frequency of the metric/dimension
     *
     * @param itemUseReq
     */
    @PostMapping("/stat")
    public List<ItemUseResp> getStatInfo(@RequestBody ItemUseReq itemUseReq) {
        return queryService.getStatInfo(itemUseReq);
    }

}

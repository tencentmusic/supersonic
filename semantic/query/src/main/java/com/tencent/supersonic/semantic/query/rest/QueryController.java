package com.tencent.supersonic.semantic.query.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.enums.QueryTypeEnum;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.BatchDownloadReq;
import com.tencent.supersonic.semantic.api.query.request.ExplainSqlReq;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryDimValueReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2SQLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.service.DownloadService;
import com.tencent.supersonic.semantic.query.service.QueryService;
import com.tencent.supersonic.semantic.query.service.SemanticQueryEngine;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    private SemanticQueryEngine semanticQueryEngine;

    @Autowired
    private DownloadService downloadService;


    @PostMapping("/sql")
    public Object queryBySql(@RequestBody QueryS2SQLReq queryS2SQLReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        Object queryBySql = queryService.queryBySql(queryS2SQLReq, user);
        log.info("queryBySql:{},queryBySql");
        return queryBySql;
    }

    @PostMapping("/struct")
    public Object queryByStruct(@RequestBody QueryStructReq queryStructReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryByStructWithAuth(queryStructReq, user);
    }

    @PostMapping("/download/struct")
    public void downloadByStruct(@RequestBody QueryStructReq queryStructReq,
                                HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.downloadByStruct(queryStructReq, user, response);
    }

    @PostMapping("/download/batch")
    public void downloadBatch(@RequestBody BatchDownloadReq batchDownloadReq,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.batchDownload(batchDownloadReq, user, response);
    }

    @PostMapping("/queryStatement")
    public Object queryStatement(@RequestBody QueryStatement queryStatement) throws Exception {
        return queryService.queryByQueryStatement(queryStatement);
    }

    @PostMapping("/struct/parse")
    public SqlParserResp parseByStruct(@RequestBody ParseSqlReq parseSqlReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        QueryStructReq queryStructCmd = new QueryStructReq();
        QueryStatement queryStatement = semanticQueryEngine.physicalSql(queryStructCmd, parseSqlReq);
        SqlParserResp sqlParserResp = new SqlParserResp();
        BeanUtils.copyProperties(queryStatement, sqlParserResp);
        return sqlParserResp;
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

    @PostMapping("/queryDimValue")
    public QueryResultWithSchemaResp queryDimValue(@RequestBody QueryDimValueReq queryDimValueReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryService.queryDimValue(queryDimValueReq, user);
    }

    @PostMapping("/explain")
    public <T> ExplainResp explain(@RequestBody ExplainSqlReq<T> explainSqlReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        User user = UserHolder.findUser(request, response);
        String queryReqJson = JsonUtil.toString(explainSqlReq.getQueryReq());
        QueryTypeEnum queryTypeEnum = explainSqlReq.getQueryTypeEnum();

        if (QueryTypeEnum.SQL.equals(queryTypeEnum)) {
            QueryS2SQLReq queryS2SQLReq = JsonUtil.toObject(queryReqJson, QueryS2SQLReq.class);
            ExplainSqlReq<QueryS2SQLReq> explainSqlReqNew = ExplainSqlReq.<QueryS2SQLReq>builder()
                    .queryReq(queryS2SQLReq)
                    .queryTypeEnum(queryTypeEnum).build();
            return queryService.explain(explainSqlReqNew, user);
        }
        if (QueryTypeEnum.STRUCT.equals(queryTypeEnum)) {
            QueryStructReq queryStructReq = JsonUtil.toObject(queryReqJson, QueryStructReq.class);
            ExplainSqlReq<QueryStructReq> explainSqlReqNew = ExplainSqlReq.<QueryStructReq>builder()
                    .queryReq(queryStructReq)
                    .queryTypeEnum(queryTypeEnum).build();
            return queryService.explain(explainSqlReqNew, user);
        }
        return null;
    }

}

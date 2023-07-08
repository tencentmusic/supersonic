package com.tencent.supersonic.semantic.query.application;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.cache.CacheUtils;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.pojo.QueryStat;
import com.tencent.supersonic.semantic.api.core.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.pojo.Cache;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QuerySqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import com.tencent.supersonic.semantic.core.domain.DatabaseService;
import com.tencent.supersonic.semantic.query.domain.ParserService;
import com.tencent.supersonic.semantic.query.domain.QueryService;
import com.tencent.supersonic.semantic.query.domain.SchemaService;
import com.tencent.supersonic.semantic.query.domain.annotation.DataPermission;
import com.tencent.supersonic.semantic.query.domain.utils.QueryReqConverter;
import com.tencent.supersonic.semantic.query.domain.utils.QueryStructUtils;
import com.tencent.supersonic.semantic.query.domain.utils.StatUtils;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    private final ParserService parserService;
    private final DatabaseService databaseService;
    private final QueryStructUtils queryStructUtils;
    private final StatUtils statUtils;
    private final CacheUtils cacheUtils;
    private final QueryReqConverter queryReqConverter;

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;

    public QueryServiceImpl(ParserService parserService,
            DatabaseService databaseService,
            QueryStructUtils queryStructUtils,
            StatUtils statUtils,
            CacheUtils cacheUtils,
            QueryReqConverter queryReqConverter) {
        this.parserService = parserService;
        this.databaseService = databaseService;
        this.queryStructUtils = queryStructUtils;
        this.statUtils = statUtils;
        this.cacheUtils = cacheUtils;
        this.queryReqConverter = queryReqConverter;
    }

    @Override
    public Object queryBySql(QuerySqlReq querySqlCmd, User user) throws Exception {
        DomainSchemaFilterReq filter = new DomainSchemaFilterReq();
        List<Long> domainIds = new ArrayList<>();
        domainIds.add(querySqlCmd.getDomainId());

        filter.setDomainIds(domainIds);
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        List<DomainSchemaResp> domainSchemas = schemaService.fetchDomainSchema(filter, user);

        SqlParserResp sqlParser = queryReqConverter.convert(querySqlCmd, domainSchemas);

        return databaseService.executeSql(sqlParser.getSql(), querySqlCmd.getDomainId());
    }

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructCmd, User user) throws Exception {
        QueryResultWithSchemaResp queryResultWithColumns;
        log.info("[queryStructCmd:{}]", queryStructCmd);
        try {
            statUtils.initStatInfo(queryStructCmd, user);
            String cacheKey = cacheUtils.generateCacheKey(queryStructCmd.getDomainId().toString(),
                    queryStructCmd.generateCommandMd5());
            handleGlobalCacheDisable(queryStructCmd);

            if (queryStructUtils.queryCache(queryStructCmd.getCacheInfo())) {
                queryResultWithColumns = queryStructUtils.queryByStructByCache(queryStructCmd, cacheKey);
            } else {
                queryResultWithColumns = queryStructUtils.queryByStructWithoutCache(queryStructCmd, cacheKey);
            }
            statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);

        } catch (Exception e) {
            log.warn("exception in queryByStruct, e: ", e);
            statUtils.statInfo2DbAsync(TaskStatusEnum.ERROR);
            throw e;
        }

        return queryResultWithColumns;
    }

    @Override
    @DataPermission
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructCmd, User user, HttpServletRequest request)
            throws Exception {
        return queryByStruct(queryStructCmd, user);
    }


    @Override
    public QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructCmd, User user)
            throws Exception {
        statUtils.initStatInfo(queryMultiStructCmd.getQueryStructCmds().get(0), user);
        String cacheKey = cacheUtils.generateCacheKey(
                queryMultiStructCmd.getQueryStructCmds().get(0).getDomainId().toString(),
                queryMultiStructCmd.generateCommandMd5());
        return queryStructUtils.queryByMultiStructWithoutCache(queryMultiStructCmd, cacheKey);
    }


    private void handleGlobalCacheDisable(QueryStructReq queryStructCmd) {
        if (!cacheEnable) {
            Cache cacheInfo = new Cache();
            cacheInfo.setCache(false);
            queryStructCmd.setCacheInfo(cacheInfo);
        }
    }

    @Override
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend) {
        List<ItemUseResp> statInfos = statUtils.getStatInfo(itemUseCommend);
        return statInfos;
    }


    @Override
    public List<QueryStat> getQueryStatInfoWithoutCache(ItemUseReq itemUseCommend) {
        return statUtils.getQueryStatInfoWithoutCache(itemUseCommend);
    }


}

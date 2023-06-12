package com.tencent.supersonic.semantic.query.application;


import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.query.domain.ParserService;
import com.tencent.supersonic.semantic.query.domain.parser.SemanticSchemaManager;
import com.tencent.supersonic.semantic.query.domain.parser.SemanticSqlService;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.SemanticModel;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Primary
@Slf4j
public class ParserServiceImpl implements ParserService {

    private final SemanticSchemaManager semanticSchemaManager;
    private final SemanticSqlService semanticSqlService;

    public ParserServiceImpl(SemanticSchemaManager schemaManager,
            SemanticSqlService semanticSqlService) {
        this.semanticSchemaManager = schemaManager;
        this.semanticSqlService = semanticSqlService;
    }



    @Override
    public SqlParserResp physicalSql(ParseSqlReq sqlCommend) throws Exception {
        return parser(sqlCommend);
    }

    @Override
    public SqlParserResp physicalSql(MetricReq metricCommand) throws Exception {
        return parser(metricCommand);
    }


    public SqlParserResp parser(ParseSqlReq sqlCommend) {
        log.info("parser MetricReq [{}] ", sqlCommend);
        SqlParserResp sqlParserInfo = new SqlParserResp();
        try {
            if (!CollectionUtils.isEmpty(sqlCommend.getTables())) {
                List<String> tables = new ArrayList<>();
                String sourceId = "";
                for (MetricTable metricTable : sqlCommend.getTables()) {
                    MetricReq metricReq = new MetricReq();
                    metricReq.setMetrics(metricTable.getMetrics());
                    metricReq.setDimensions(metricTable.getDimensions());
                    metricReq.setWhere(formatWhere(metricTable.getWhere()));
                    metricReq.setRootPath(sqlCommend.getRootPath());
                    SqlParserResp tableSql = parser(metricReq, metricTable.isAgg());
                    if (!tableSql.isOk()) {
                        sqlParserInfo.setErrMsg(String.format("parser table [%s] error [%s]", metricTable.getAlias(),
                                tableSql.getErrMsg()));
                        return sqlParserInfo;
                    }
                    tables.add(String.format("%s as (%s)", metricTable.getAlias(), tableSql.getSql()));
                    sourceId = tableSql.getSourceId();
                }

                if (!tables.isEmpty()) {
                    String sql = "with " + String.join(",", tables) + "\n" + sqlCommend.getSql();
                    sqlParserInfo.setSql(sql);
                    sqlParserInfo.setSourceId(sourceId);
                    return sqlParserInfo;
                }
            }
        } catch (Exception e) {
            log.error("physicalSql error {}", e);
            sqlParserInfo.setErrMsg(e.getMessage());
        }
        return sqlParserInfo;
    }

    public SqlParserResp parser(MetricReq metricCommand) {
        return parser(metricCommand, true);
    }

    public SqlParserResp parser(MetricReq metricCommand, boolean isAgg) {
        log.info("parser MetricReq [{}] isAgg [{}]", metricCommand, isAgg);
        SqlParserResp sqlParser = new SqlParserResp();
        if (metricCommand.getRootPath().isEmpty()) {
            sqlParser.setErrMsg("rootPath empty");
            return sqlParser;
        }
        try {
            SemanticModel semanticModel = semanticSchemaManager.get(metricCommand.getRootPath());
            if (semanticModel == null) {
                sqlParser.setErrMsg("semanticSchema not found");
                return sqlParser;
            }
            return semanticSqlService.explain(metricCommand, isAgg, semanticModel);
        } catch (Exception e) {
            sqlParser.setErrMsg(e.getMessage());
            log.error("parser error MetricCommand[{}] error [{}]", metricCommand, e);
        }
        return sqlParser;
    }


    private String formatWhere(String where) {
        return where.replace("\"", "\\\\\"");
    }
}

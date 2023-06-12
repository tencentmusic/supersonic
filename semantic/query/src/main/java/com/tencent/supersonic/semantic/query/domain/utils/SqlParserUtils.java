package com.tencent.supersonic.semantic.query.domain.utils;

import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlParserUtils {

    private final ParserCommandConverter parserCommandConverter;


    private final MultiSourceJoinUtils multiSourceJoinUtils;

    public SqlParserUtils(ParserCommandConverter parserCommandConverter, MultiSourceJoinUtils multiSourceJoinUtils) {
        this.parserCommandConverter = parserCommandConverter;
        this.multiSourceJoinUtils = multiSourceJoinUtils;
    }


    public SqlParserResp getSqlParserWithoutCache(QueryStructReq queryStructCmd) throws Exception {
        log.info("stat getSqlParser without cache");
        multiSourceJoinUtils.buildJoinPrefix(queryStructCmd);
        SqlParserResp sqlParser = parserCommandConverter.getSqlParser(queryStructCmd);
        return sqlParser;
    }

}

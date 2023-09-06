package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class QueryFilterAppend extends BaseSemanticCorrector {

    @Override
    public CorrectionInfo corrector(CorrectionInfo correctionInfo) throws JSQLParserException {
        String queryFilter = getQueryFilter(correctionInfo.getQueryFilters());
        String preSql = correctionInfo.getSql();

        if (StringUtils.isNotEmpty(queryFilter)) {
            log.info("add queryFilter to preSql :{}", queryFilter);
            Expression expression = CCJSqlParserUtil.parseCondExpression(queryFilter);
            String sql = SqlParserUpdateHelper.addWhere(preSql, expression);
            correctionInfo.setPreSql(preSql);
            correctionInfo.setSql(sql);
        }
        return correctionInfo;
    }

    private String getQueryFilter(QueryFilters queryFilters) {
        if (Objects.isNull(queryFilters) || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return null;
        }
        return queryFilters.getFilters().stream()
                .map(filter -> {
                    String bizNameWrap = StringUtil.getSpaceWrap(filter.getBizName());
                    String operatorWrap = StringUtil.getSpaceWrap(filter.getOperator().getValue());
                    String valueWrap = StringUtil.getCommaWrap(filter.getValue().toString());
                    return bizNameWrap + operatorWrap + valueWrap;
                })
                .collect(Collectors.joining(Constants.AND_UPPER));
    }

}

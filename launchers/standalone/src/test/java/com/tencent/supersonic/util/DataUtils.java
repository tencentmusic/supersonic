package com.tencent.supersonic.util;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;

import java.util.Set;

public class DataUtils {

    public static QueryContextReq getQueryContextReq(Integer id,String query) {
        QueryContextReq queryContextReq = new QueryContextReq();
        queryContextReq.setQueryText(query);//"alice的访问次数"
        queryContextReq.setChatId(id);
        queryContextReq.setUser(new User(1L, "admin", "admin", "admin@email"));
        return queryContextReq;
    }

    public static SchemaItem getSchemaItem(Long id, String name, String bizName) {
        SchemaItem schemaItem = new SchemaItem();
        schemaItem.setId(id);
        schemaItem.setName(name);
        schemaItem.setBizName(bizName);
        return schemaItem;
    }

    public static Filter getFilter(String bizName, FilterOperatorEnum filterOperatorEnum, Object value, String name,
                                   Long elementId) {
        Filter filter = new Filter();
        filter.setBizName(bizName);
        filter.setOperator(filterOperatorEnum);
        filter.setValue(value);
        filter.setName(name);
        filter.setElementID(elementId);
        return filter;
    }

    public static DateConf getDateConf(Integer unit, DateConf.DateMode dateMode, String period) {
        DateConf dateInfo = new DateConf();
        dateInfo.setUnit(unit);
        dateInfo.setDateMode(dateMode);
        dateInfo.setPeriod(period);
        return dateInfo;
    }


    public static Boolean compareDate(DateConf dateInfo1, DateConf dateInfo2) {
        Boolean timeFilterExist = dateInfo1.getUnit().equals(dateInfo2.getUnit()) &&
                dateInfo1.getDateMode().equals(dateInfo2.getDateMode()) &&
                dateInfo1.getPeriod().equals(dateInfo2.getPeriod());
        return timeFilterExist;
    }

    public static Boolean compareSchemaItem(Set<SchemaItem> metrics, SchemaItem schemaItemMetric) {
        Boolean metricExist = false;
        for (SchemaItem schemaItem : metrics) {
            if(schemaItem.getBizName().equals(schemaItemMetric.getBizName()))
            if (schemaItem.getId()!=null&&schemaItem.getId().equals(schemaItemMetric.getId()) &&
                    schemaItem.getName()!=null&&schemaItem.getName().equals(schemaItemMetric.getName()) ) {
                metricExist = true;
            }
        }
        return metricExist;
    }

    public static Boolean compareDateDimension(Set<SchemaItem> dimensions) {
        SchemaItem schemaItemDimension = new SchemaItem();
        schemaItemDimension.setBizName("sys_imp_date");
        Boolean dimensionExist = false;
        for (SchemaItem schemaItem : dimensions) {
            if (schemaItem.getBizName().equals(schemaItemDimension.getBizName())) {
                dimensionExist = true;
            }
        }
        return dimensionExist;
    }

    public static Boolean compareDimensionFilter(Set<Filter> dimensionFilters, Filter dimensionFilter) {
        Boolean dimensionFilterExist = false;
        for (Filter filter : dimensionFilters) {
            if (filter.getBizName().equals(dimensionFilter.getBizName()) &&
                    filter.getOperator().equals(dimensionFilter.getOperator()) &&
                    filter.getValue().toString().equals(dimensionFilter.getValue().toString()) &&
                    filter.getElementID().equals(dimensionFilter.getElementID()) &&
                    filter.getName().equals(dimensionFilter.getName())) {
                dimensionFilterExist = true;
            }
        }
        return dimensionFilterExist;
    }

}

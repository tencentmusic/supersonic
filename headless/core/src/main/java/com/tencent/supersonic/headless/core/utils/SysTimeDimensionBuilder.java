package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * generate system time dimension tools
 */
@Slf4j
public class SysTimeDimensionBuilder {

    public static void addSysTimeDimension(List<Dim> dims, DbAdaptor engineAdaptor) {
        log.debug("addSysTimeDimension before:{}, engineAdaptor:{}", dims, engineAdaptor);
        Dim timeDim = getTimeDim(dims);
        if (timeDim == null) {
            timeDim = Dim.getDefault();
            //todo not find the time dimension
            return;
        }
        dims.add(generateSysDayDimension(timeDim, engineAdaptor));
        dims.add(generateSysWeekDimension(timeDim, engineAdaptor));
        dims.add(generateSysMonthDimension(timeDim, engineAdaptor));
        log.debug("addSysTimeDimension after:{}, engineAdaptor:{}", dims, engineAdaptor);
    }

    private static Dim generateSysDayDimension(Dim timeDim, DbAdaptor engineAdaptor) {
        Dim dim = new Dim();
        dim.setBizName(TimeDimensionEnum.DAY.getName());
        dim.setType(DimensionType.time.name());
        dim.setExpr(generateTimeExpr(timeDim, TimeDimensionEnum.DAY.name().toLowerCase(), engineAdaptor));
        DimensionTimeTypeParams typeParams = new DimensionTimeTypeParams();
        typeParams.setTimeGranularity(TimeDimensionEnum.DAY.name().toLowerCase());
        typeParams.setIsPrimary("true");
        dim.setTypeParams(typeParams);
        return dim;
    }

    private static Dim generateSysWeekDimension(Dim timeDim, DbAdaptor engineAdaptor) {
        Dim dim = new Dim();
        dim.setBizName(TimeDimensionEnum.WEEK.getName());
        dim.setType(DimensionType.time.name());
        dim.setExpr(generateTimeExpr(timeDim, TimeDimensionEnum.WEEK.name().toLowerCase(), engineAdaptor));
        DimensionTimeTypeParams typeParams = new DimensionTimeTypeParams();
        typeParams.setTimeGranularity(TimeDimensionEnum.WEEK.name().toLowerCase());
        typeParams.setIsPrimary("false");
        dim.setTypeParams(typeParams);
        return dim;
    }

    private static Dim generateSysMonthDimension(Dim timeDim, DbAdaptor engineAdaptor) {
        Dim dim = new Dim();
        dim.setBizName(TimeDimensionEnum.MONTH.getName());
        dim.setType(DimensionType.time.name());
        dim.setExpr(generateTimeExpr(timeDim, TimeDimensionEnum.MONTH.name().toLowerCase(), engineAdaptor));
        DimensionTimeTypeParams typeParams = new DimensionTimeTypeParams();
        typeParams.setTimeGranularity(TimeDimensionEnum.MONTH.name().toLowerCase());
        typeParams.setIsPrimary("false");
        dim.setTypeParams(typeParams);
        return dim;
    }

    private static String generateTimeExpr(Dim timeDim, String dateType, DbAdaptor engineAdaptor) {
        String bizName = timeDim.getBizName();
        String dateFormat = timeDim.getDateFormat();
        return engineAdaptor.getDateFormat(dateType, dateFormat, bizName);
    }

    private static Dim getTimeDim(List<Dim> timeDims) {
        for (Dim dim : timeDims) {
            if (dim.getType().equalsIgnoreCase(DimensionType.time.name())) {
                return dim;
            }
        }
        return null;
    }

}

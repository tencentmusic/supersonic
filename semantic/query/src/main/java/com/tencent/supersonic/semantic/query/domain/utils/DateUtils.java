package com.tencent.supersonic.domain.semantic.query.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.APOSTROPHE;
import static com.tencent.supersonic.common.constant.Constants.COMMA;
import static com.tencent.supersonic.common.constant.Constants.DAY;
import static com.tencent.supersonic.common.constant.Constants.DAY_FORMAT;
import static com.tencent.supersonic.common.constant.Constants.MONTH;

import com.google.common.base.Strings;
import com.tencent.supersonic.semantic.api.core.response.ItemDateResp;
import com.tencent.supersonic.common.pojo.DateConf;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Slf4j
@Component
public class DateUtils {

    @Value("${query.parameter.sys.date:sys_imp_date}")
    private String sysDateCol;

    public Boolean recentMode(DateConf dateInfo) {
        if (Objects.nonNull(dateInfo) && DateConf.DateMode.RECENT_UNITS == dateInfo.getDateMode()
                && DAY.equalsIgnoreCase(dateInfo.getPeriod()) && Objects.nonNull(dateInfo.getUnit())) {
            return true;
        }
        return false;
    }

    public boolean hasDataMode(DateConf dateInfo) {
        if (Objects.nonNull(dateInfo) && DateConf.DateMode.AVAILABLE_TIME == dateInfo.getDateMode()) {
            return true;
        }
        return false;
    }

    /**
     * dateMode = 4, advance time until data is available
     *
     * @param dateDate
     * @param dateInfo
     * @return
     */
    public String hasDataModeStr(ItemDateResp dateDate, DateConf dateInfo) {
        if (Objects.isNull(dateDate)
                || Strings.isNullOrEmpty(dateDate.getStartDate())
                || Strings.isNullOrEmpty(dateDate.getStartDate())
        ) {
            return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, dateInfo.getStartDate(), sysDateCol,
                    dateInfo.getEndDate());
        } else {
            log.info("dateDate:{}", dateDate);
        }
        String dateFormatStr = dateDate.getDateFormat();
        if (Strings.isNullOrEmpty(dateFormatStr)) {
            dateFormatStr = DAY_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        LocalDate endData = LocalDate.parse(dateDate.getEndDate(), formatter);
        LocalDate endReq = LocalDate.parse(dateInfo.getEndDate(), formatter);

        if (endReq.isAfter(endData)) {
            if (DAY.equalsIgnoreCase(dateInfo.getPeriod())) {
                Long unit = getInterval(dateInfo.getStartDate(), dateInfo.getEndDate(), dateFormatStr, ChronoUnit.DAYS);
                LocalDate dateMax = endData;
                LocalDate dateMin = dateMax.minusDays(unit - 1);
                return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, dateMin, sysDateCol, dateMax);
            }

            if (MONTH.equalsIgnoreCase(dateInfo.getPeriod())) {
                Long unit = getInterval(dateInfo.getStartDate(), dateInfo.getEndDate(), dateFormatStr,
                        ChronoUnit.MONTHS);
                return generateMonthSql(endData, unit, dateFormatStr);
            }

        }
        return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, dateInfo.getStartDate(), sysDateCol,
                dateInfo.getEndDate());
    }


    public String generateMonthSql(LocalDate endData, Long unit, String dateFormatStr) {
        LocalDate dateMax = endData;
        List<String> months = generateMonthStr(dateMax, unit, dateFormatStr);
        if (!CollectionUtils.isEmpty(months)) {
            StringJoiner joiner = new StringJoiner(",");
            months.stream().forEach(month -> joiner.add("'" + month + "'"));
            return String.format("(%s in (%s))", sysDateCol, joiner.toString());
        }
        return "";
    }

    private List<String> generateMonthStr(LocalDate dateMax, Long unit, String formatStr) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern(formatStr);
        List<String> months = new ArrayList<>();
        for (int i = unit.intValue() - 1; i >= 0; i--) {
            LocalDate tmp = dateMax.minusMonths(i);
            months.add(tmp.with(TemporalAdjusters.firstDayOfMonth()).format(format));
        }
        return months;
    }

    public String recentDayStr(ItemDateResp dateDate, DateConf dateInfo) {
        String dateFormatStr = dateDate.getDateFormat();
        if (Strings.isNullOrEmpty(dateFormatStr)) {
            dateFormatStr = DAY_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        LocalDate end = LocalDate.parse(dateDate.getEndDate(), formatter);
        // todo  unavailableDateList logic

        Integer unit = dateInfo.getUnit() - 1;
        String start = end.minusDays(unit).format(formatter);
        return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, start, sysDateCol, dateDate.getEndDate());
    }

    private Long getInterval(String startDate, String endDate, String dateFormat, ChronoUnit chronoUnit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        try {
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);
            return start.until(end, chronoUnit) + 1;
        } catch (Exception e) {
            log.warn("e:{}", e);
        }
        return -1L;
    }


    public String recentDateStr(ItemDateResp dateDate, DateConf dateInfo) {
        if (DAY.equalsIgnoreCase(dateInfo.getPeriod())) {
            return recentDayStr(dateDate, dateInfo);
        }
        return "";
    }

    /**
     * dateMode = 1; between, continuous value
     *
     * @param dateInfo
     * @return
     */
    public String betweenDateStr(ItemDateResp dateDate, DateConf dateInfo) {
        return String.format("%s >= '%s' and %s <= '%s'",
                sysDateCol, dateInfo.getStartDate(), sysDateCol, dateInfo.getEndDate());
    }

    /**
     * dateMode = 2; list discrete value
     *
     * @param dateInfo
     * @return
     */
    public String listDateStr(ItemDateResp dateDate, DateConf dateInfo) {
        StringJoiner joiner = new StringJoiner(COMMA);
        dateInfo.getDateList().stream().forEach(date -> joiner.add(APOSTROPHE + date + APOSTROPHE));
        return String.format("(%s in (%s))", sysDateCol, joiner.toString());
    }

    /**
     * dateMode = 3; - recent time units
     *
     * @param dateInfo
     * @return
     */
    public String defaultRecentDateInfo(DateConf dateInfo) {
        if (Objects.isNull(dateInfo)) {
            return "";
        }

        Integer unit = dateInfo.getUnit();

        if (DAY.equalsIgnoreCase(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            LocalDate dateMin = dateMax.minusDays(unit - 1);
            return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, dateMin, sysDateCol, dateMax);
        }

        if (MONTH.equalsIgnoreCase(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            return generateMonthSql(dateMax, unit.longValue(), DAY_FORMAT);
        }

        return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, LocalDate.now().minusDays(2), sysDateCol,
                LocalDate.now().minusDays(1));
    }

    public String getDateWhereStr(DateConf dateInfo, ItemDateResp dateDate) {
        String dateStr = "";
        switch (dateInfo.getDateMode()) {
            case BETWEEN_CONTINUOUS:
                dateStr = betweenDateStr(dateDate, dateInfo);
                break;
            case LIST_DISCRETE:
                dateStr = listDateStr(dateDate, dateInfo);
                break;
            case RECENT_UNITS:
                dateStr = recentDateStr(dateDate, dateInfo);
                break;
            case AVAILABLE_TIME:
                dateStr = hasDataModeStr(dateDate, dateInfo);
                break;
            default:
                break;

        }

        return dateStr;
    }
}
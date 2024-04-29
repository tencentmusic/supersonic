package com.tencent.supersonic.common.util;

import com.google.common.base.Strings;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static com.tencent.supersonic.common.pojo.Constants.APOSTROPHE;
import static com.tencent.supersonic.common.pojo.Constants.COMMA;
import static com.tencent.supersonic.common.pojo.Constants.DAY;
import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.MONTH;
import static com.tencent.supersonic.common.pojo.Constants.MONTH_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.WEEK;
import static com.tencent.supersonic.common.pojo.Constants.YEAR;


@Slf4j
@Component
@Data
public class DateModeUtils {

    @Value("${query.parameter.sys.date:sys_imp_date}")
    private String sysDateCol;
    @Value("${query.parameter.sys.month:sys_imp_month}")
    private String sysDateMonthCol;
    @Value("${query.parameter.sys.month:sys_imp_week}")
    private String sysDateWeekCol;

    @Value("${query.parameter.sys.zipper.begin:start_}")
    private String sysZipperDateColBegin;
    @Value("${query.parameter.sys.zipper.end:end_}")
    private String sysZipperDateColEnd;

    public Boolean recentMode(DateConf dateInfo) {
        if (Objects.nonNull(dateInfo) && DateConf.DateMode.RECENT == dateInfo.getDateMode()
                && DAY.equalsIgnoreCase(dateInfo.getPeriod()) && Objects.nonNull(dateInfo.getUnit())) {
            return true;
        }
        return false;
    }

    public boolean hasAvailableDataMode(DateConf dateInfo) {
        if (Objects.nonNull(dateInfo) && DateConf.DateMode.AVAILABLE == dateInfo.getDateMode()) {
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
        ImmutablePair<String, String> dayRange = recentDay(dateDate, dateInfo);
        return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, dayRange.left, sysDateCol, dayRange.right);
    }

    public ImmutablePair<String, String> recentDay(ItemDateResp dateDate, DateConf dateInfo) {
        String dateFormatStr = dateDate.getDateFormat();
        if (Strings.isNullOrEmpty(dateFormatStr)) {
            dateFormatStr = DAY_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        LocalDate end = LocalDate.parse(dateDate.getEndDate(), formatter);
        // todo  unavailableDateList logic

        Integer unit = dateInfo.getUnit() - 1;
        String start = end.minusDays(unit).format(formatter);
        return ImmutablePair.of(start, dateDate.getEndDate());
    }

    public String recentMonthStr(LocalDate endData, Long unit, String dateFormatStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        String endStr = endData.format(formatter);
        String start = endData.minusMonths(unit).format(formatter);
        return String.format("(%s >= '%s' and %s <= '%s')", sysDateMonthCol, start, sysDateMonthCol, endStr);
    }

    public String recentMonthStr(ItemDateResp dateDate, DateConf dateInfo) {
        List<ImmutablePair<String, String>> range = recentMonth(dateDate, dateInfo);
        if (range.size() == 1) {
            return String.format("(%s >= '%s' and %s <= '%s')", sysDateMonthCol, range.get(0).left, sysDateMonthCol,
                    range.get(0).right);
        }
        if (range.size() > 0) {
            StringJoiner joiner = new StringJoiner(",");
            range.stream().forEach(month -> joiner.add("'" + month.left + "'"));
            return String.format("(%s in (%s))", sysDateCol, joiner.toString());
        }
        return "";
    }

    public List<ImmutablePair<String, String>> recentMonth(ItemDateResp dateDate, DateConf dateInfo) {
        LocalDate endData = LocalDate.parse(dateDate.getEndDate(),
                DateTimeFormatter.ofPattern(dateDate.getDateFormat()));
        List<ImmutablePair<String, String>> ret = new ArrayList<>();
        if (dateDate.getDatePeriod() != null && MONTH.equalsIgnoreCase(dateDate.getDatePeriod())) {
            Long unit = getInterval(dateInfo.getStartDate(), dateInfo.getEndDate(), dateDate.getDateFormat(),
                    ChronoUnit.MONTHS);
            LocalDate dateMax = endData;
            List<String> months = generateMonthStr(dateMax, unit, dateDate.getDateFormat());
            if (!CollectionUtils.isEmpty(months)) {
                months.stream().forEach(m -> ret.add(ImmutablePair.of(m, m)));
                return ret;
            }
        }
        String dateFormatStr = MONTH_FORMAT;
        Integer unit = dateInfo.getUnit() - 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        String endStr = endData.format(formatter);
        String start = endData.minusMonths(unit).format(formatter);
        ret.add(ImmutablePair.of(start, endStr));
        return ret;
    }

    public String recentWeekStr(LocalDate endData, Long unit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DAY_FORMAT);
        String start = endData.minusDays(unit * 7).format(formatter);
        return String.format("(%s >= '%s' and %s <= '%s')", sysDateWeekCol, start, sysDateWeekCol,
                endData.format(formatter));
    }

    public String recentWeekStr(ItemDateResp dateDate, DateConf dateInfo) {
        ImmutablePair<String, String> dayRange = recentWeek(dateDate, dateInfo);
        return String.format("(%s >= '%s' and %s <= '%s')", sysDateWeekCol, dayRange.left, sysDateWeekCol,
                dayRange.right);
    }

    public ImmutablePair<String, String> recentWeek(ItemDateResp dateDate, DateConf dateInfo) {
        String dateFormatStr = dateDate.getDateFormat();
        if (Strings.isNullOrEmpty(dateFormatStr)) {
            dateFormatStr = DAY_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        LocalDate end = LocalDate.parse(dateDate.getEndDate(), formatter);
        Integer unit = dateInfo.getUnit() - 1;
        String start = end.minusDays(unit * 7).format(formatter);
        return ImmutablePair.of(start, end.format(formatter));
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
        if (Objects.isNull(dateDate)) {
            return "";
        }
        if (DAY.equalsIgnoreCase(dateInfo.getPeriod())) {
            return recentDayStr(dateDate, dateInfo);
        }
        if (MONTH.equalsIgnoreCase(dateInfo.getPeriod())) {
            return recentMonthStr(dateDate, dateInfo);
        }
        if (WEEK.equalsIgnoreCase(dateInfo.getPeriod())) {
            return recentWeekStr(dateDate, dateInfo);
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
        if (MONTH.equalsIgnoreCase(dateInfo.getPeriod())) {
            // startDate YYYYMM
            if (!dateInfo.getStartDate().contains(Constants.MINUS)) {
                return String.format("%s >= '%s' and %s <= '%s'",
                        sysDateMonthCol, dateInfo.getStartDate(), sysDateMonthCol, dateInfo.getEndDate());
            }
            LocalDate endData = LocalDate.parse(dateInfo.getEndDate(),
                    DateTimeFormatter.ofPattern(DAY_FORMAT));
            LocalDate startData = LocalDate.parse(dateInfo.getStartDate(),
                    DateTimeFormatter.ofPattern(DAY_FORMAT));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(MONTH_FORMAT);
            return String.format("%s >= '%s' and %s <= '%s'",
                    sysDateMonthCol, startData.format(formatter), sysDateMonthCol, endData.format(formatter));
        }
        if (WEEK.equalsIgnoreCase(dateInfo.getPeriod())) {
            return String.format("%s >= '%s' and %s <= '%s'",
                    sysDateWeekCol, dateInfo.getStartDate(), sysDateWeekCol, dateInfo.getEndDate());
        }
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
        String dateCol = sysDateCol;
        if (MONTH.equalsIgnoreCase(dateInfo.getPeriod())) {
            dateCol = sysDateMonthCol;
        }
        if (WEEK.equalsIgnoreCase(dateInfo.getPeriod())) {
            dateCol = sysDateWeekCol;
        }
        return String.format("(%s in (%s))", dateCol, joiner.toString());
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

        if (WEEK.equalsIgnoreCase(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            return recentWeekStr(dateMax, unit.longValue());
        }
        if (MONTH.equalsIgnoreCase(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            return recentMonthStr(dateMax, unit.longValue(), MONTH_FORMAT);
        }
        if (YEAR.equalsIgnoreCase(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            return recentMonthStr(dateMax, unit.longValue() * 12, MONTH_FORMAT);
        }

        return String.format("(%s >= '%s' and %s <= '%s')", sysDateCol, LocalDate.now().minusDays(2), sysDateCol,
                LocalDate.now().minusDays(1));
    }

    public String getDateWhereStr(DateConf dateInfo) {
        ItemDateResp dateDate = null;
        return getDateWhereStr(dateInfo, dateDate);
    }

    public String getDateWhereStr(DateConf dateInfo, ItemDateResp dateDate) {
        if (Objects.isNull(dateInfo)) {
            return "";
        }
        String dateStr = "";
        switch (dateInfo.getDateMode()) {
            case BETWEEN:
                dateStr = betweenDateStr(dateDate, dateInfo);
                break;
            case LIST:
                dateStr = listDateStr(dateDate, dateInfo);
                break;
            case RECENT:
                dateStr = recentDateStr(dateDate, dateInfo);
                break;
            case AVAILABLE:
                dateStr = hasDataModeStr(dateDate, dateInfo);
                break;
            default:
                break;

        }

        return dateStr;
    }

    public String getDateWhereStr(DateConf dateConf, ImmutablePair<String, String> range) {
        if (DAY.equalsIgnoreCase(dateConf.getPeriod()) || WEEK.equalsIgnoreCase(dateConf.getPeriod())) {
            if (range.left.equals(range.right)) {
                return String.format("(%s <= '%s' and %s > '%s')", sysZipperDateColBegin + sysDateCol, range.left,
                        sysZipperDateColEnd + sysDateCol, range.left);
            }
            return String.format("( '%s' <= %s and '%s' >= %s)", range.left, sysZipperDateColEnd + sysDateCol,
                    range.right, sysZipperDateColBegin + sysDateCol);
        }

        if (MONTH.equalsIgnoreCase(dateConf.getPeriod())) {
            if (range.left.equals(range.right)) {
                return String.format("(%s <= '%s' and %s > '%s')", sysZipperDateColBegin + sysDateMonthCol, range.left,
                        sysZipperDateColEnd + sysDateMonthCol, range.left);
            }
            return String.format("( '%s' <= %s and '%s' >= %s)", range.left, sysZipperDateColEnd + sysDateMonthCol,
                    range.right, sysZipperDateColBegin + sysDateMonthCol);

        }
        return "";
    }

    public String getSysDateCol(DateConf dateInfo) {
        if (DAY.equalsIgnoreCase(dateInfo.getPeriod())) {
            return sysDateCol;
        }
        if (WEEK.equalsIgnoreCase(dateInfo.getPeriod())) {
            return sysDateWeekCol;
        }
        if (MONTH.equalsIgnoreCase(dateInfo.getPeriod())) {
            return sysDateMonthCol;
        }
        return "";
    }

    public boolean isDateStr(String date) {
        return Pattern.matches("[\\d\\s-:]+", date);
    }

    public String getPeriodByCol(String col) {
        if (sysDateCol.equalsIgnoreCase(col)) {
            return DAY;
        }
        if (sysDateWeekCol.equalsIgnoreCase(col)) {
            return WEEK;
        }
        if (sysDateMonthCol.equalsIgnoreCase(col)) {
            return MONTH;
        }
        return "";
    }

    public String getDateColBegin(DateConf dateInfo) {
        return sysZipperDateColBegin + getSysDateCol(dateInfo);
    }

    public String getDateColEnd(DateConf dateInfo) {
        return sysZipperDateColEnd + getSysDateCol(dateInfo);
    }

    public List<String> getDateCol() {
        return Arrays.asList(sysDateCol, sysDateMonthCol, sysDateWeekCol);
    }

}

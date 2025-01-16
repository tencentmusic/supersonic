package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static com.tencent.supersonic.common.pojo.Constants.*;

@Slf4j
@Component
@Data
public class DateModeUtils {

    @Value("${s2.query.parameter.sys.zipper.begin:start_}")
    private String sysZipperDateColBegin;

    @Value("${s2.query.parameter.sys.zipper.end:end_}")
    private String sysZipperDateColEnd;

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
        if (Objects.isNull(dateDate) || StringUtils.isEmpty(dateDate.getStartDate())
                || StringUtils.isEmpty(dateDate.getStartDate())) {
            return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(),
                    dateInfo.getStartDate(), dateInfo.getDateField(), dateInfo.getEndDate());
        } else {
            log.info("dateDate:{}", dateDate);
        }
        String dateFormatStr = dateDate.getDateFormat();
        if (StringUtils.isEmpty(dateFormatStr)) {
            dateFormatStr = DAY_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        LocalDate endData = LocalDate.parse(dateDate.getEndDate(), formatter);
        LocalDate endReq = LocalDate.parse(dateInfo.getEndDate(), formatter);

        if (endReq.isAfter(endData)) {
            if (DatePeriodEnum.DAY.equals(dateInfo.getPeriod())) {
                Long unit = getInterval(dateInfo.getStartDate(), dateInfo.getEndDate(),
                        dateFormatStr, ChronoUnit.DAYS);
                LocalDate dateMax = endData;
                LocalDate dateMin = dateMax.minusDays(unit - 1);
                return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(),
                        dateMin, dateInfo.getDateField(), dateMax);
            }

            if (DatePeriodEnum.MONTH.equals(dateInfo.getPeriod())) {
                Long unit = getInterval(dateInfo.getStartDate(), dateInfo.getEndDate(),
                        dateFormatStr, ChronoUnit.MONTHS);
                return generateMonthSql(endData, unit, dateFormatStr, dateInfo);
            }
        }
        return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(),
                dateInfo.getStartDate(), dateInfo.getDateField(), dateInfo.getEndDate());
    }

    public String generateMonthSql(LocalDate endData, Long unit, String dateFormatStr,
            DateConf dateConf) {
        LocalDate dateMax = endData;
        List<String> months = generateMonthStr(dateMax, unit, dateFormatStr);
        if (!CollectionUtils.isEmpty(months)) {
            StringJoiner joiner = new StringJoiner(",");
            months.stream().forEach(month -> joiner.add("'" + month + "'"));
            return String.format("(%s in (%s))", dateConf.getDateField(), joiner.toString());
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
        return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(), dayRange.left,
                dateInfo.getDateField(), dayRange.right);
    }

    public ImmutablePair<String, String> recentDay(ItemDateResp dateDate, DateConf dateInfo) {
        String dateFormatStr = dateDate.getDateFormat();
        if (StringUtils.isEmpty(dateFormatStr)) {
            dateFormatStr = DAY_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        LocalDate end = LocalDate.parse(dateDate.getEndDate(), formatter);
        // todo unavailableDateList logic

        Integer unit = dateInfo.getUnit() - 1;
        String start = end.minusDays(unit).format(formatter);
        return ImmutablePair.of(start, dateDate.getEndDate());
    }

    public String recentMonthStr(LocalDate endData, Long unit, String dateFormatStr,
            DateConf dateInfo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        String endStr = endData.format(formatter);
        String start = endData.minusMonths(unit).format(formatter);
        return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(), start,
                dateInfo.getDateField(), endStr);
    }

    public String recentMonthStr(ItemDateResp dateDate, DateConf dateInfo) {
        List<ImmutablePair<String, String>> range = recentMonth(dateDate, dateInfo);
        if (range.size() == 1) {
            return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(),
                    range.get(0).left, dateInfo.getDateField(), range.get(0).right);
        }
        if (range.size() > 0) {
            StringJoiner joiner = new StringJoiner(",");
            range.stream().forEach(month -> joiner.add("'" + month.left + "'"));
            return String.format("(%s in (%s))", dateInfo.getDateField(), joiner.toString());
        }
        return "";
    }

    public List<ImmutablePair<String, String>> recentMonth(ItemDateResp dateDate,
            DateConf dateInfo) {
        LocalDate endData = LocalDate.parse(dateDate.getEndDate(),
                DateTimeFormatter.ofPattern(dateDate.getDateFormat()));
        List<ImmutablePair<String, String>> ret = new ArrayList<>();
        if (dateDate.getDatePeriod() != null
                && DatePeriodEnum.MONTH.equals(dateDate.getDatePeriod())) {
            Long unit = getInterval(dateInfo.getStartDate(), dateInfo.getEndDate(),
                    dateDate.getDateFormat(), ChronoUnit.MONTHS);
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

    public String recentWeekStr(LocalDate endData, Long unit, DateConf dataInfo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DAY_FORMAT);
        String start = endData.minusDays(unit * 7).format(formatter);
        return String.format("(%s >= '%s' and %s <= '%s')", dataInfo.getDateField(), start,
                dataInfo.getDateField(), endData.format(formatter));
    }

    public String recentWeekStr(ItemDateResp dateDate, DateConf dateInfo) {
        ImmutablePair<String, String> dayRange = recentWeek(dateDate, dateInfo);
        return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(), dayRange.left,
                dateInfo.getDateField(), dayRange.right);
    }

    public ImmutablePair<String, String> recentWeek(ItemDateResp dateDate, DateConf dateInfo) {
        String dateFormatStr = dateDate.getDateFormat();
        if (StringUtils.isEmpty(dateFormatStr)) {
            dateFormatStr = DAY_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatStr);
        LocalDate end = LocalDate.parse(dateDate.getEndDate(), formatter);
        Integer unit = dateInfo.getUnit() - 1;
        String start = end.minusDays(unit * 7).format(formatter);
        return ImmutablePair.of(start, end.format(formatter));
    }

    private Long getInterval(String startDate, String endDate, String dateFormat,
            ChronoUnit chronoUnit) {
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
        if (DatePeriodEnum.DAY.equals(dateInfo.getPeriod())) {
            return recentDayStr(dateDate, dateInfo);
        }
        if (DatePeriodEnum.MONTH.equals(dateInfo.getPeriod())) {
            return recentMonthStr(dateDate, dateInfo);
        }
        if (DatePeriodEnum.WEEK.equals(dateInfo.getPeriod())) {
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
    public String betweenDateStr(DateConf dateInfo) {
        String dateField = dateInfo.getDateField();
        if (DatePeriodEnum.MONTH.equals(dateInfo.getPeriod())) {
            // startDate YYYYMM
            if (!dateInfo.getStartDate().contains(Constants.MINUS)) {
                return String.format("%s >= '%s' and %s <= '%s'", dateField,
                        dateInfo.getStartDate(), dateField, dateInfo.getEndDate());
            }
            LocalDate endData =
                    LocalDate.parse(dateInfo.getEndDate(), DateTimeFormatter.ofPattern(DAY_FORMAT));
            LocalDate startData = LocalDate.parse(dateInfo.getStartDate(),
                    DateTimeFormatter.ofPattern(DAY_FORMAT));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(MONTH_FORMAT);
            return String.format("%s >= '%s' and %s <= '%s'", dateField,
                    startData.format(formatter), dateField, endData.format(formatter));
        }
        if (DatePeriodEnum.WEEK.equals(dateInfo.getPeriod())) {
            return String.format("%s >= '%s' and %s <= '%s'", dateField, dateInfo.getStartDate(),
                    dateField, dateInfo.getEndDate());
        }
        return String.format("%s >= '%s' and %s <= '%s'", dateField, dateInfo.getStartDate(),
                dateField, dateInfo.getEndDate());
    }

    /**
     * dateMode = 2; list discrete value
     *
     * @param dateInfo
     * @return
     */
    public String listDateStr(DateConf dateInfo) {
        StringJoiner joiner = new StringJoiner(COMMA);
        dateInfo.getDateList().stream().forEach(date -> joiner.add(APOSTROPHE + date + APOSTROPHE));
        String dateCol = dateInfo.getDateField();
        if (DatePeriodEnum.MONTH.equals(dateInfo.getPeriod())) {
            dateCol = dateInfo.getDateField();
        }
        if (DatePeriodEnum.WEEK.equals(dateInfo.getPeriod())) {
            dateCol = dateInfo.getDateField();
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

        if (DatePeriodEnum.DAY.equals(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            LocalDate dateMin = dateMax.minusDays(unit - 1);
            return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(), dateMin,
                    dateInfo.getDateField(), dateMax);
        }

        if (DatePeriodEnum.WEEK.equals(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            return recentWeekStr(dateMax, unit.longValue(), dateInfo);
        }
        if (DatePeriodEnum.MONTH.equals(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            return recentMonthStr(dateMax, unit.longValue(), MONTH_FORMAT, dateInfo);
        }
        if (DatePeriodEnum.YEAR.equals(dateInfo.getPeriod())) {
            LocalDate dateMax = LocalDate.now().minusDays(1);
            return recentMonthStr(dateMax, unit.longValue() * 12, MONTH_FORMAT, dateInfo);
        }

        return String.format("(%s >= '%s' and %s <= '%s')", dateInfo.getDateField(),
                LocalDate.now().minusDays(2), dateInfo.getDateField(),
                LocalDate.now().minusDays(1));
    }

    public String getDateWhereStr(DateConf dateInfo) {
        return getDateWhereStr(dateInfo, null);
    }

    public String getDateWhereStr(DateConf dateInfo, ItemDateResp dateDate) {
        if (Objects.isNull(dateInfo)) {
            return "";
        }
        String dateStr = "";
        switch (dateInfo.getDateMode()) {
            case BETWEEN:
                dateStr = betweenDateStr(dateInfo);
                break;
            case LIST:
                dateStr = listDateStr(dateInfo);
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

    public String getSysDateCol(DateConf dateInfo) {
        return dateInfo.getDateField();
    }

}

import { CHART_SECONDARY_COLOR, CLS_PREFIX, THEME_COLOR_LIST } from '../../../common/constants';
import {
  formatByDecimalPlaces,
  getFormattedValue,
  getMinMaxDate,
  groupByColumn,
  normalizeTrendData,
} from '../../../utils/utils';
import type { ECharts } from 'echarts';
import * as echarts from 'echarts';
import React, { useEffect, useRef, useState } from 'react';
import moment from 'moment';
import { ColumnType } from '../../../common/type';
import NoPermissionChart from '../NoPermissionChart';
import classNames from 'classnames';
import { isArray } from 'lodash';

type Props = {
  model?: string;
  dateColumnName: string;
  categoryColumnName: string;
  metricField: ColumnType;
  resultList: any[];
  triggerResize?: boolean;
  onApplyAuth?: (model: string) => void;
};

const MetricTrendChart: React.FC<Props> = ({
  model,
  dateColumnName,
  categoryColumnName,
  metricField,
  resultList,
  triggerResize,
  onApplyAuth,
}) => {
  const chartRef = useRef<any>();
  const [instance, setInstance] = useState<ECharts>();

  const renderChart = () => {
    let instanceObj: any;
    if (!instance) {
      instanceObj = echarts.init(chartRef.current);
      setInstance(instanceObj);
    } else {
      instanceObj = instance;
      instanceObj.clear();
    }

    const valueColumnName = metricField.nameEn;
    const dataSource = resultList.map((item: any) => {
      return {
        ...item,
        [dateColumnName]: Array.isArray(item[dateColumnName])
          ? moment(item[dateColumnName].join('')).format('MM-DD')
          : item[dateColumnName],
      };
    });

    const groupDataValue = groupByColumn(dataSource, categoryColumnName);
    const [startDate, endDate] = getMinMaxDate(dataSource, dateColumnName);
    const groupData = Object.keys(groupDataValue).reduce((result: any, key) => {
      result[key] =
        startDate &&
        endDate &&
        (dateColumnName.includes('date') || dateColumnName.includes('month'))
          ? normalizeTrendData(
              groupDataValue[key],
              dateColumnName,
              valueColumnName,
              startDate,
              endDate,
              dateColumnName.includes('month') ? 'months' : 'days'
            )
          : groupDataValue[key];
      return result;
    }, {});

    const sortedGroupKeys = Object.keys(groupData).sort((a, b) => {
      return (
        groupData[b][groupData[b].length - 1][valueColumnName] -
        groupData[a][groupData[a].length - 1][valueColumnName]
      );
    });

    const xData = groupData[sortedGroupKeys[0]]?.map((item: any) => {
      const date = isArray(item[dateColumnName])
        ? item[dateColumnName].join('-')
        : `${item[dateColumnName]}`;
      return date.length === 10 ? moment(date).format('MM-DD') : date;
    });

    instanceObj.setOption({
      legend: categoryColumnName && {
        left: 0,
        top: 0,
        icon: 'rect',
        itemWidth: 15,
        itemHeight: 5,
        type: 'scroll',
      },
      xAxis: {
        type: 'category',
        axisTick: {
          alignWithLabel: true,
          lineStyle: {
            color: CHART_SECONDARY_COLOR,
          },
        },
        axisLine: {
          lineStyle: {
            color: CHART_SECONDARY_COLOR,
          },
        },
        axisLabel: {
          showMaxLabel: true,
          color: '#999',
        },
        data: xData,
      },
      yAxis: {
        type: 'value',
        splitLine: {
          lineStyle: {
            opacity: 0.3,
          },
        },
        axisLabel: {
          formatter: function (value: any) {
            return value === 0
              ? 0
              : metricField.dataFormatType === 'percent'
              ? `${formatByDecimalPlaces(value, metricField.dataFormat?.decimalPlaces || 2)}%`
              : getFormattedValue(value);
          },
        },
      },
      tooltip: {
        trigger: 'axis',
        formatter: function (params: any[]) {
          const param = params[0];
          const valueLabels = params
            .sort((a, b) => b.value - a.value)
            .map(
              (item: any) =>
                `<div style="margin-top: 3px;">${
                  item.marker
                } <span style="display: inline-block; width: 70px; margin-right: 12px;">${
                  item.seriesName
                }</span><span style="display: inline-block; width: 90px; text-align: right; font-weight: 500;">${
                  item.value === ''
                    ? '-'
                    : metricField.dataFormatType === 'percent' ||
                      metricField.dataFormatType === 'decimal'
                    ? `${formatByDecimalPlaces(
                        item.value,
                        metricField.dataFormat?.decimalPlaces || 2
                      )}${metricField.dataFormatType === 'percent' ? '%' : ''}`
                    : getFormattedValue(item.value)
                }</span></div>`
            )
            .join('');
          return `${param.name}<br />${valueLabels}`;
        },
      },
      grid: {
        left: '1%',
        right: '4%',
        bottom: '3%',
        top: categoryColumnName ? 45 : 20,
        containLabel: true,
      },
      series: sortedGroupKeys.slice(0, 20).map((category, index) => {
        const data = groupData[category];
        return {
          type: 'line',
          name: categoryColumnName ? category : metricField.name,
          symbol: 'circle',
          showSymbol: data.length === 1,
          smooth: true,
          data: data.map((item: any) => {
            const value = item[valueColumnName];
            return (metricField.dataFormatType === 'percent' ||
              metricField.dataFormatType === 'decimal') &&
              metricField.dataFormat?.needMultiply100
              ? value * 100
              : value;
          }),
          color: THEME_COLOR_LIST[index],
        };
      }),
    });
    instanceObj.resize();
  };

  useEffect(() => {
    if (metricField.authorized) {
      renderChart();
    }
  }, [resultList, metricField]);

  useEffect(() => {
    if (triggerResize && instance) {
      instance.resize();
    }
  }, [triggerResize]);

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  const flowTrendChartClass = classNames(`${prefixCls}-flow-trend-chart`, {
    [`${prefixCls}-flow-trend-chart-single`]: !categoryColumnName,
  });

  return (
    <div>
      {!metricField.authorized ? (
        <NoPermissionChart model={model || ''} onApplyAuth={onApplyAuth} />
      ) : (
        <div className={flowTrendChartClass} ref={chartRef} />
      )}
    </div>
  );
};

export default MetricTrendChart;

import { CHART_SECONDARY_COLOR, CLS_PREFIX, THEME_COLOR_LIST } from '../../../common/constants';
import { getFormattedValue } from '../../../utils/utils';
import type { ECharts } from 'echarts';
import * as echarts from 'echarts';
import React, { useContext, useEffect, useRef, useState } from 'react';
import moment from 'moment';
import { ColumnType } from '../../../common/type';
import { isArray } from 'lodash';
import { ChartItemContext } from '../../ChatItem';
import { useExportByEcharts } from '../../../hooks';

type Props = {
  dateColumnName: string;
  metricFields: ColumnType[];
  resultList: any[];
  triggerResize?: boolean;
  chartType?: string;
  question: string;
};

const MultiMetricsTrendChart: React.FC<Props> = ({
  dateColumnName,
  metricFields,
  resultList,
  triggerResize,
  chartType,
  question,
}) => {
  const chartRef = useRef<any>();
  const instanceRef = useRef<ECharts>();
  const renderChart = () => {
    let instanceObj: any;
    if (!instanceRef.current) {
      instanceObj = echarts.init(chartRef.current);
      instanceRef.current = instanceObj;
    } else {
      instanceObj = instanceRef.current;
      instanceObj.clear();
    }

    const xData = resultList?.map((item: any) => {
      const date = isArray(item[dateColumnName])
        ? item[dateColumnName].join('-')
        : `${item[dateColumnName]}`;
      return date.length === 10 ? moment(date).format('MM-DD') : date;
    });

    instanceObj.setOption({
      legend: {
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
            return value === 0 ? 0 : getFormattedValue(value);
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
                  item.value === '' ? '-' : getFormattedValue(item.value)
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
        top: 45,
        containLabel: true,
      },
      series: metricFields.map((metricField, index) => {
        return {
          type: chartType,
          name: metricField.name,
          symbol: 'circle',
          showSymbol: resultList.length === 1,
          smooth: true,
          data: resultList.map((item: any) => {
            const value = item[metricField.bizName];
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

  const { downloadChartAsImage } = useExportByEcharts({
    instanceRef,
    question,
  });

  const { register } = useContext(ChartItemContext);

  register('downloadChartAsImage', downloadChartAsImage);

  useEffect(() => {
    renderChart();
  }, [resultList, chartType]);

  useEffect(() => {
    if (triggerResize && instanceRef.current) {
      instanceRef.current.resize();
    }
  }, [triggerResize]);

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  return <div className={`${prefixCls}-flow-trend-chart`} ref={chartRef} />;
};

export default MultiMetricsTrendChart;

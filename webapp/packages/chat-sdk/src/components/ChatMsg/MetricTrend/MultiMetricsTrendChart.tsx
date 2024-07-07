import { CHART_SECONDARY_COLOR, CLS_PREFIX, THEME_COLOR_LIST } from '../../../common/constants';
import { getFormattedValue } from '../../../utils/utils';
import type { ECharts } from 'echarts';
import * as echarts from 'echarts';
import React, { useEffect, useRef, useState } from 'react';
import moment from 'moment';
import { ColumnType } from '../../../common/type';
import { isArray } from 'lodash';

type Props = {
  dateColumnName: string;
  metricFields: ColumnType[];
  resultList: any[];
  triggerResize?: boolean;
  chartType?: string;
};

const MultiMetricsTrendChart: React.FC<Props> = ({
  dateColumnName,
  metricFields,
  resultList,
  triggerResize,
  chartType,
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

    const xData = resultList?.map((item: any) => {
      const date = isArray(item[dateColumnName])
        ? item[dateColumnName].join('-')
        : `${item[dateColumnName]}`;
      return date.length === 10 ? moment(date).format('MM-DD') : date;
    });

    const width = chartRef.current.offsetWidth

    instanceObj.setOption({
      title: metricFields.map((category,idx) => {
        const xRate = 100 / metricFields.length * idx;

        return {
          show: chartType === 'pie',
          text: `{a|${category + '' === 'undefined' ? '' : category}}` ,
          bottom: 0,
          left: xRate + '%',
          textStyle: {
            rich:{
              a: {
                fontSize: 14,
                color: '#666',
                align: 'center',
                width: width / metricFields.length,
              }
            }
          }
        }
      }),
      legend: {
        left: 0,
        top: 0,
        icon: 'rect',
        itemWidth: 15,
        itemHeight: 5,
        type: 'scroll',
      },
      xAxis: {
        show: ['bar', 'line'].includes(chartType!),
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
        show: ['bar', 'line'].includes(chartType!),
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
        show: ['bar', 'line'].includes(chartType!),
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
        const normalizedData = resultList.map((item: any) => {
          const value = item[metricField.nameEn];
          return (metricField.dataFormatType === 'percent' ||
            metricField.dataFormatType === 'decimal') &&
            metricField.dataFormat?.needMultiply100
            ? value * 100
            : value;
        })

        const xRate = 100 / metricFields.length / 2 + 100 / metricFields.length * index;

        if (chartType === 'pie') {
          return {
            type: 'pie',
            name: metricField.name,
            center: [`${xRate}%`, '50%'],
            data: xData.map(xItem => {
              return {
                name: xItem,
                value: normalizedData[xData.indexOf(xItem)],
                itemStyle: {
                  color: THEME_COLOR_LIST[xData.indexOf(xItem)],
                },
              }
            })
          };
        }
        return {
          type: chartType,
          name: metricField.name,
          symbol: 'circle',
          showSymbol: resultList.length === 1,
          smooth: true,
          data: resultList.map((item: any) => {
            const value = item[metricField.nameEn];
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
    renderChart();
  }, [resultList, chartType]);

  useEffect(() => {
    if (triggerResize && instance) {
      instance.resize();
    }
  }, [triggerResize]);

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  return <div className={`${prefixCls}-flow-trend-chart`} ref={chartRef} />;
};

export default MultiMetricsTrendChart;

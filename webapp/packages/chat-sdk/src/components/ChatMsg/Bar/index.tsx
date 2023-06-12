import { CHART_BLUE_COLOR, CHART_SECONDARY_COLOR, PREFIX_CLS } from '../../../common/constants';
import { MsgDataType } from '../../../common/type';
import { getChartLightenColor, getFormattedValue } from '../../../utils/utils';
import type { ECharts } from 'echarts';
import * as echarts from 'echarts';
import React, { useEffect, useRef, useState } from 'react';
import NoPermissionChart from '../NoPermissionChart';

type Props = {
  data: MsgDataType;
  onApplyAuth?: (domain: string) => void;
};

const BarChart: React.FC<Props> = ({ data, onApplyAuth }) => {
  const chartRef = useRef<any>();
  const [instance, setInstance] = useState<ECharts>();

  const { queryColumns, queryResults, entityInfo } = data;
  const categoryColumnName =
    queryColumns?.find(column => column.showType === 'CATEGORY')?.nameEn || '';
  const metricColumn = queryColumns?.find(column => column.showType === 'NUMBER');
  const metricColumnName = metricColumn?.nameEn || '';

  const renderChart = () => {
    let instanceObj: any;
    if (!instance) {
      instanceObj = echarts.init(chartRef.current);
      setInstance(instanceObj);
    } else {
      instanceObj = instance;
    }
    const data = (queryResults || []).sort(
      (a: any, b: any) => b[metricColumnName] - a[metricColumnName]
    );
    const xData = data.map(item => item[categoryColumnName]);
    instanceObj.setOption({
      legend: {
        left: 0,
        top: 0,
        icon: 'rect',
        itemWidth: 15,
        itemHeight: 5,
      },
      xAxis: {
        type: 'category',
        axisTick: {
          show: false,
        },
        axisLine: {
          lineStyle: {
            color: CHART_SECONDARY_COLOR,
          },
        },
        axisLabel: {
          width: 200,
          overflow: 'truncate',
          showMaxLabel: true,
          hideOverlap: false,
          interval: 0,
          color: '#333',
          rotate: 30,
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
            .map(
              (item: any) =>
                `<div style="margin-top: 3px;">${
                  item.marker
                } <span style="display: inline-block; width: 70px; margin-right: 12px;">${
                  item.seriesName
                }</span><span style="display: inline-block; width: 90px; text-align: right; font-weight: 500;">${getFormattedValue(
                  item.value
                )}</span></div>`
            )
            .join('');
          return `${param.name}<br />${valueLabels}`;
        },
      },
      grid: {
        left: '2%',
        right: '1%',
        bottom: '3%',
        top: 50,
        containLabel: true,
      },
      series: {
        type: 'bar',
        name: metricColumn?.name,
        barWidth: 20,
        itemStyle: {
          borderRadius: [10, 10, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: CHART_BLUE_COLOR },
            { offset: 1, color: getChartLightenColor(CHART_BLUE_COLOR) },
          ]),
        },
        label: {
          show: true,
          position: 'top',
          formatter: function ({ value }: any) {
            return getFormattedValue(value);
          },
        },
        data: data.map(item => {
          return item[metricColumn?.nameEn || ''];
        }),
      },
    });
    instanceObj.resize();
  };

  useEffect(() => {
    if (queryResults && queryResults.length > 0 && metricColumn?.authorized) {
      renderChart();
    }
  }, [queryResults]);

  if (!metricColumn?.authorized) {
    return (
      <NoPermissionChart
        domain={entityInfo?.domainInfo.name || ''}
        chartType="barChart"
        onApplyAuth={onApplyAuth}
      />
    );
  }

  return <div className={`${PREFIX_CLS}-bar`} ref={chartRef} />;
};

export default BarChart;

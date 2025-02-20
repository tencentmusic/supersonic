import { CHART_BLUE_COLOR, CHART_SECONDARY_COLOR, PREFIX_CLS } from '../../../common/constants';
import { MsgDataType } from '../../../common/type';
import {
  formatByDecimalPlaces,
  getChartLightenColor,
  getFormattedValue,
} from '../../../utils/utils';
import type { ECharts } from 'echarts';
import * as echarts from 'echarts';
import {
  forwardRef,
  ForwardRefRenderFunction,
  useContext,
  useEffect,
  useImperativeHandle,
  useRef,
} from 'react';
import NoPermissionChart from '../NoPermissionChart';
import { ColumnType } from '../../../common/type';
import { Spin } from 'antd';
import { ChartItemContext } from '../../ChatItem';
import { useExportByEcharts } from '../../../hooks';

type Props = {
  data: MsgDataType;
  question: string;
  triggerResize?: boolean;
  loading: boolean;
  metricField: ColumnType;
  onApplyAuth?: (model: string) => void;
};

const BarChart: React.FC<Props> = ({
  data,
  question,
  triggerResize,
  loading,
  metricField,
  onApplyAuth,
}) => {
  const chartRef = useRef<any>();
  const instanceRef = useRef<ECharts>();

  const { queryColumns, queryResults, entityInfo } = data;

  const categoryColumnName =
    queryColumns?.find(column => column.showType === 'CATEGORY')?.bizName || '';
  const metricColumn = queryColumns?.find(column => column.showType === 'NUMBER');
  const metricColumnName = metricColumn?.bizName || '';

  const renderChart = () => {
    let instanceObj: any;
    if (!instanceRef.current) {
      instanceObj = echarts.init(chartRef.current);
      instanceRef.current = instanceObj;
    } else {
      instanceObj = instanceRef.current;
    }
    const data = (queryResults || []);
    const xData = data.map(item =>
      item[categoryColumnName] !== undefined ? item[categoryColumnName] : '未知'
    );
    instanceObj.setOption({
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
        left: '2%',
        right: '1%',
        bottom: '3%',
        top: 20,
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
            return value === 0
              ? 0
              : metricField.dataFormatType === 'percent'
              ? `${formatByDecimalPlaces(value, metricField.dataFormat?.decimalPlaces || 2)}%`
              : getFormattedValue(value);
          },
        },
        data: data.map(item => {
          return item[metricColumn?.bizName || ''];
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

  useEffect(() => {
    if (triggerResize && instanceRef.current) {
      instanceRef.current.resize();
    }
  }, [triggerResize]);

  if (metricColumn && !metricColumn?.authorized) {
    return (
      <NoPermissionChart
        model={entityInfo?.dataSetInfo.name || ''}
        chartType="barChart"
        onApplyAuth={onApplyAuth}
      />
    );
  }

  const prefixCls = `${PREFIX_CLS}-bar`;

  const { downloadChartAsImage } = useExportByEcharts({
    instanceRef,
    question,
  });

  const { register } = useContext(ChartItemContext);

  register('downloadChartAsImage', downloadChartAsImage);

  return (
    <div>
      <div className={`${prefixCls}-top-bar`}>
        <div className={`${prefixCls}-indicator-name`}>{question}</div>
      </div>
      <Spin spinning={loading}>
        <div className={`${prefixCls}-chart`} ref={chartRef} />
      </Spin>
    </div>
  );
};

export default BarChart;

import { PREFIX_CLS, THEME_COLOR_LIST } from '../../../common/constants';
import { MsgDataType } from '../../../common/type';
import { formatByDecimalPlaces, getFormattedValue } from '../../../utils/utils';
import type { ECharts } from 'echarts';
import * as echarts from 'echarts';
import { useEffect, useRef } from 'react';
import { ColumnType } from '../../../common/type';

type Props = {
  data: MsgDataType;
  metricField: ColumnType;
  categoryField: ColumnType;
  triggerResize?: boolean;
};

const PieChart: React.FC<Props> = ({
  data,
  metricField,
  categoryField,
  triggerResize,
}) => {
  const chartRef = useRef<any>();
  const instanceRef = useRef<ECharts>();

  const { queryResults } = data;
  const categoryColumnName = categoryField?.bizName || '';
  const metricColumnName = metricField?.bizName || '';

  const renderChart = () => {
    let instanceObj: any;
    if (!instanceRef.current) {
      instanceObj = echarts.init(chartRef.current);
      instanceRef.current = instanceObj;
    } else {
      instanceObj = instanceRef.current;
    }

    const data = queryResults || [];
    const seriesData = data.map((item, index) => {
      const value = item[metricColumnName];
      const name = item[categoryColumnName] !== undefined ? item[categoryColumnName] : '未知';
      return {
        name,
        value,
        itemStyle: {
          color: THEME_COLOR_LIST[index % THEME_COLOR_LIST.length],
        },
      };
    });

    instanceObj.setOption({
      tooltip: {
        trigger: 'item',
        formatter: function (params: any) {
          const value = params.value;
          return `${params.name}: ${
            metricField.dataFormatType === 'percent'
              ? `${formatByDecimalPlaces(
                  metricField.dataFormat?.needMultiply100 ? +value * 100 : value,
                  metricField.dataFormat?.decimalPlaces || 2
                )}%`
              : getFormattedValue(value)
          }`;
        },
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        type: 'scroll',
        data: seriesData.map(item => item.name),
        selectedMode: true,
        textStyle: {
          color: '#666',
        },
      },
      series: [
        {
          name: '占比',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2,
          },
          label: {
            show: false,
            position: 'center',
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '14',
              fontWeight: 'bold',
            },
          },
          labelLine: {
            show: false,
          },
          data: seriesData,
        },
      ],
    });
    instanceObj.resize();
  };

  useEffect(() => {
    if (queryResults && queryResults.length > 0) {
      renderChart();
    }
  }, [queryResults, metricField, categoryField]);

  useEffect(() => {
    if (triggerResize && instanceRef.current) {
      instanceRef.current.resize();
    }
  }, [triggerResize]);

  return <div className={`${PREFIX_CLS}-pie-chart`} ref={chartRef} />;
};

export default PieChart;

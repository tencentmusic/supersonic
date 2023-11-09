import { CHART_SECONDARY_COLOR } from '@/common/constants';
import {
  formatByDecimalPlaces,
  formatByPercentageData,
  getFormattedValueData,
} from '@/utils/utils';
import { Skeleton, Button, Tooltip } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import type { ECharts } from 'echarts';
import * as echarts from 'echarts';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { groupBy, sum } from 'lodash';
import styles from '../style.less';
import moment from 'moment';

type Props = {
  title?: string;
  tip?: string;
  data: any[];
  fields: any[];
  loading: boolean;
  isPer?: boolean;
  isPercent?: boolean;
  dateFieldName?: string;
  dateFormat?: string;
  height?: number;
  renderType?: string;
  decimalPlaces?: number;
  rowNumber?: number;
  groupByDimensionFieldName?: string;
  onDownload?: () => void;
};

const TrendChart: React.FC<Props> = ({
  title,
  tip,
  data,
  fields,
  loading,
  isPer,
  isPercent,
  dateFieldName = 'sys_imp_date',
  // columnFieldName,
  rowNumber = 0,
  groupByDimensionFieldName,
  dateFormat,
  height,
  renderType,
  decimalPlaces,
  onDownload,
}) => {
  const chartRef = useRef<any>();
  const [instance, setInstance] = useState<ECharts>();
  const renderChart = useCallback(() => {
    let instanceObj: ECharts;
    if (!instance) {
      instanceObj = echarts.init(chartRef.current);
      setInstance(instanceObj);
    } else {
      instanceObj = instance;
      if (renderType === 'clear') {
        instanceObj.clear();
      }
    }
    const xData = Array.from(
      new Set(
        data
          .map((item) =>
            moment(`${dateFieldName && item[dateFieldName]}`).format(dateFormat ?? 'YYYY-MM-DD'),
          )
          .sort((a, b) => {
            return moment(a).valueOf() - moment(b).valueOf();
          }),
      ),
    );

    const formatterSeriesData = () => {
      if (groupByDimensionFieldName) {
        const groupByMap = groupBy(data, groupByDimensionFieldName);

        const seriesData = Object.keys(groupByMap).map((fieldKey: string) => {
          const dimensionDataList = groupByMap[fieldKey];
          const dimensionDataMapByDate = dimensionDataList.reduce((itemMap, item) => {
            itemMap[item[dateFieldName]] = { ...item };
            return itemMap;
          }, {});
          const dataList = xData.reduce((itemData: any[], dateString) => {
            const dimensionDataMapItem = dimensionDataMapByDate[dateString];
            if (dimensionDataMapItem) {
              itemData.push(dimensionDataMapItem[fields?.[0]?.column]);
            } else {
              itemData.push(0);
            }
            return itemData;
          }, []);
          return {
            type: 'line',
            name: fieldKey,
            symbol: 'circle',
            smooth: true,
            sortNum: sum(dataList),
            data: dataList,
          };
        });
        if (rowNumber) {
          return seriesData.sort((a, b) => b.sortNum - a.sortNum).slice(0, rowNumber);
        }
        return seriesData;
      }
      const seriesData = fields.map((field) => {
        const fieldData = {
          type: 'line',
          name: field.name,
          symbol: 'circle',
          showSymbol: data.length === 1,
          smooth: true,
          data: data.reduce((itemData, item) => {
            const target = item[field.column];
            if (target) {
              itemData.push(target);
            }
            return itemData;
          }, []),
        };
        return fieldData;
      });
      return seriesData;
    };
    const seriesData = formatterSeriesData();

    instanceObj.setOption({
      legend: {
        left: 0,
        top: 0,
        icon: 'rect',
        itemWidth: 15,
        itemHeight: 5,
        selected: fields.reduce((result, item) => {
          if (item.selected === false) {
            result[item.name] = false;
          }
          return result;
        }, {}),
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
              : isPer
              ? `${formatByDecimalPlaces(value, decimalPlaces ?? 0)}%`
              : isPercent
              ? formatByPercentageData(value, decimalPlaces ?? 0)
              : getFormattedValueData(value);
          },
        },
      },
      tooltip: {
        trigger: 'axis',
        formatter: function (params: any) {
          const param = params[0];
          const valueLabels = params
            .map(
              (item: any) =>
                `<div style="margin-top: 3px;">${
                  item.marker
                } <span style="display: inline-block; width: 70px; margin-right: 5px;">${
                  item.seriesName
                }</span><span style="display: inline-block; width: 90px; text-align: right; font-weight: 500;">${
                  item.value === ''
                    ? '-'
                    : isPer
                    ? `${formatByDecimalPlaces(item.value, decimalPlaces ?? 2)}%`
                    : isPercent
                    ? formatByPercentageData(item.value, decimalPlaces ?? 2)
                    : getFormattedValueData(item.value)
                }</span></div>`,
            )
            .join('');
          return `${param.name}<br />${valueLabels}`;
        },
      },
      grid: {
        left: '1%',
        right: '4%',
        bottom: '3%',
        top: height && height < 300 ? 45 : 60,
        containLabel: true,
      },
      series: seriesData,
    });
    instanceObj.resize();
  }, [
    data,
    fields,
    instance,
    isPer,
    isPercent,
    dateFieldName,
    decimalPlaces,
    renderType,
    rowNumber,
    groupByDimensionFieldName,
  ]);

  useEffect(() => {
    if (!loading) {
      renderChart();
    }
  }, [renderChart, loading, data]);

  return (
    <div className={styles.trendChart}>
      {title && (
        <div className={styles.top}>
          <div className={styles.title}>{title}</div>
          {onDownload && (
            <Tooltip title="下载">
              <Button shape="circle" className={styles.downloadBtn} onClick={onDownload}>
                <DownloadOutlined />
              </Button>
            </Tooltip>
          )}
        </div>
      )}
      <Skeleton
        className={styles.chart}
        style={{ height, display: loading ? 'table' : 'none' }}
        paragraph={{ rows: height && height > 300 ? 15 : 6 }}
      />
      <div
        className={styles.chart}
        style={{ height, display: !loading ? 'block' : 'none' }}
        ref={chartRef}
      />
    </div>
  );
};

export default TrendChart;

import { useEffect, useState } from 'react';
import { CLS_PREFIX, DATE_TYPES } from '../../../common/constants';
import { ColumnType, DrillDownDimensionType, FieldType, MsgDataType } from '../../../common/type';
import { isMobile } from '../../../utils/utils';
import { queryData } from '../../../service';
import MetricTrendChart from './MetricTrendChart';
import classNames from 'classnames';
import { Spin } from 'antd';
import Table from '../Table';
import DrillDownDimensions from '../../DrillDownDimensions';
import MetricInfo from './MetricInfo';
import FilterSection from '../FilterSection';
import moment from 'moment';

type Props = {
  data: MsgDataType;
  triggerResize?: boolean;
  onApplyAuth?: (domain: string) => void;
};

const MetricTrend: React.FC<Props> = ({ data, triggerResize, onApplyAuth }) => {
  const { queryColumns, queryResults, entityInfo, chatContext, queryMode, aggregateInfo } = data;

  const dateOptions = DATE_TYPES[chatContext?.dateInfo?.period] || DATE_TYPES.DAY;
  const initialDateOption = dateOptions.find(
    (option: any) => option.value === chatContext?.dateInfo?.unit
  )?.value;

  const [columns, setColumns] = useState<ColumnType[]>(queryColumns);
  const currentMetricField = columns.find((column: any) => column.showType === 'NUMBER');

  const [activeMetricField, setActiveMetricField] = useState<FieldType>(chatContext.metrics?.[0]);
  const [dataSource, setDataSource] = useState<any[]>(queryResults);
  const [currentDateOption, setCurrentDateOption] = useState<number>(initialDateOption);
  const [drillDownDimension, setDrillDownDimension] = useState<DrillDownDimensionType>();
  const [loading, setLoading] = useState(false);

  const dateField: any = columns.find(
    (column: any) => column.showType === 'DATE' || column.type === 'DATE'
  );
  const dateColumnName = dateField?.nameEn || '';
  const categoryColumnName =
    columns.find((column: any) => column.showType === 'CATEGORY')?.nameEn || '';

  useEffect(() => {
    setDataSource(queryResults);
  }, [queryResults]);

  const onLoadData = async (value: any) => {
    setLoading(true);
    const { data } = await queryData({
      ...chatContext,
      ...value,
    });
    setLoading(false);
    if (data.code === 200) {
      setColumns(data.data?.queryColumns || []);
      setDataSource(data.data?.queryResults || []);
    }
  };

  const selectDateOption = (dateOption: number) => {
    setCurrentDateOption(dateOption);
    const endDate = moment().subtract(1, 'days').format('YYYY-MM-DD');
    const startDate = moment(endDate)
      .subtract(dateOption - 1, 'days')
      .format('YYYY-MM-DD');
    onLoadData({
      metrics: [activeMetricField],
      dimensions: drillDownDimension
        ? [...(chatContext.dimensions || []), drillDownDimension]
        : undefined,
      dateInfo: {
        ...chatContext?.dateInfo,
        startDate,
        endDate,
        unit: dateOption,
      },
    });
  };

  const onSwitchMetric = (metricField: FieldType) => {
    setActiveMetricField(metricField);
    onLoadData({
      dateInfo: { ...chatContext.dateInfo, unit: currentDateOption || chatContext.dateInfo.unit },
      dimensions: drillDownDimension
        ? [...(chatContext.dimensions || []), drillDownDimension]
        : undefined,
      metrics: [metricField],
    });
  };

  const onSelectDimension = (dimension?: DrillDownDimensionType) => {
    setDrillDownDimension(dimension);
    onLoadData({
      dateInfo: { ...chatContext.dateInfo, unit: currentDateOption || chatContext.dateInfo.unit },
      metrics: [activeMetricField],
      dimensions:
        dimension === undefined ? undefined : [...(chatContext.dimensions || []), dimension],
    });
  };

  if (!currentMetricField) {
    return null;
  }

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-charts`}>
        {chatContext.metrics.length > 0 && (
          <div className={`${prefixCls}-metric-fields`}>
            {chatContext.metrics.map((metricField: FieldType) => {
              const metricFieldClass = classNames(`${prefixCls}-metric-field`, {
                [`${prefixCls}-metric-field-active`]:
                  activeMetricField?.bizName === metricField.bizName &&
                  chatContext.metrics.length > 1,
                [`${prefixCls}-metric-field-single`]: chatContext.metrics.length === 1,
              });
              return (
                <div
                  className={metricFieldClass}
                  key={metricField.bizName}
                  onClick={() => {
                    if (chatContext.metrics.length > 1) {
                      onSwitchMetric(metricField);
                    }
                  }}
                >
                  {metricField.name}
                </div>
              );
            })}
          </div>
        )}
        {aggregateInfo?.metricInfos?.length > 0 && <MetricInfo aggregateInfo={aggregateInfo} />}
        <FilterSection chatContext={chatContext} />
        <div className={`${prefixCls}-date-options`}>
          {dateOptions.map((dateOption: { label: string; value: number }, index: number) => {
            const dateOptionClass = classNames(`${prefixCls}-date-option`, {
              [`${prefixCls}-date-active`]: dateOption.value === currentDateOption,
              [`${prefixCls}-date-mobile`]: isMobile,
            });
            return (
              <>
                <div
                  key={dateOption.value}
                  className={dateOptionClass}
                  onClick={() => {
                    selectDateOption(dateOption.value);
                  }}
                >
                  {dateOption.label}
                  {dateOption.value === currentDateOption && (
                    <div className={`${prefixCls}-active-identifier`} />
                  )}
                </div>
                {index !== dateOptions.length - 1 && (
                  <div className={`${prefixCls}-date-option-divider`} />
                )}
              </>
            );
          })}
        </div>
        <Spin spinning={loading}>
          {dataSource?.length === 1 ? (
            <Table data={{ ...data, queryResults: dataSource }} onApplyAuth={onApplyAuth} />
          ) : (
            <MetricTrendChart
              domain={entityInfo?.domainInfo.name}
              dateColumnName={dateColumnName}
              categoryColumnName={categoryColumnName}
              metricField={currentMetricField}
              resultList={dataSource}
              triggerResize={triggerResize}
              onApplyAuth={onApplyAuth}
            />
          )}
        </Spin>
        {(queryMode === 'METRIC_DOMAIN' || queryMode === 'METRIC_FILTER') && (
          <DrillDownDimensions
            domainId={chatContext.domainId}
            drillDownDimension={drillDownDimension}
            dimensionFilters={chatContext.dimensionFilters}
            onSelectDimension={onSelectDimension}
          />
        )}
      </div>
    </div>
  );
};

export default MetricTrend;

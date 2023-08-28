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

type Props = {
  data: MsgDataType;
  chartIndex: number;
  triggerResize?: boolean;
  onApplyAuth?: (model: string) => void;
};

const MetricTrend: React.FC<Props> = ({ data, chartIndex, triggerResize, onApplyAuth }) => {
  const { entityInfo, chatContext, queryMode } = data;
  const { dateInfo, dimensionFilters, elementMatches } = chatContext || {};
  const { dateMode, unit } = dateInfo || {};
  const dateOptions = DATE_TYPES[chatContext?.dateInfo?.period] || DATE_TYPES.DAY;

  const [columns, setColumns] = useState<ColumnType[]>([]);
  const [activeMetricField, setActiveMetricField] = useState<FieldType>();
  const [dataSource, setDataSource] = useState<any[]>([]);
  const [currentDateOption, setCurrentDateOption] = useState<number>();
  const [dimensions, setDimensions] = useState<FieldType[]>();
  const [drillDownDimension, setDrillDownDimension] = useState<DrillDownDimensionType>();
  const [aggregateInfoValue, setAggregateInfoValue] = useState<any>();
  const [dateModeValue, setDateModeValue] = useState<any>();
  const [loading, setLoading] = useState(false);

  const dateField: any = columns.find(
    (column: any) => column.showType === 'DATE' || column.type === 'DATE'
  );
  const dateColumnName = dateField?.nameEn || '';
  const categoryColumnName =
    columns.find((column: any) => column.showType === 'CATEGORY')?.nameEn || '';

  const entityId = dimensionFilters?.length > 0 ? dimensionFilters[0].value : undefined;
  const entityName = elementMatches?.find((item: any) => item.element?.type === 'ID')?.element
    ?.name;

  const isEntityMode =
    (queryMode === 'ENTITY_LIST_FILTER' || queryMode === 'METRIC_ENTITY') &&
    typeof entityId === 'string' &&
    entityName !== undefined;

  useEffect(() => {
    const { queryColumns, queryResults, chatContext, aggregateInfo } = data;

    const initialDateOption = dateOptions.find((option: any) => {
      return dateMode === 'RECENT' && option.value === unit;
    })?.value;

    setColumns(queryColumns || []);
    setActiveMetricField(chatContext?.metrics?.[0]);
    setDataSource(queryResults);
    setCurrentDateOption(initialDateOption);
    setDimensions(chatContext?.dimensions);
    setDrillDownDimension(undefined);
    setAggregateInfoValue(aggregateInfo);
    setDateModeValue(chatContext?.dateInfo?.dateMode);
  }, [data]);

  useEffect(() => {
    if (queryMode === 'METRIC_GROUPBY') {
      const dimensionValue = chatContext?.dimensions?.find(
        dimension => dimension.type === 'DIMENSION'
      );
      setDrillDownDimension(dimensionValue);
      setDimensions(
        chatContext?.dimensions?.filter(dimension => dimension.id !== dimensionValue?.id)
      );
    }
  }, []);

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
      setAggregateInfoValue(data.data?.aggregateInfo);
    }
  };

  const selectDateOption = (dateOption: number) => {
    setCurrentDateOption(dateOption);
    setDateModeValue('RECENT');
    onLoadData({
      metrics: [activeMetricField],
      dimensions: drillDownDimension ? [...(dimensions || []), drillDownDimension] : undefined,
      dateInfo: {
        ...chatContext?.dateInfo,
        dateMode: 'RECENT',
        unit: dateOption,
      },
    });
  };

  const onSwitchMetric = (metricField: FieldType) => {
    setActiveMetricField(metricField);
    onLoadData({
      dateInfo: {
        ...chatContext.dateInfo,
        dateMode: dateModeValue,
        unit: currentDateOption || chatContext.dateInfo.unit,
      },
      dimensions: drillDownDimension ? [...(dimensions || []), drillDownDimension] : undefined,
      metrics: [metricField],
    });
  };

  const onSelectDimension = (dimension?: DrillDownDimensionType) => {
    setDrillDownDimension(dimension);
    onLoadData({
      dateInfo: {
        ...chatContext.dateInfo,
        dateMode: dateModeValue,
        unit: currentDateOption || chatContext.dateInfo.unit,
      },
      metrics: [activeMetricField],
      dimensions: dimension === undefined ? undefined : [...(dimensions || []), dimension],
    });
  };

  const currentMetricField = columns.find((column: any) => column.showType === 'NUMBER');

  if (!currentMetricField) {
    return null;
  }

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  const hasFilterSection = dimensionFilters?.length > 0;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-charts`}>
        <div className={`${prefixCls}-top-bar`}>
          {chatContext.metrics.length > 0 && (
            <div className={`${prefixCls}-metric-fields`}>
              {chatContext.metrics.slice(0, 5).map((metricField: FieldType) => {
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
          {(hasFilterSection || drillDownDimension) && (
            <div className={`${prefixCls}-filter-section-wrapper`}>
              (
              <div className={`${prefixCls}-filter-section`}>
                <FilterSection chatContext={chatContext} />
                {drillDownDimension && (
                  <div className={`${prefixCls}-filter-item`}>
                    <div className={`${prefixCls}-filter-item-label`}>下钻维度：</div>
                    <div className={`${prefixCls}-filter-item-value`}>
                      {drillDownDimension.name}
                    </div>
                  </div>
                )}
              </div>
              )
            </div>
          )}
        </div>
        <Spin spinning={loading}>
          <div className={`${prefixCls}-content`}>
            {aggregateInfoValue?.metricInfos?.length > 0 && (
              <MetricInfo aggregateInfo={aggregateInfoValue} />
            )}
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
            {dataSource?.length === 1 || chartIndex % 2 === 1 ? (
              <Table data={{ ...data, queryResults: dataSource }} onApplyAuth={onApplyAuth} />
            ) : (
              <MetricTrendChart
                model={entityInfo?.modelInfo.name}
                dateColumnName={dateColumnName}
                categoryColumnName={categoryColumnName}
                metricField={currentMetricField}
                resultList={dataSource}
                triggerResize={triggerResize}
                onApplyAuth={onApplyAuth}
              />
            )}
          </div>
          {queryMode.includes('METRIC') && !isEntityMode && (
            <DrillDownDimensions
              modelId={chatContext.modelId}
              drillDownDimension={drillDownDimension}
              dimensionFilters={chatContext.dimensionFilters}
              onSelectDimension={onSelectDimension}
            />
          )}
        </Spin>
      </div>
    </div>
  );
};

export default MetricTrend;

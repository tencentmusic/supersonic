import { useEffect, useState } from 'react';
import { CLS_PREFIX, DATE_TYPES } from '../../../common/constants';
import { ColumnType, FieldType, MsgDataType } from '../../../common/type';
import { isMobile } from '../../../utils/utils';
import { queryData } from '../../../service';
import MetricTrendChart from './MetricTrendChart';
import classNames from 'classnames';
import { Spin } from 'antd';
import Table from '../Table';

type Props = {
  data: MsgDataType;
  triggerResize?: boolean;
  onApplyAuth?: (domain: string) => void;
  onCheckMetricInfo?: (data: any) => void;
};

const MetricTrend: React.FC<Props> = ({ data, triggerResize, onApplyAuth, onCheckMetricInfo }) => {
  const { queryColumns, queryResults, entityInfo, chatContext } = data;
  const [columns, setColumns] = useState<ColumnType[]>(queryColumns);
  const currentMetricField = columns.find((column: any) => column.showType === 'NUMBER');

  const [activeMetricField, setActiveMetricField] = useState<FieldType>(chatContext.metrics?.[0]);
  const [dataSource, setDataSource] = useState<any[]>(queryResults);
  const [currentDateOption, setCurrentDateOption] = useState<number>();
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

  const dateOptions = DATE_TYPES[chatContext?.dateInfo?.period] || DATE_TYPES[0];

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
    onLoadData({
      metrics: [activeMetricField],
      dateInfo: { ...chatContext?.dateInfo, unit: dateOption },
    });
  };

  const onSwitchMetric = (metricField: FieldType) => {
    setActiveMetricField(metricField);
    onLoadData({
      dateInfo: { ...chatContext.dateInfo, unit: currentDateOption || chatContext.dateInfo.unit },
      metrics: [metricField],
    });
  };

  if (!currentMetricField) {
    return null;
  }

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-charts`}>
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
                  {/* <SemanticInfoPopover
                    classId={chatContext.domainId}
                    uniqueId={metricField.bizName}
                    onDetailBtnClick={onCheckMetricInfo}
                  > */}
                  {metricField.name}
                  {/* </SemanticInfoPopover> */}
                </div>
              );
            })}
          </div>
        )}
        {dataSource?.length === 1 ? (
          <Table data={data} onApplyAuth={onApplyAuth} />
        ) : (
          <Spin spinning={loading}>
            <MetricTrendChart
              domain={entityInfo?.domainInfo.name}
              dateColumnName={dateColumnName}
              categoryColumnName={categoryColumnName}
              metricField={currentMetricField}
              resultList={dataSource}
              triggerResize={triggerResize}
              onApplyAuth={onApplyAuth}
            />
          </Spin>
        )}
      </div>
    </div>
  );
};

export default MetricTrend;

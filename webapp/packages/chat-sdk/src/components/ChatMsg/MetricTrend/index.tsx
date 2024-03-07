import { CLS_PREFIX } from '../../../common/constants';
import { DrillDownDimensionType, FieldType, MsgDataType } from '../../../common/type';
import { isMobile } from '../../../utils/utils';
import MetricTrendChart from './MetricTrendChart';
import { Spin, Select } from 'antd';
import Table from '../Table';
import MetricInfo from './MetricInfo';
import DateOptions from '../DateOptions';
import MultiMetricsTrendChart from './MultiMetricsTrendChart';
import { useState } from 'react';

const metricChartSelectOptions = [
  {
    value: 'line',
    label: '折线图',
  },
  {
    value: 'bar',
    label: '柱状图',
  },
];

type Props = {
  data: MsgDataType;
  chartIndex: number;
  triggerResize?: boolean;
  loading: boolean;
  activeMetricField?: FieldType;
  drillDownDimension?: DrillDownDimensionType;
  currentDateOption?: number;
  onApplyAuth?: (model: string) => void;
  onSelectDateOption: (value: number) => void;
};

const MetricTrend: React.FC<Props> = ({
  data,
  chartIndex,
  triggerResize,
  loading,
  activeMetricField,
  drillDownDimension,
  currentDateOption,
  onApplyAuth,
  onSelectDateOption,
}) => {
  const { queryColumns, queryResults, aggregateInfo, entityInfo, chatContext } = data;
  const [chartType, setChartType] = useState('line');

  const dateField: any = queryColumns?.find(
    (column: any) => column.showType === 'DATE' || column.type === 'DATE'
  );
  const dateColumnName = dateField?.nameEn || '';
  const categoryColumnName =
    queryColumns?.find((column: any) => column.showType === 'CATEGORY')?.nameEn || '';
  const metricFields = queryColumns?.filter((column: any) => column.showType === 'NUMBER');

  const currentMetricField = queryColumns?.find((column: any) => column.showType === 'NUMBER');

  if (!currentMetricField) {
    return null;
  }

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-charts`}>
        {metricFields?.length === 1 && (
          <div className={`${prefixCls}-top-bar`}>
            <div
              className={`${prefixCls}-metric-fields ${prefixCls}-metric-field-single`}
              key={activeMetricField?.bizName}
            >
              {activeMetricField?.name}
            </div>
          </div>
        )}
        <Spin spinning={loading}>
          <div className={`${prefixCls}-content`}>
            {!isMobile &&
              aggregateInfo?.metricInfos?.length > 0 &&
              drillDownDimension === undefined && (
                <MetricInfo aggregateInfo={aggregateInfo} currentMetricField={currentMetricField} />
              )}
            <div className={`${prefixCls}-select-options`}>
              <DateOptions
                chatContext={chatContext}
                currentDateOption={currentDateOption}
                onSelectDateOption={onSelectDateOption}
              />
              <div>
                <Select
                  defaultValue="line"
                  bordered={false}
                  options={metricChartSelectOptions}
                  onChange={(value: string) => setChartType(value)}
                />
              </div>
            </div>
            {queryResults?.length === 1 || chartIndex % 2 === 1 ? (
              <Table data={{ ...data, queryResults }} onApplyAuth={onApplyAuth} />
            ) : metricFields.length > 1 ? (
              <MultiMetricsTrendChart
                dateColumnName={dateColumnName}
                metricFields={metricFields}
                resultList={queryResults}
                triggerResize={triggerResize}
                chartType={chartType}
              />
            ) : (
              <MetricTrendChart
                model={entityInfo?.dataSetInfo.name}
                dateColumnName={dateColumnName}
                categoryColumnName={categoryColumnName}
                metricField={currentMetricField}
                resultList={queryResults}
                triggerResize={triggerResize}
                onApplyAuth={onApplyAuth}
                chartType={chartType}
              />
            )}
          </div>
        </Spin>
      </div>
    </div>
  );
};

export default MetricTrend;

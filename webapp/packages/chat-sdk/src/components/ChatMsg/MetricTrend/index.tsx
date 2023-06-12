import { useEffect, useState } from 'react';
import { CLS_PREFIX, DATE_TYPES } from '../../../common/constants';
import { ColumnType, MsgDataType } from '../../../common/type';
import { groupByColumn, isMobile } from '../../../utils/utils';
import { queryData } from '../../../service';
import MetricTrendChart from './MetricTrendChart';
import classNames from 'classnames';
import { Spin } from 'antd';
import Table from '../Table';
import SemanticInfoPopover from '../SemanticInfoPopover';

type Props = {
  data: MsgDataType;
  onApplyAuth?: (domain: string) => void;
  onCheckMetricInfo?: (data: any) => void;
};

const MetricTrend: React.FC<Props> = ({ data, onApplyAuth, onCheckMetricInfo }) => {
  const { queryColumns, queryResults, entityInfo, chatContext } = data;
  const [columns, setColumns] = useState<ColumnType[]>(queryColumns);
  const metricFields = columns.filter((column: any) => column.showType === 'NUMBER') || [];

  const [currentMetricField, setCurrentMetricField] = useState<ColumnType>(metricFields[0]);
  const [onlyOneDate, setOnlyOneDate] = useState(false);
  const [trendData, setTrendData] = useState(data);
  const [dataSource, setDataSource] = useState<any[]>(queryResults);
  const [mergeMetric, setMergeMetric] = useState(false);
  const [currentDateOption, setCurrentDateOption] = useState<number>();
  const [loading, setLoading] = useState(false);

  const dateField: any = columns.find(
    (column: any) => column.showType === 'DATE' || column.type === 'DATE'
  );
  const dateColumnName = dateField?.nameEn || '';
  const categoryColumnName =
    columns.find((column: any) => column.showType === 'CATEGORY')?.nameEn || '';

  const getColumns = () => {
    const categoryFieldData = groupByColumn(dataSource, categoryColumnName);
    const result = [dateField];
    const columnsValue = Object.keys(categoryFieldData).map(item => ({
      authorized: currentMetricField.authorized,
      name: item !== 'undefined' ? item : currentMetricField.name,
      nameEn: `${item}${currentMetricField.name}`,
      showType: 'NUMBER',
      type: 'NUMBER',
    }));
    return result.concat(columnsValue);
  };

  const getResultList = () => {
    return [
      {
        [dateField.nameEn]: dataSource[0][dateField.nameEn],
        ...dataSource.reduce((result, item) => {
          result[`${item[categoryColumnName]}${currentMetricField.name}`] =
            item[currentMetricField.nameEn];
          return result;
        }, {}),
      },
    ];
  };

  useEffect(() => {
    setDataSource(queryResults);
  }, [queryResults]);

  useEffect(() => {
    let onlyOneDateValue = false;
    let dataValue = trendData;
    if (dateColumnName && dataSource.length > 0) {
      const dateFieldData = groupByColumn(dataSource, dateColumnName);
      onlyOneDateValue =
        Object.keys(dateFieldData).length === 1 && Object.keys(dateFieldData)[0] !== undefined;
      if (onlyOneDateValue) {
        if (categoryColumnName !== '') {
          dataValue = {
            ...trendData,
            queryColumns: getColumns(),
            queryResults: getResultList(),
          };
        } else {
          setMergeMetric(true);
        }
      }
    }
    setOnlyOneDate(onlyOneDateValue);
    setTrendData(dataValue);
  }, [currentMetricField]);

  const dateOptions = DATE_TYPES[chatContext.dateInfo?.period] || DATE_TYPES[0];

  const onLoadData = async (value: number) => {
    setLoading(true);
    const { data } = await queryData({
      ...chatContext,
      dateInfo: { ...chatContext.dateInfo, unit: value },
    });
    setLoading(false);
    if (data.code === 200) {
      setColumns(data.data?.queryColumns || []);
      setDataSource(data.data?.queryResults || []);
    }
  };

  const selectDateOption = (dateOption: number) => {
    setCurrentDateOption(dateOption);
    // const { domainName, dimensions, metrics, aggType, filters } = chatContext || {};
    // const dimensionSection = dimensions?.join('、') || '';
    // const metricSection = metrics?.join('、') || '';
    // const aggregatorSection = aggType || '';
    // const filterSection = filters
    //   .reduce((result, dimensionName) => {
    //     result = result.concat(dimensionName);
    //     return result;
    //   }, [])
    //   .join('、');
    onLoadData(dateOption);
  };

  if (metricFields.length === 0) {
    return null;
  }

  const prefixCls = `${CLS_PREFIX}-metric-trend`;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-charts`}>
        {!onlyOneDate && (
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
        )}
        {metricFields.length > 1 && !mergeMetric && (
          <div className={`${prefixCls}-metric-fields`}>
            {metricFields.map((metricField: ColumnType) => {
              const metricFieldClass = classNames(`${prefixCls}-metric-field`, {
                [`${prefixCls}-metric-field-active`]:
                  currentMetricField?.nameEn === metricField.nameEn,
              });
              return (
                <div
                  className={metricFieldClass}
                  key={metricField.nameEn}
                  onClick={() => {
                    setCurrentMetricField(metricField);
                  }}
                >
                  <SemanticInfoPopover
                    classId={chatContext.domainId}
                    uniqueId={metricField.nameEn}
                    onDetailBtnClick={onCheckMetricInfo}
                  >
                    {metricField.name}
                  </SemanticInfoPopover>
                </div>
              );
            })}
          </div>
        )}
        {onlyOneDate ? (
          <Table data={trendData} onApplyAuth={onApplyAuth} />
        ) : (
          <Spin spinning={loading}>
            <MetricTrendChart
              domain={entityInfo?.domainInfo.name}
              dateColumnName={dateColumnName}
              categoryColumnName={categoryColumnName}
              metricField={currentMetricField}
              resultList={dataSource}
              onApplyAuth={onApplyAuth}
            />
          </Spin>
        )}
      </div>
    </div>
  );
};

export default MetricTrend;

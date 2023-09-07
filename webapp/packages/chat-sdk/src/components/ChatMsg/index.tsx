import Bar from './Bar';
import MetricCard from './MetricCard';
import MetricTrend from './MetricTrend';
import Table from './Table';
import { ColumnType, DrillDownDimensionType, MsgDataType } from '../../common/type';
import { useEffect, useState } from 'react';
import { queryData } from '../../service';
import classNames from 'classnames';
import { PREFIX_CLS } from '../../common/constants';
import Text from './Text';

type Props = {
  data: MsgDataType;
  chartIndex: number;
  triggerResize?: boolean;
};

const ChatMsg: React.FC<Props> = ({ data, chartIndex, triggerResize }) => {
  const { queryColumns, queryResults, chatContext, queryMode } = data;

  const [columns, setColumns] = useState<ColumnType[]>();
  const [referenceColumn, setReferenceColumn] = useState<ColumnType>();
  const [dataSource, setDataSource] = useState<any[]>(queryResults);

  const [drillDownDimension, setDrillDownDimension] = useState<DrillDownDimensionType>();
  const [loading, setLoading] = useState(false);

  const prefixCls = `${PREFIX_CLS}-chat-msg`;

  const updateColummns = (queryColumnsValue: ColumnType[]) => {
    const referenceColumn = queryColumnsValue.find(item => item.showType === 'more');
    setReferenceColumn(referenceColumn);
    setColumns(queryColumnsValue.filter(item => item.showType !== 'more'));
  };

  useEffect(() => {
    updateColummns(queryColumns);
    setDataSource(queryResults);
  }, [queryColumns, queryResults]);

  if (!queryColumns || !queryResults || !columns) {
    return null;
  }

  const singleData = dataSource.length === 1;
  const dateField = columns.find(item => item.showType === 'DATE' || item.type === 'DATE');
  const categoryField = columns.filter(item => item.showType === 'CATEGORY');
  const metricFields = columns.filter(item => item.showType === 'NUMBER');

  const isDslMetricCard =
    queryMode === 'DSL' && singleData && metricFields.length === 1 && columns.length === 1;

  const isMetricCard =
    (queryMode.includes('METRIC') || isDslMetricCard) &&
    (singleData || chatContext?.dateInfo?.startDate === chatContext?.dateInfo?.endDate);

  const isText =
    columns.length === 1 &&
    columns[0].showType === 'CATEGORY' &&
    ((!queryMode.includes('METRIC') && !queryMode.includes('ENTITY')) ||
      queryMode === 'METRIC_INTERPRET') &&
    singleData;

  const isTable =
    !isText &&
    !isMetricCard &&
    (categoryField.length > 1 ||
      queryMode === 'ENTITY_DETAIL' ||
      queryMode === 'ENTITY_DIMENSION' ||
      (categoryField.length === 1 && metricFields.length === 0));

  const onLoadData = async (value: any) => {
    setLoading(true);
    const { data } = await queryData({
      ...chatContext,
      ...value,
    });
    setLoading(false);
    if (data.code === 200) {
      updateColummns(data.data?.queryColumns || []);
      setDataSource(data.data?.queryResults || []);
    }
  };

  const onSelectDimension = (dimension?: DrillDownDimensionType) => {
    setDrillDownDimension(dimension);
    onLoadData({
      dimensions:
        dimension === undefined ? undefined : [...(chatContext.dimensions || []), dimension],
    });
  };

  const getMsgContent = () => {
    if (isText) {
      return <Text columns={columns} referenceColumn={referenceColumn} dataSource={dataSource} />;
    }
    if (isMetricCard) {
      return (
        <MetricCard
          data={{ ...data, queryColumns: columns, queryResults: dataSource }}
          loading={loading}
          drillDownDimension={drillDownDimension}
          onSelectDimension={onSelectDimension}
        />
      );
    }
    if (isTable) {
      return <Table data={{ ...data, queryColumns: columns, queryResults: dataSource }} />;
    }
    if (dateField && metricFields.length > 0) {
      if (!dataSource.every(item => item[dateField.nameEn] === dataSource[0][dateField.nameEn])) {
        return (
          <MetricTrend
            data={{ ...data, queryColumns: columns, queryResults: dataSource }}
            chartIndex={chartIndex}
            triggerResize={triggerResize}
          />
        );
      }
    }
    if (categoryField?.length > 0 && metricFields?.length > 0) {
      return (
        <Bar
          data={{ ...data, queryColumns: columns, queryResults: dataSource }}
          triggerResize={triggerResize}
          loading={loading}
          drillDownDimension={drillDownDimension}
          onSelectDimension={onSelectDimension}
        />
      );
    }
    return <Table data={{ ...data, queryColumns: columns, queryResults: dataSource }} />;
  };

  const chartMsgClass = classNames({ [prefixCls]: !isTable });

  return (
    <div className={chartMsgClass}>
      {dataSource?.length === 0 ? <div>暂无数据，如有疑问请联系管理员</div> : getMsgContent()}
    </div>
  );
};

export default ChatMsg;

import { isMobile } from '../../utils/utils';
import Bar from './Bar';
import Message from './Message';
import MetricCard from './MetricCard';
import MetricTrend from './MetricTrend';
import Table from './Table';
import { ColumnType, DrillDownDimensionType, MsgDataType } from '../../common/type';
import { useState } from 'react';
import { queryData } from '../../service';

type Props = {
  question: string;
  data: MsgDataType;
  chartIndex: number;
  isMobileMode?: boolean;
  triggerResize?: boolean;
};

const ChatMsg: React.FC<Props> = ({ question, data, chartIndex, isMobileMode, triggerResize }) => {
  const { queryColumns, queryResults, chatContext, entityInfo, queryMode } = data;

  const [columns, setColumns] = useState<ColumnType[]>(queryColumns);
  const [dataSource, setDataSource] = useState<any[]>(queryResults);

  const [drillDownDimension, setDrillDownDimension] = useState<DrillDownDimensionType>();
  const [loading, setLoading] = useState(false);

  if (!queryColumns || !queryResults) {
    return null;
  }

  const singleData = dataSource.length === 1;
  const dateField = columns.find(item => item.showType === 'DATE' || item.type === 'DATE');
  const categoryField = columns.filter(item => item.showType === 'CATEGORY');
  const metricFields = columns.filter(item => item.showType === 'NUMBER');

  const isMetricCard =
    (queryMode === 'METRIC_DOMAIN' || queryMode === 'METRIC_FILTER') &&
    (singleData || chatContext?.dateInfo?.startDate === chatContext?.dateInfo?.endDate);

  const isText = columns.length === 1 && columns[0].showType === 'CATEGORY' && singleData;

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

  const onSelectDimension = (dimension?: DrillDownDimensionType) => {
    setDrillDownDimension(dimension);
    onLoadData({
      dimensions:
        dimension === undefined ? undefined : [...(chatContext.dimensions || []), dimension],
    });
  };

  const getMsgContent = () => {
    if (isText) {
      return (
        <div style={{ lineHeight: '24px', width: 'fit-content' }}>
          {dataSource[0][columns[0].nameEn]}
        </div>
      );
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
    if (
      categoryField.length > 1 ||
      queryMode === 'ENTITY_DETAIL' ||
      queryMode === 'ENTITY_DIMENSION' ||
      (categoryField.length === 1 && metricFields.length === 0)
    ) {
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
    return (
      <Bar
        data={{ ...data, queryColumns: columns, queryResults: dataSource }}
        triggerResize={triggerResize}
        loading={loading}
        drillDownDimension={drillDownDimension}
        onSelectDimension={onSelectDimension}
      />
    );
  };

  let width = '100%';
  if (isText) {
    width = 'fit-content';
  } else if (isMetricCard) {
    width = '370px';
  } else if (categoryField.length > 1 && !isMobile && !isMobileMode) {
    if (columns.length === 1) {
      width = '600px';
    } else if (columns.length === 2) {
      width = '1000px';
    }
  }

  return (
    <Message
      position="left"
      chatContext={chatContext}
      entityInfo={entityInfo}
      title={question}
      isMobileMode={isMobileMode}
      width={width}
      queryMode={queryMode}
    >
      {getMsgContent()}
    </Message>
  );
};

export default ChatMsg;

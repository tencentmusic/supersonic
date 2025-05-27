import Bar from './Bar';
import MetricCard from './MetricCard';
import MetricTrend from './MetricTrend';
import MarkDown from './MarkDown';
import Table from './Table';
import { ColumnType, DrillDownDimensionType, FieldType, MsgDataType } from '../../common/type';
import { useEffect, useState } from 'react';
import { queryData } from '../../service';
import classNames from 'classnames';
import { PREFIX_CLS, MsgContentTypeEnum } from '../../common/constants';
import Text from './Text';
import DrillDownDimensions from '../DrillDownDimensions';
import MetricOptions from '../MetricOptions';
import { isMobile } from '../../utils/utils';
import Pie from './Pie';

type Props = {
  queryId?: number;
  question: string;
  data: MsgDataType;
  chartIndex: number;
  triggerResize?: boolean;
  forceShowTable?: boolean;
  isSimpleMode?: boolean;
  onMsgContentTypeChange: (msgContentType: MsgContentTypeEnum) => void;
};

const ChatMsg: React.FC<Props> = ({
  queryId,
  question,
  data,
  chartIndex,
  triggerResize,
  forceShowTable = false,
  isSimpleMode,
  onMsgContentTypeChange,
}) => {
  const { queryColumns, queryResults, chatContext, queryMode } = data || {};
  const { dimensionFilters, elementMatches } = chatContext || {};

  const [columns, setColumns] = useState<ColumnType[]>([]);
  const [referenceColumn, setReferenceColumn] = useState<ColumnType>();
  const [dataSource, setDataSource] = useState<any[]>(queryResults);
  const [drillDownDimension, setDrillDownDimension] = useState<DrillDownDimensionType>();
  const [secondDrillDownDimension, setSecondDrillDownDimension] =
    useState<DrillDownDimensionType>();
  const [loading, setLoading] = useState(false);
  const [defaultMetricField, setDefaultMetricField] = useState<FieldType>();
  const [activeMetricField, setActiveMetricField] = useState<FieldType>();
  const [dateModeValue, setDateModeValue] = useState<any>();
  const [currentDateOption, setCurrentDateOption] = useState<number>();

  const prefixCls = `${PREFIX_CLS}-chat-msg`;

  const updateColummns = (queryColumnsValue: ColumnType[]) => {
    const referenceColumn = queryColumnsValue.find(item => item.showType === 'more');
    setReferenceColumn(referenceColumn);
    setColumns(queryColumnsValue.filter(item => item.showType !== 'more'));
  };

  useEffect(() => {
    updateColummns(queryColumns);
    setDataSource(queryResults);
    setDefaultMetricField(chatContext?.metrics?.[0]);
    setActiveMetricField(chatContext?.metrics?.[0]);
    setDateModeValue(chatContext?.dateInfo?.dateMode);
    setCurrentDateOption(chatContext?.dateInfo?.unit);
    setDrillDownDimension(undefined);
    setSecondDrillDownDimension(undefined);
  }, [data]);

  const metricFields = columns.filter(item => item.showType === 'NUMBER');

  const getMsgContentType = () => {
    const singleData = dataSource.length === 1;
    const dateField = columns.find(item => item.showType === 'DATE' || item.type === 'DATE');
    const categoryField = columns.filter(item => item.showType === 'CATEGORY');
    const metricFields = columns.filter(item => item.showType === 'NUMBER');
    if (!columns) {
      return;
    }
    if (isSimpleMode) {
      return MsgContentTypeEnum.MARKDOWN;
    }
    if (forceShowTable) {
      return MsgContentTypeEnum.TABLE;
    }
    const isDslMetricCard =
      queryMode === 'LLM_S2SQL' && singleData && metricFields.length === 1 && columns.length === 1;
    const isMetricCard = (queryMode.includes('METRIC') || isDslMetricCard) && singleData;
    const isText = !queryColumns?.length;

    if (isText) {
      return MsgContentTypeEnum.TEXT;
    }

    if (isMetricCard) {
      return MsgContentTypeEnum.METRIC_CARD;
    }

    const isTable =
      !isText &&
      !isMetricCard &&
      (categoryField.length > 1 ||
        queryMode === 'TAG_DETAIL' ||
        queryMode === 'ENTITY_DIMENSION' ||
        dataSource?.length === 1 ||
        (categoryField.length === 1 && metricFields.length === 0));

    if (isTable) {
      return MsgContentTypeEnum.TABLE;
    }
    const isMetricTrend =
      dateField &&
      metricFields.length > 0 &&
      categoryField.length <= 1 &&
      !(metricFields.length > 1 && categoryField.length > 0) &&
      !dataSource.every(item => item[dateField.bizName] === dataSource[0][dateField.bizName]);

    if (isMetricTrend) {
      return MsgContentTypeEnum.METRIC_TREND;
    }

    const isMetricPie =
      categoryField.length > 0 &&
      metricFields?.length === 1 &&
      (isMobile ? dataSource?.length <= 5 : dataSource?.length <= 10) &&
      dataSource.every(item => item[metricFields[0].bizName] > 0);

    if (isMetricPie) {
      return MsgContentTypeEnum.METRIC_PIE;
    }

    const isMetricBar =
      categoryField?.length > 0 &&
      metricFields?.length === 1 &&
      (isMobile ? dataSource?.length <= 5 : dataSource?.length <= 50);

    if (isMetricBar) {
      return MsgContentTypeEnum.METRIC_BAR;
    }
    return MsgContentTypeEnum.TABLE;
  };

  const getMsgStyle = (type: MsgContentTypeEnum) => {
    if (isMobile) {
      return { maxWidth: 'calc(100vw - 20px)' };
    }
    if (!queryResults?.length || !queryColumns.length) {
      return;
    }
    if (type === MsgContentTypeEnum.METRIC_BAR) {
      return {
        [queryResults.length > 5 ? 'width' : 'minWidth']: queryResults.length * 150,
      };
    }
    if (type === MsgContentTypeEnum.TABLE) {
      return {
        [queryColumns.length > 5 ? 'width' : 'minWidth']: queryColumns.length * 150,
      };
    }
    if (type === MsgContentTypeEnum.METRIC_TREND || type === MsgContentTypeEnum.METRIC_PIE) {
      return { width: 'calc(100vw - 410px)' };
    }
  };

  useEffect(() => {
    const type = getMsgContentType();
    if (type) {
      onMsgContentTypeChange?.(type);
    }
  }, [data, columns, isSimpleMode]);

  if (!queryColumns || !queryResults || !columns) {
    return null;
  }

  const getMsgContent = () => {
    const contentType = getMsgContentType();
    switch (contentType) {
      case MsgContentTypeEnum.TEXT:
        return <Text columns={columns} referenceColumn={referenceColumn} dataSource={dataSource} />;
      case MsgContentTypeEnum.METRIC_CARD:
        return (
          <MetricCard
            data={{ ...data, queryColumns: columns, queryResults: dataSource }}
            question={question}
            loading={loading}
          />
        );
      case MsgContentTypeEnum.TABLE:
        return (
          <Table
            question={question}
            data={{ ...data, queryColumns: columns, queryResults: dataSource }}
            loading={loading}
          />
        );
      case MsgContentTypeEnum.METRIC_TREND:
        return (
          <MetricTrend
            data={{
              ...data,
              queryColumns: columns,
              queryResults: dataSource,
            }}
            question={question}
            loading={loading}
            chartIndex={chartIndex}
            triggerResize={triggerResize}
            activeMetricField={activeMetricField}
            drillDownDimension={drillDownDimension}
            currentDateOption={currentDateOption}
            onSelectDateOption={selectDateOption}
          />
        );
      case MsgContentTypeEnum.METRIC_BAR:
        return (
          <Bar
            data={{ ...data, queryColumns: columns, queryResults: dataSource }}
            question={question}
            triggerResize={triggerResize}
            loading={loading}
            metricField={metricFields[0]}
          />
        );
      case MsgContentTypeEnum.METRIC_PIE:
        const categoryField = columns.find(item => item.showType === 'CATEGORY');
        if (!categoryField) {
          return null;
        }
        return (
          <Pie
            data={{ ...data, queryColumns: columns, queryResults: dataSource }}
            question={question}
            triggerResize={triggerResize}
            loading={loading}
            metricField={metricFields[0]}
            categoryField={categoryField}
          />
        );
      case MsgContentTypeEnum.MARKDOWN:
        return (
          <div style={{ maxHeight: 800 }}>
            <MarkDown markdown={data.textResult} loading={loading} />
          </div>
        );
      default:
        return (
          <Table
            question={question}
            data={{ ...data, queryColumns: columns, queryResults: dataSource }}
            loading={loading}
          />
        );
    }
  };

  const onLoadData = async (value: any) => {
    setLoading(true);
    const res: any = await queryData({
      ...chatContext,
      ...value,
      queryId,
      parseId: chatContext.id,
    });
    setLoading(false);
    if (res.code === 200) {
      updateColummns(res.data?.queryColumns || []);
      setDataSource(res.data?.queryResults || []);
    }
  };

  const onSelectDimension = async (dimension?: DrillDownDimensionType) => {
    setLoading(true);
    setDrillDownDimension(dimension);
    onLoadData({
      dateInfo: {
        ...chatContext.dateInfo,
        dateMode: dateModeValue,
        unit: currentDateOption || chatContext.dateInfo?.unit,
      },
      dimensions: dimension
        ? [...(chatContext.dimensions || []), dimension]
        : chatContext.dimensions,
      metrics: [activeMetricField || defaultMetricField],
    });
  };

  const onSelectSecondDimension = (dimension?: DrillDownDimensionType) => {
    setSecondDrillDownDimension(dimension);
    onLoadData({
      dateInfo: {
        ...chatContext.dateInfo,
        dateMode: dateModeValue,
        unit: currentDateOption || chatContext.dateInfo?.unit,
      },
      dimensions: [
        ...(chatContext.dimensions || []),
        ...(drillDownDimension ? [drillDownDimension] : []),
        ...(dimension ? [dimension] : []),
      ],
      metrics: [activeMetricField || defaultMetricField],
    });
  };

  const onSwitchMetric = (metricField?: FieldType) => {
    setActiveMetricField(metricField);
    onLoadData({
      dateInfo: {
        ...chatContext.dateInfo,
        dateMode: dateModeValue,
        unit: currentDateOption || chatContext.dateInfo?.unit,
      },
      dimensions: drillDownDimension
        ? [...(chatContext.dimensions || []), drillDownDimension]
        : chatContext.dimensions,
      metrics: [metricField || defaultMetricField],
    });
  };

  const selectDateOption = (dateOption: number) => {
    setCurrentDateOption(dateOption);
    setDateModeValue('RECENT');
    onLoadData({
      metrics: [activeMetricField || defaultMetricField],
      dimensions: drillDownDimension
        ? [...(chatContext.dimensions || []), drillDownDimension]
        : chatContext.dimensions,
      dateInfo: {
        ...chatContext?.dateInfo,
        dateMode: 'RECENT',
        unit: dateOption,
      },
    });
  };

  const chartMsgClass = classNames({
    [prefixCls]: ![MsgContentTypeEnum.TABLE, MsgContentTypeEnum.MARKDOWN].includes(
      getMsgContentType() as MsgContentTypeEnum
    ),
  });

  const entityId = dimensionFilters?.length > 0 ? dimensionFilters[0].value : undefined;
  const entityName = elementMatches?.find((item: any) => item.element?.type === 'ID')?.element
    ?.name;

  const isEntityMode =
    (queryMode === 'TAG_LIST_FILTER' || queryMode === 'METRIC_TAG') &&
    typeof entityId === 'string' &&
    entityName !== undefined;

  const existDrillDownDimension =
    queryMode.includes('METRIC') &&
    getMsgContentType() !== MsgContentTypeEnum.TEXT &&
    !isEntityMode;

  const recommendMetrics = chatContext?.metrics?.filter(metric =>
    queryColumns.every(queryColumn => queryColumn.bizName !== metric.bizName)
  );

  const isMultipleMetric =
    (queryMode.includes('METRIC') || queryMode === 'LLM_S2SQL') &&
    recommendMetrics?.length > 0 &&
    queryColumns?.filter(column => column.showType === 'NUMBER').length === 1;

  const type = getMsgContentType();
  const style = type ? getMsgStyle(type) : undefined;

  return (
    <div className={chartMsgClass} style={style}>
      {dataSource?.length === 0 ? (
        <div>暂无数据</div>
      ) : (
        <div>
          {getMsgContent()}
          {(isMultipleMetric || existDrillDownDimension) && !isSimpleMode && (
            <div
              className={`${prefixCls}-bottom-tools ${
                getMsgContentType() === MsgContentTypeEnum.METRIC_CARD
                  ? `${prefixCls}-metric-card-tools`
                  : ''
              } ${isMobile ? 'mobile' : ''}`}
            >
              {isMultipleMetric && (
                <MetricOptions
                  metrics={chatContext.metrics}
                  defaultMetric={defaultMetricField}
                  currentMetric={activeMetricField}
                  onSelectMetric={onSwitchMetric}
                />
              )}
              {existDrillDownDimension && (
                <DrillDownDimensions
                  drillDownDimensions={data?.recommendedDimensions || []}
                  drillDownDimension={drillDownDimension}
                  secondDrillDownDimension={secondDrillDownDimension}
                  originDimensions={chatContext.dimensions}
                  dimensionFilters={chatContext.dimensionFilters}
                  onSelectDimension={onSelectDimension}
                  onSelectSecondDimension={onSelectSecondDimension}
                />
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ChatMsg;

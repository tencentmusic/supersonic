import {
  ChatContextType,
  DateInfoType,
  DimensionType,
  EntityInfoType,
  FilterItemType,
  MetricType,
  MsgDataType,
  ParseStateEnum,
  ParseTimeCostType,
  SimilarQuestionType,
} from '../../common/type';
import { useEffect, useRef, useState } from 'react';
import { chatExecute, chatParse, queryData, queryEntityInfo, switchEntity } from '../../service';
import { PARSE_ERROR_TIP, PREFIX_CLS, SEARCH_EXCEPTION_TIP } from '../../common/constants';
import IconFont from '../IconFont';
import ParseTip from './ParseTip';
import ExecuteItem from './ExecuteItem';
import { exportData, isMobile } from '../../utils/utils';
import classNames from 'classnames';
import Tools from '../Tools';
import dayjs from 'dayjs';
import {
  IAggregationPill,
  IDateFilterPill,
  IGroupPill,
  INumberFilterPill,
  IPill,
  ITextFilterPill,
  ITopNPill,
} from '../FiltersInfo/types';
import SqlItemModal, { SqlItemModalHandle } from './SqlItemModal';
import SimilarQuestionItem from './SimilarQuestionItem';

type Props = {
  msg: string;
  conversationId?: number;
  modelId?: number;
  agentId?: number;
  score?: number;
  filter?: any[];
  parseInfos?: ChatContextType[];
  parseTimeCostValue?: ParseTimeCostType;
  msgData?: MsgDataType;
  triggerResize?: boolean;
  isDeveloper?: boolean;
  integrateSystem?: string;
  executeItemNode?: React.ReactNode;
  renderCustomExecuteNode?: boolean;
  isSimpleMode?: boolean;
  onMsgDataLoaded?: (data: MsgDataType, valid: boolean, isRefresh?: boolean) => void;
  onQuestionAsked?: () => void;
  onUpdateMessageScroll?: () => void;
  onSendMsg?: (msg: string) => void;
};

const ChatItem: React.FC<Props> = ({
  msg,
  conversationId,
  modelId,
  agentId,
  score,
  filter,
  triggerResize,
  parseInfos,
  parseTimeCostValue,
  msgData,
  isDeveloper,
  integrateSystem,
  executeItemNode,
  renderCustomExecuteNode,
  isSimpleMode,
  onMsgDataLoaded,
  onQuestionAsked,
  onUpdateMessageScroll,
  onSendMsg,
}) => {
  const [parseLoading, setParseLoading] = useState(false);
  const [parseTimeCost, setParseTimeCost] = useState<ParseTimeCostType>();
  const [parseInfo, setParseInfo] = useState<ChatContextType>();
  const [parseInfoOptions, setParseInfoOptions] = useState<ChatContextType[]>([]);
  const [parseTip, setParseTip] = useState('');
  const [executeMode, setExecuteMode] = useState(false);
  const [executeLoading, setExecuteLoading] = useState(false);
  const [executeTip, setExecuteTip] = useState('');
  const [data, setData] = useState<MsgDataType>();
  const [entitySwitchLoading, setEntitySwitchLoading] = useState(false);
  const [dimensionFilters, setDimensionFilters] = useState<FilterItemType[]>([]);
  const [dateInfo, setDateInfo] = useState<DateInfoType>({} as DateInfoType);
  const [entityInfo, setEntityInfo] = useState<EntityInfoType>({} as EntityInfoType);
  const [dataCache, setDataCache] = useState<Record<number, { tip: string; data?: MsgDataType }>>(
    {}
  );
  const sqlItemModalRef = useRef<SqlItemModalHandle>(null);

  const prefixCls = `${PREFIX_CLS}-item`;

  const updateData = (res: Result<MsgDataType>) => {
    let tip: string = '';
    let data: MsgDataType | undefined = undefined;
    const { queryColumns, queryResults, queryState, queryMode, response, chatContext } =
      res.data || {};
    if (res.code === 400 || res.code === 401 || res.code === 412) {
      tip = res.msg;
    } else if (res.code !== 200) {
      tip = SEARCH_EXCEPTION_TIP;
    } else if (queryState !== 'SUCCESS') {
      tip = response && typeof response === 'string' ? response : SEARCH_EXCEPTION_TIP;
    } else if (
      (queryColumns && queryColumns.length > 0 && queryResults) ||
      queryMode === 'WEB_PAGE'
    ) {
      data = res.data;
      tip = '';
    }
    if (chatContext) {
      setDataCache({ ...dataCache, [chatContext!.id!]: { tip, data } });
    }
    if (data) {
      setData(data);
      setExecuteTip('');
      return true;
    }
    setExecuteTip(tip || SEARCH_EXCEPTION_TIP);
    return false;
  };

  const onExecute = async (
    parseInfoValue: ChatContextType,
    parseInfos?: ChatContextType[],
    isSwitchParseInfo?: boolean
  ) => {
    setExecuteMode(true);
    if (isSwitchParseInfo) {
      setEntitySwitchLoading(true);
    } else {
      setExecuteLoading(true);
    }
    try {
      const res: any = await chatExecute(msg, conversationId!, parseInfoValue);
      const valid = updateData(res);
      onMsgDataLoaded?.(
        {
          ...res.data,
          parseInfos,
          queryId: parseInfoValue.queryId,
        },
        valid
      );
    } catch (e) {
      const tip = SEARCH_EXCEPTION_TIP;
      setExecuteTip(SEARCH_EXCEPTION_TIP);
      setDataCache({ ...dataCache, [parseInfoValue!.id!]: { tip } });
    }
    onQuestionAsked?.();
    if (isSwitchParseInfo) {
      setEntitySwitchLoading(false);
    } else {
      setExecuteLoading(false);
    }
  };

  const updateDimensionFitlers = (filters: FilterItemType[]) => {
    setDimensionFilters(
      filters.sort((a, b) => {
        if (a.name < b.name) {
          return -1;
        }
        if (a.name > b.name) {
          return 1;
        }
        return 0;
      })
    );
  };

  const sendMsg = async () => {
    setParseLoading(true);
    const parseData: any = await chatParse(msg, conversationId, modelId, agentId, filter);
    setParseLoading(false);
    const { code, data } = parseData || {};
    const { state, selectedParses, candidateParses, queryId, parseTimeCost } = data || {};
    const parses = selectedParses?.concat(candidateParses || []) || [];
    if (
      code !== 200 ||
      state === ParseStateEnum.FAILED ||
      !parses.length ||
      (!parses[0]?.properties?.type && !parses[0]?.queryMode)
    ) {
      setParseTip(PARSE_ERROR_TIP);
      setParseInfo({ queryId } as any);
      onQuestionAsked?.();
      return;
    }
    onUpdateMessageScroll?.();
    const parseInfos = parses.slice(0, 5).map((item: any) => ({
      ...item,
      queryId,
    }));
    setParseInfoOptions(parseInfos || []);
    const parseInfoValue = parseInfos[0];
    setParseInfo(parseInfoValue);
    setParseTimeCost(parseTimeCost);
    setEntityInfo(parseInfoValue.entityInfo || {});
    updateDimensionFitlers(parseInfoValue?.dimensionFilters || []);
    setDateInfo(parseInfoValue?.dateInfo);
    onExecute(parseInfoValue, parseInfos);
  };

  useEffect(() => {
    if (data !== undefined || executeTip !== '' || parseLoading) {
      return;
    }
    if (msgData) {
      const parseInfoOptionsValue =
        parseInfos && parseInfos.length > 0
          ? parseInfos.map(item => ({ ...item, queryId: msgData.queryId }))
          : [{ ...msgData.chatContext, queryId: msgData.queryId }];
      const parseInfoValue = parseInfoOptionsValue[0];
      setParseInfoOptions(parseInfoOptionsValue);
      setParseInfo(parseInfoValue);
      setParseTimeCost(parseTimeCostValue);
      updateDimensionFitlers(parseInfoValue.dimensionFilters || []);
      setDateInfo(parseInfoValue.dateInfo);
      setExecuteMode(true);
      updateData({ code: 200, data: msgData, msg: 'success' });
    } else if (msg) {
      sendMsg();
    }
  }, [msg, msgData]);

  const onSwitchEntity = async (entityId: string) => {
    setEntitySwitchLoading(true);
    const res = await switchEntity(entityId, data?.chatContext?.modelId, conversationId || 0);
    setEntitySwitchLoading(false);
    setData(res.data);
    const { chatContext, entityInfo } = res.data || {};
    const chatContextValue = { ...(chatContext || {}), queryId: parseInfo?.queryId };
    setParseInfo(chatContextValue);
    setEntityInfo(entityInfo);
    updateDimensionFitlers(chatContextValue?.dimensionFilters || []);
    setDateInfo(chatContextValue?.dateInfo);
    setDataCache({ ...dataCache, [chatContextValue.id!]: { tip: '', data: res.data } });
  };

  const onFiltersChange = (dimensionFilters: FilterItemType[]) => {
    setDimensionFilters(dimensionFilters);
  };

  const onDateInfoChange = (dateRange: any) => {
    setDateInfo({
      ...(dateInfo || {}),
      startDate: dayjs(dateRange[0]).format('YYYY-MM-DD'),
      endDate: dayjs(dateRange[1]).format('YYYY-MM-DD'),
      dateMode: 'BETWEEN',
      unit: 0,
    });
  };

  const onRefresh = async () => {
    setEntitySwitchLoading(true);
    const { dimensions, metrics, id, queryId } = parseInfo || {};
    const chatContextValue = {
      dimensions,
      metrics,
      dateInfo,
      dimensionFilters,
      parseId: id,
      queryId,
    };
    const res: any = await queryData(chatContextValue);
    setEntitySwitchLoading(false);
    if (res.code === 200) {
      const resChatContext = res.data?.chatContext;
      const contextValue = { ...(resChatContext || chatContextValue), queryId };
      const dataValue = {
        ...res.data,
        chatContext: contextValue,
        parseInfos: parseInfoOptions,
        queryId,
      };
      onMsgDataLoaded?.(dataValue, true, true);
      setData(dataValue);
      setParseInfo(contextValue);
      setDataCache({ ...dataCache, [id!]: { tip: '', data: dataValue } });
    }
  };

  const handleFilterEditConfirm = async ({
    pillData,
    datasetId,
    dimensions: dataSetDimensions,
    metrics: dataSetMetrics,
  }: {
    pillData: IPill[];
    datasetId: number;
    dimensions: any[];
    metrics: any[];
  }) => {
    const getFieldInfo = (field: string) => {
      const fieldInfo =
        [...dataSetDimensions, ...dataSetMetrics].find(item => item.bizName === field) || {};
      return fieldInfo;
    };

    setEntitySwitchLoading(true);
    const { dimensions: oldDimensions, metrics: oldMetrics, id, queryId } = parseInfo || {};
    const dateFilterPill = pillData.find(item => item.type === 'date-filter') as IDateFilterPill;
    let newDateInfo = dateInfo ? { ...dateInfo } : undefined;
    if (dateFilterPill) {
      newDateInfo = {
        ...dateInfo,
        startDate: dateFilterPill.value?.[0]!,
        endDate: dateFilterPill.value?.[1]!,
      };
    } else {
      newDateInfo = undefined;
    }

    let newDimensionFilters: FilterItemType[] = [];
    const textFilterPills = pillData.filter(
      item => item.type === 'text-filter'
    ) as ITextFilterPill[];

    textFilterPills.forEach(item => {
      // 判断原dimensionFilters中是否存在，存在则替换部分值，不存在则插入
      const oldFilter = dimensionFilters.find(filter => filter.bizName === item.field);
      if (oldFilter) {
        oldFilter.name = item.fieldName;
        oldFilter.value = item.value;
        oldFilter.operator = item.operator;
        newDimensionFilters.push(oldFilter);
      } else {
        newDimensionFilters.push({
          elementID: getFieldInfo(item.field).id,
          name: item.fieldName,
          value: item.value,
          operator: item.operator,
          bizName: item.field,
        });
      }
    });

    const numberFilterPills = pillData.filter(
      item => item.type === 'number-filter'
    ) as INumberFilterPill[];

    numberFilterPills.forEach(item => {
      // 判断原dimensionFilters中是否存在，存在则替换部分值，不存在则插入
      const oldFilter = dimensionFilters.find(filter => filter.bizName === item.field);
      if (oldFilter) {
        oldFilter.name = item.fieldName;
        oldFilter.value = item.value;
        oldFilter.operator = item.operator;
        newDimensionFilters.push(oldFilter);
      } else {
        newDimensionFilters.push({
          elementID: getFieldInfo(item.field).id,
          name: item.fieldName,
          value: item.value,
          operator: item.operator,
          bizName: item.field,
        });
      }
    });

    const newDimensions: DimensionType[] = [];
    const groupPill = pillData.find(item => item.type === 'group') as IGroupPill;
    groupPill?.fields.forEach(field => {
      const oldDim = oldDimensions?.find(dim => dim.bizName === field.field);
      if (!oldDim) {
        const dim = getFieldInfo(field.field);
        newDimensions?.push({
          ...dim,
          type: 'DIMENSION',
          dataFormatType: null,
          dataSet: datasetId,
          dataSetName: '',
          model: modelId!,
          order: null,
          relatedSchemaElements: [],
          schemaValueMaps: null,
          useCnt: null,
        });
      } else {
        newDimensions.push(oldDim);
      }
    });

    const newMetrics: MetricType[] = [];
    const aggPill = pillData.find(item => item.type === 'aggregation') as IAggregationPill;
    aggPill?.fields.forEach(field => {
      const oldMetric = oldMetrics?.find(metric => metric.bizName === field.field);
      if (!oldMetric) {
        const metric = getFieldInfo(field.field);
        newMetrics?.push({
          ...metric,
          type: 'METRIC',
          aggregator: field.operator,
          dataFormatType: null,
          dataSet: datasetId,
          dataSetName: '',
          model: modelId!,
          order: null,
          relatedSchemaElements: [],
          schemaValueMaps: null,
          useCnt: null,
        });
      } else {
        newMetrics.push(oldMetric);
      }
    });

    const topNPill = pillData.find(item => item.type === 'top-n') as ITopNPill;

    const chatContextValue = {
      dimensions: newDimensions,
      metrics: newMetrics,
      dateInfo: newDateInfo,
      dimensionFilters: newDimensionFilters,
      parseId: id,
      limit: topNPill?.value,
      queryId,
    };
    const res: any = await queryData(chatContextValue);
    setEntitySwitchLoading(false);
    if (res.code === 200) {
      const resChatContext = res.data?.chatContext;
      const contextValue = { ...(resChatContext || chatContextValue), queryId };
      const dataValue = {
        ...res.data,
        chatContext: contextValue,
        parseInfos: parseInfoOptions,
        queryId,
      };
      onMsgDataLoaded?.(dataValue, true, true);
      setData(dataValue);
      setParseInfo(contextValue);
      setDataCache({ ...dataCache, [id!]: { tip: '', data: dataValue } });
    }
  };

  const getEntityInfo = async (parseInfoValue: ChatContextType) => {
    const res = await queryEntityInfo(parseInfoValue.queryId, parseInfoValue.id);
    setEntityInfo(res.data);
  };

  const onSelectParseInfo = async (parseInfoValue: ChatContextType) => {
    setParseInfo(parseInfoValue);
    updateDimensionFitlers(parseInfoValue.dimensionFilters || []);
    setDateInfo(parseInfoValue.dateInfo);
    if (parseInfoValue.entityInfo) {
      setEntityInfo(parseInfoValue.entityInfo);
    } else {
      getEntityInfo(parseInfoValue);
    }
    if (dataCache[parseInfoValue.id!]) {
      const { tip, data } = dataCache[parseInfoValue.id!];
      setExecuteTip(tip);
      setData(data);
      onMsgDataLoaded?.(
        {
          ...(data as any),
          parseInfos,
          queryId: parseInfoValue.queryId,
        },
        true,
        true
      );
    } else {
      onExecute(parseInfoValue, parseInfoOptions, true);
    }
  };

  const onSelectQuestion = (question: SimilarQuestionType) => {
    onSendMsg?.(question.queryText);
  };

  const contentClass = classNames(`${prefixCls}-content`, {
    [`${prefixCls}-content-mobile`]: isMobile,
  });

  const { llmReq, llmResp } = parseInfo?.properties?.CONTEXT || {};

  return (
    <div className={prefixCls}>
      {!isMobile && integrateSystem !== 'wiki' && (
        <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} />
      )}
      <div className={isMobile ? `${prefixCls}-mobile-msg-card` : `${prefixCls}-msg-card`}>
        <div className={contentClass}>
          <ParseTip
            isSimpleMode={isSimpleMode}
            parseLoading={parseLoading}
            parseInfoOptions={parseInfoOptions}
            parseTip={parseTip}
            currentParseInfo={parseInfo}
            agentId={agentId}
            dimensionFilters={dimensionFilters}
            dateInfo={dateInfo}
            entityInfo={entityInfo}
            integrateSystem={integrateSystem}
            parseTimeCost={parseTimeCost?.parseTime}
            isDeveloper={isDeveloper}
            onSelectParseInfo={onSelectParseInfo}
            onSwitchEntity={onSwitchEntity}
            onFiltersChange={onFiltersChange}
            onDateInfoChange={onDateInfoChange}
            onRefresh={onRefresh}
            onQueryConditionChange={handleFilterEditConfirm}
          />
          {executeMode && (
            <>
              {!isMobile &&
                parseInfo?.sqlInfo &&
                isDeveloper &&
                integrateSystem !== 'c2' &&
                !isSimpleMode && (
                  <SqlItemModal
                    ref={sqlItemModalRef}
                    llmReq={llmReq}
                    llmResp={llmResp}
                    integrateSystem={integrateSystem}
                    queryMode={parseInfo.queryMode}
                    sqlInfo={parseInfo.sqlInfo}
                    sqlTimeCost={parseTimeCost?.sqlTime}
                  />
                )}
              <ExecuteItem
                isSimpleMode={isSimpleMode}
                queryId={parseInfo?.queryId}
                executeLoading={executeLoading}
                entitySwitchLoading={entitySwitchLoading}
                executeTip={executeTip}
                chartIndex={0}
                data={data}
                triggerResize={triggerResize}
                executeItemNode={executeItemNode}
                isDeveloper={isDeveloper}
                renderCustomExecuteNode={renderCustomExecuteNode}
                menu={{
                  onClick: ({ key }) => {
                    if (key === 'viewSQL') {
                      sqlItemModalRef.current?.show();
                    }

                    if (key === 'exportData' && !!data?.queryResults.length) {
                      exportData(data);
                    }
                  },
                  items: [
                    {
                      label: '导出查询结果',
                      disabled: !data?.queryResults.length,
                      key: 'exportData',
                    },
                    {
                      label: '查看SQL',
                      key: 'viewSQL',
                    },
                  ],
                }}
              />
            </>
          )}
          {/* {(parseTip !== '' || (executeMode && !executeLoading)) &&
            integrateSystem !== 'c2' &&
            !isSimpleMode && (
              <SimilarQuestionItem
                queryId={parseInfo?.queryId}
                defaultExpanded={parseTip !== '' || executeTip !== '' || integrateSystem === 'wiki'}
                similarQueries={data?.similarQueries}
                onSelectQuestion={onSelectQuestion}
              />
            )} */}
        </div>
        {(parseTip !== '' || (executeMode && !executeLoading)) && integrateSystem !== 'c2' && (
          <Tools queryId={parseInfo?.queryId || 0} scoreValue={score} />
        )}
      </div>
    </div>
  );
};

export default ChatItem;

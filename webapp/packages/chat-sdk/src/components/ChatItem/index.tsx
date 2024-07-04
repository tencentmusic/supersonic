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
  isDebugMode?: boolean;
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
  isDebugMode,
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
    const { queryColumns, queryResults, queryState, queryMode, response, chatContext, textResult } =
      res.data || {};
    if (res.code === 400 || res.code === 401 || res.code === 412) {
      tip = res.msg;
    } else if (res.code !== 200) {
      tip = SEARCH_EXCEPTION_TIP;
    } else if (queryState !== 'SUCCESS') {
      tip = response && typeof response === 'string' ? response : SEARCH_EXCEPTION_TIP;
    } else if (
      (queryColumns && queryColumns.length > 0 && queryResults) ||
      queryMode === 'WEB_PAGE' ||
      queryMode === 'WEB_SERVICE' ||
      queryMode === 'PLAIN_TEXT'
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
      const res: any = await chatExecute(msg, conversationId!, parseInfoValue, agentId);
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
      setExecuteTip('');
    } else {
      setExecuteTip(SEARCH_EXCEPTION_TIP);
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
          alias: dim.alias ? dim.alias.split(',') : [],
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
          alias: metric.alias ? metric.alias.split(',') : [],
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
      setExecuteTip('');
    } else {
      setExecuteTip(SEARCH_EXCEPTION_TIP);
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
        <>
          {/* <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} /> */}
          <img
            className={`${prefixCls}-avatar`}
            alt="avatar"
            src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAKoAAACqCAYAAAA9dtSCAAAACXBIWXMAADddAAA3XQEZgEZdAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAABaQSURBVHgB7Z1rbBzXdcf/Z2bffC3J5WpFkRYlURYdyREdyzYTC4mNJLCqBI2BPIsWiFE5aRIUaIF+MPpNRVG0BVo0/dA0TWzE+ZakSeGibpACRuwkTezWdiU/FEuyHpRISRQpkityudzX3NtzZylasihySc7MzuzeH0BzuTu0SO5/zj2vey6g0Wg0Go2miSBoAkX3V+WQFEgKicyN50igQiaOzzxN42hQtFB9TM83ZKtVxEGLcL8EUiTlEL9lGUgZ45djN11a4eezIJkjScfDAn879SxNoIHQQvUZnV+T+6wKWkniKRbkML9Drfx0TEqE6FZxrggLusDXZQnyX2Aaz2W/S8fRAGih1omBJ2RsOoIBUyJGFfQJE59ilY2w1Ab5cyucQQn2HyGMb2afZYsbYLRQPST5NTkgKsgQrD5D0KAg4zF+uo8t5yDcI0sk/z4cNZ6e+lZw3QEtVBdJPiGTvFgnUcYwIPZJGHuXRKk+kvCOrCHp79h9+KegWlYtVIdpPyIHDQOt7Cs+xkv4gPpggQywVVNLegj1Y1S5AUlhfHv0WSogYNTzD9cwJP+ULeccHhcG9is/U1rKWkqVPrKjc9saSNSbAQF6asbESX78MwQMbVFr5JGjMjTKN3Y2i5iR42BHYp8F8ThgPMzL+T4EBaLj9/XjgZeOcggXILRQV0FF5ln2Mq08MqawE+zDnGjfz/lMFqinPqajSKLfmXuGAmVVtVDfD1vOtssYJAsDhsSQJFug7G9KJcwBNAJEP+Df7+tBCqy0j4pqdG6EWJQcjYsxwdUfOzofZpGmHMxp+gcpR2DSI/zoOQSEphVq8ity2ODlnB21L7JAhywhU/w0f1ByOfKpfwDkDoQ+KcQhBEioTbP0t31VpkhgkMV5iH/rz3LKKGW/QDJV57RRXSDQ85xG+3pQGlka7g1S0fmJScTCFcTynM+EwDCni56CZQty0DaSN1vKRrWaayBJjliSlFV9GgGgISyq6jIqR/imW+DEOtfNYeJhUa2bH2hIH9Mx6G86Bf4iCAWAYFpUtprJCfSpurkhxEiphD1URExCfoIDoD62opoaIMIQFwD6+OEZ+JxACZUtZ6ZUxGfkODLsY45wSTDJf+19ELK1SVfwzSFVuZe0UJ2g64g8VCE8RhKZUkE1c8g+9ldabyzpWqAbR6Xj2H/vQwDwnVC7npR9FdjL9xfZYh6yJFeAVLKd7J+1+vNqdTpFjCP/PcrHn/oW5eBj6iZUFZ0fZz8TFpKsuxFlMaWUv8/5zMFbIzwJnzR1NCT8N/9cuUw/4Ye+3gngqVDtXKaFAwbfyccvYoADn4+xrzmkWuBkE+YyfUJSiPc2CvoVV8Whmjqum3bukj/EIC/nu/gWPiSr7W8ZZSW1taw7GdXTAJ+3/jkuVBWZc9ooaXEFKCtxPz/Vx8sLO+zUt7R7UuMz1MrGn74JH+OMUNnf7BrDJ1R0zukjdXeqLb0DCHArXJMxAJ+z7sqUHQRl0Wrk7Ba4AfZvvqyWc2gCjWnQh7nu/wp8yrosqtpycXyca+fAFzk6V83DvnfCNbWhjA5/CrZQ7d2UIQxjHo+zv/kZNEoDsWYZYeEIf/oBfMqaSz+LdACm+APOZn6Wo/NhaBqV8YigB/w6CmhVi6qai6XAX0tJj9QyTkYTaGIlwkH+/GP4EONOLyyJ9J/Ze/mEFmlToHonDsCnrChUFTRJIZ6y99boilGzEOMK4cMqqwMfcptQVcIe8+Ioh4Ffgqap4DgkxVmdg/Ahtwm1XMAh9km/DE0TIjPs7o3Ah9y+9EvxiNQ+aXNC9sysLfAhtwi1/Uk5Ioke0sFTk6KGBXPi33b/fMYtQiUhvix1Mr+5kXK4WMQj8BnLQlUtefxlUlvTJoeXf0OKDHwW/S//MKpvFEs9os2CyXdlyHzv60jI3pnZ7KSipnHgyQ+g//PvyLlav+nCBIo791T3/04uwDq8m4pwkGWhqjlLJGWykXUaNqvCbItVRZkIA9Hwe69H+TlDCxXtMTl0T5I+tljCdK3fk+lCfv4qLPWYKwflX70pCxUL02GJUlsvru/P0AI2QVMk87ta2Ey0VYWphBoLV0Ub0aWMFTEN7C2UsYckztf6PcrA3bjJ1WNOcym/MidMFGeuYuHnx+R8zMS0iODSwSGaxzpp6LdKibO/E2iJAvGItpa1YgnEpubRu1jGeDyMMjaKSndJ9nmBbjIgShUUDQtDL56Qo4sX8M7hw7W7B8tCVae/CUKgphCvhBLk9m6gt4OtpraYG2ZyDveUyji2KaHeBAvWYLHG2drGjTLSLb148MVj8n/bLJx+fh6lo4+uPgF7OeqXEYyzXK8hwCjLOZCqClWLdHPMF7B9oYwoXIRXuAcLIXz64xns/Om7ctV/a1moCYEc+ySBOy3jBp3sh96zFbirCxoHKFZkOruAdrgMR19JtqXDbRVs/5GU5p2uWxbq5e8oayrO8sPAiXUb+6EfYJF267l9jnL6Kh6CB1gWi7WI4fRbePBO19xSmWI3Qi39gRKqCph2pzklossUjsMWdQc8wrasAve+dEw+sNLrt5ZQTXsIQWCOIVQ+qVrqdZrJHRbLsh9eQ5wVOGZvtb+FW4QaZj+VSzOBsKhKpPdkgJ42XU1yCyERfm0Ue+Eh/FbGWZUjL7wit0gpl/V5i1CnnqEzRHIcAUhTtfNS390Gjcu8e1V+BB5jSLSH4njwP17Hcmh8ez+qoJ+ws+rrEYSq1LkjBY0HcIWqezLnfvT/fkyJFKfCt974+nahmvb4QV8v/2n+s7XFofEAriZ1Z/PeC5UrsGErjPSJE5zhxwpCDYdxRp2XCR+zKw2NRwhQ6MxV1OWsV84C9F8R6FSPbxPq0uRh3x49uLWjuvRrvEEImZhZwN2oA1y5isQspFQhYMXt0kYYfwWfojqhNN5Sqsj2sSy6UQfKhHs73kRsRaHOfpvehg+tqmrNa3W1+qxZCUsgcX3Bez9VoTIA8QQSd5yUwn6q7ya7KZHqZhPvqVhovzQrdqJeFLD1zkKFeB0+Q5VJQyY0HqMS/wtF6rmeRwJ1gC167x2FKgzDdxa1jS1qRAu1LmQXMXhuStTnTCqBzCpLv7/SVKohOhHV5dJ6YVloLVeMukQIKvq/o1DNBCZYrL4+KUPjHSULnfNF0VkRMFAH7jx2MssurPRP5B8iveep3kzljLtn8vU5rfuOQrWPxiZLNVL7Qqwq2teBVH1ZKMj+bK5OaarVXhSG+TYRvQSNBtXl/1IW/YtlhOExqws1ZFtT35ZTNd7CaapoqVKfFNWqQl34Fk2wn/oL+KDrX0X9OjVVf8Zn5Efn8/C8d23tCI6URSVtVTU2hQoycyXvA6q1hWrhJQ6234ZGs8SVLHrhMWsKNfssZaUM5jZqjTtcyUrP+1Nra/Egg5d+qYSqNyV7TLIF2JkG2hPVJPLsgsQbo6grswu0Hx5Tk1DVNmppQR1AoU+L9gi1y/ZzHyY8NFjdDv5e5zDh3SvAj1+WOF2nELciZOLsJGV2pb0Lsmsqh2W/S8fJ4Nq/xhOUFf3GY4TD91V32qr9YarP4cbH/gHgL3+P8JWPk31tPXhjTH4UHlJz3VYS/Sc0nvD5EcKBXWtfd4Ct7fY67catCPI0RVWzUMnHR2Q3Gspi1kKSU+97vZ9lYpMvITO94F2aqvZOmDJGoXGdnvb1Nd/s3lqfTp1iBd0nL2MIHlGzUFWaiuv+Op/qMiqIMtdRgUvUaw+ZRHh+0bsNf+vqLRTw7+5UjbeoyJ+X/ru92p26LqGGVIMK+Xvcj8Y7LInIYsGbuv+6hFq01Lgf0mkqjU2+KPovTIvd8IB1CTXMZVR23X097kfjHZak+HyBenKL7lcs1yXU7AByloGfQKNZQuVT8xYicJn1bdQ6ShUOSMftHaoaDTNXkDsvTQvXT6Ne945Ci1QXFemASmNTLCOTXTR64DLrFmoshHFOSP8YGs0Ss4tym9tVqnVPclJjKTuerJxijauuf91NFXDaObm0hd/F3s7q14UywIl8XJgCFmo8ADKXx/Y5Fmp3i3upyw2NHJMwxwlS+akHoAksvV3AJ+8lLsNy6baj+ly5UhXo1BzwMpujX59a+7zxxTJ6F8ru5lM3JFQjhAlZ5nyqlFqoAUX1FDz+AOEjd98+IVE5nAM91Q81Q+nXJ1cXa0UgoQ755Yc1n0a9XjY0nmU4o85NtbenaAKIEunnP7yySN9/3edGqpZ3LS5cw0G4yIaE+hKnqQjGawjQ4Wma91B+6b67aps1q87xUu7BWrBVbXVzLOXGB16FuUJFpIUaQKLh2s+NNVghLTXUndQhv2Oz2AKX2LhQC/YEldEgHJ6m2Thqr1ZPjdOmLs3CtanUGxaqvY2aMCqlFqqmypWse6dRb27WpcR/kZ5NpVmiJGT7hWm4UqXa1NENc8/Qzzr+UCihul7r9RMm351tHDbs3w4MbSP0p4DpeWCG092jkxKvna09Wd5ISAFzvmBXqKbgMJs/Y0SNT5fSs70zfmDPtmoO8uZNeLuXb1XCT48BvzghMer42+VvhET85BV5cN82cjyfuukx15wPPokmQu38/NSHaNWdomo//p8cJmSarMCsTk/JFanXjTTVpoXK/4Nn0EQogQ5uXfu6FEfKB3mdiXk+8ra+WEImLk47P0Rt00KdeZrGm2V3qkrTPDBItlVdCzWGR10fc72l2F9ULLROXMd2OIwjJ1yQEL9GE6C2MresY3tyqo3Q1mRj5ewKVcGHFlUhDUMf87MCqqpj1uWwm/qSLyF9ftLZTJAzf0a1O1Vvo9YssVgSmYsz2AEHcep+zxJId/1rbCQoMl9Ct5Onpzgi1KSq/BPegEYD+5DdWL6I7pLfhDo6YNf71X5/3U2lsSlZsoMtqmOTsZxZ+o9SpXp6ip7zr6lSKCKdc3Dcj3MxaQtG9Sl/mhsULfRML4iUU4f8OiZUdcgvSXvp11ZVY5PNGz1lC44cY+eYUNUhvxI4Bd32p3EBR9PRwsBJEI1Co3EYR4XaVcFxgp5LpXEeR4W6tPyrfKpe/jXIFWXGqVyq45VoU52bSrgGTdPTlZAXWmLOBNfOC7U6lVpbVA2SLcZkyFBHP2wex4U69ay91/+M3kbd3LBA82ETZTiEK01o7Kd+n/+jl/8mJhLC1bYIFuAQrgjVMHTNv9npaqVTmSRm4RCuCFUd8gtdTm1aWqN0bmdavBEN+3zpVxBpq9qM8GpaTiZw+t5e4xwcxDWhcqj3Q2iaCoNQ7m7Bax/chpfhMK4JVRp25D8OTdMQD8uLD2ynn+1IO7+auibU+e8QR/2kI/8moSOBE4/eY3zPDZEqXN0jyX7qv0PT8HTE5W8fHKB/294N14YYbX721CqQgRekxJ9xTtXVo1003sMJ/bmWGI1tS+LVD91Fb7BFzcNFXBUqm+trAmqKihyBpiEIGZSLR+TlbUl69d67cDzdijl4gLtCDSGLijjBtlULNcCw9Sy3RXG+LY4L7TExOZAyTu9Ke5t+dFWoU2lcS1403pZY+6wijX/ZksTlB3fRD9kHvRoPk2NJ/PXg7sCZ6u7UUeht1IEmFkJxKIPxuIOVpvXi+mSkchSvEJGeTeUjOBvjSOudl7i69CuMCioc+eudqXVEVYxCJuZU6x0/LvZ12gHQhxAgXBeqSvx3HLHOQpISa5MNYawfKjqPhuR0JEyzHKVP9XfSO92tmO7vwuSOLRiEFurtEBmvcTh1ElIOQ+MKttXk3GY0hOmOBJ1rj2My1UJXt3RiMt1KnqSQ3MQTofK/MspuuA6oXCAcQmkwTc9FQsj3dojxRNTIb01i1qktIH7BE6GqQ36PX8T/SMJBXaVylu4WTD2yB79Uj6Nho25Rudt4Mg9ZHfILtYVan/LnOFymlqpBOVrH1JEXeDa4OybwHDurejjFHSBAcACUj0VoOmw0tug2gjc+KjPxLI12HJFKqAcQULILwKtnJCav13b9FIcwhTUkx8n0ibY4neMgaC4Zx8S+PsyUyriPU3o1zRa1KuuvtS+WMJdbxG9qvb5YwlXUGc+EqmCr8UOO/r+EgJLNAz9/e31nR73/qEmOzhcTUVxKROlyVxxjqVZMZJK4ykFRuWupA+ncFVzipLwjU/BWYjaHSb6Bnqv1er5pLNQZT4UajuKFUsBT/8pCFta5MEdNTBmGLO5IGS/2tONSSxiL7S3IdcSRXyk6X1h09+AOy4LI5VHjuuAPCB7Dy/+xjeRTt3UCu9MqsoXvUTnNRBjjHS10jpPs57gSNM7izHcksNjoQY9beGpRFbye/blF+NdGSVOxRcyZprKMlGuNystD2+i/lcXckdJ5YyfxXKjCwisw6QX2fB5fz/dVBHyRwTYNFDhVUopFMJGIYLIjRpfT7Rjr68bl7hbSZ225hOdCzQ4glxzD95eS/6lav69csc/ZBOqw9IdNVemRC1yafJet5lQsZOQzXAXas9UYg8YTPBeq6lENf0O+UiraVtW3GQC2nLlkAu/EOa+ZbhXnExEj35/CeFeClvYGNeHZkXXEe6Gi2vnfzlaVg44hTn3UFFgVK+rsIrgKp50m2mM4m2qlcyo672nDDOc5yx0Jw9WNa5q18Tzqv5n2J+QIGfJ7/HBorWvjEWBvL9e2NxmCqQoQ5ygtw8Bi2JA5tpiXu1vp7EM78IrbOyk1G6euQlV0HpF/zEWAP5JS7lvtus0IVYlTLeWREK6zMCdippzbv51+qSPz4FB3oSq6jshDlpT/gFUsK4vMFipH2DWhutlZ3OMmEUfocibdTqdUPpOrP3PacgYPXwhV0c5iNSQek9W01cBK1+zJAHd12wn1FYmYmDYNuZhM0Km2mJy6qwtnEnEOgpKYhia4SCz4RqiKgSdkLMtWVRriEJHxKSHlAbpp+8rOHoCXa4RMFZVTIWKKa5EwXefo/EymnS60RcVcyDTKvZ2YjusKUMPABuy0r4R6AyXY6Qha2a9shYUkiapLsKMb7Y99EHuTcYS2dWM8yqVKMwShIvOWKIrQNCRcyfyFL4W6Gj8/Jg+bhD5omgPO0JhR/ChwWWtTH7bWVPBqeq0cRTlwQrUqOjBqJqSJc1MDARTqlgM4z3dZ4Lf/atbGNJHlauHlLxBZgRPqXs6LyhBOQNPwkMTYhbeqrl4gOyvMOYxyNiBQHeqa9aFikUoB41/4AtkdHoEU6uWHubJk4P+49LoITePBkT6pZb/tvQpiIIWqfJaFVlyMhHGKfwGd2G8kBCxLYjwUwusP3UPLgXNgmyoP76biwiJOVSTOq18OmoaADBS5ePPazSJVBLr795MH6HqkDb/lFIZuMmkUTLz5fpEqAt+mfmkXpgsVvEparMFGNZ6EcfydIt5Z6eXAlVBX41dvyvuF2jEg3RveoHEYDpxQwayI4TeP7qU79gfXZSuKW3SZeGuGvVUhsIfvwIgkRKDxLSprw+/TpVgCb6y03N9MQ1lUxU/fldGWGfQaEWznQCtlmOjQFtZfGIQC25PZioWzeYHR3z1Aa7ptDSfUG7x4XibDs0gQIc31t11SohuausJuWYmDokle7MeEibFH76OaG4waVqg3c+yYTPJfJCkNbAkLxC1CP//icWhchV2wOV7KylzyPl0qo5IIYTpaRu75+1E4SrSueSJNIVTFUSmNT78Oc3ISRjoNc95k/1UgFY4hapXYPVhCWEhDs27Y1ZpRYZGymuxqZRFDrlTBfHcZ8vnn+TnmaHWg84b4f4Z+QcathFhWAAAAAElFTkSuQmCC"
          />
        </>
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
            withOutLeftBorder={!executeMode}
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
                // isDeveloper &&
                integrateSystem !== 'c2' &&
                isDebugMode &&
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
                queryMode={parseInfo?.queryMode}
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

                    if (key === 'exportData' && !!data?.queryResults?.length) {
                      exportData(data);
                    }
                  },
                  items: [
                    {
                      label: '导出数据',
                      disabled: !data?.queryResults?.length,
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
            !isSimpleMode &&
            parseInfo?.queryMode !== 'PLAIN_TEXT' && (
              <SimilarQuestionItem
                queryId={parseInfo?.queryId}
                defaultExpanded={parseTip !== '' || executeTip !== ''}
                similarQueries={data?.similarQueries}
                onSelectQuestion={onSelectQuestion}
              />
            )} */}
        </div>
        {(parseTip !== '' || (executeMode && !executeLoading)) &&
          parseInfo?.queryMode !== 'PLAIN_TEXT' && (
            <Tools queryId={parseInfo?.queryId || 0} scoreValue={score} />
          )}
      </div>
    </div>
  );
};

export default ChatItem;

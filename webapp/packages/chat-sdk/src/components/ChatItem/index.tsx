import {
  ChatContextType,
  DateInfoType,
  EntityInfoType,
  FilterItemType,
  MsgDataType,
  ParseStateEnum,
  SimilarQuestionType,
} from '../../common/type';
import { useEffect, useState } from 'react';
import { chatExecute, chatParse, queryData, queryEntityInfo, switchEntity } from '../../service';
import { PARSE_ERROR_TIP, PREFIX_CLS, SEARCH_EXCEPTION_TIP } from '../../common/constants';
import IconFont from '../IconFont';
import ParseTip from './ParseTip';
import ExecuteItem from './ExecuteItem';
import { isMobile } from '../../utils/utils';
import classNames from 'classnames';
import Tools from '../Tools';
import SqlItem from './SqlItem';
import SimilarQuestionItem from './SimilarQuestionItem';
import moment from 'moment';

type Props = {
  msg: string;
  conversationId?: number;
  modelId?: number;
  agentId?: number;
  score?: number;
  filter?: any[];
  isLastMessage?: boolean;
  parseInfos?: ChatContextType[];
  msgData?: MsgDataType;
  triggerResize?: boolean;
  isDeveloper?: boolean;
  integrateSystem?: string;
  executeItemNode?: React.ReactNode;
  renderCustomExecuteNode?: boolean;
  onMsgDataLoaded?: (data: MsgDataType, valid: boolean, isRefresh?: boolean) => void;
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
  isLastMessage,
  triggerResize,
  parseInfos,
  msgData,
  isDeveloper,
  integrateSystem,
  executeItemNode,
  renderCustomExecuteNode,
  onMsgDataLoaded,
  onUpdateMessageScroll,
  onSendMsg,
}) => {
  const [data, setData] = useState<MsgDataType>();
  const [parseLoading, setParseLoading] = useState(false);
  const [parseInfo, setParseInfo] = useState<ChatContextType>();
  const [parseInfoOptions, setParseInfoOptions] = useState<ChatContextType[]>([]);
  const [parseTip, setParseTip] = useState('');
  const [executeLoading, setExecuteLoading] = useState(false);
  const [executeTip, setExecuteTip] = useState('');
  const [executeMode, setExecuteMode] = useState(false);
  const [entitySwitchLoading, setEntitySwitchLoading] = useState(false);
  const [dimensionFilters, setDimensionFilters] = useState<FilterItemType[]>([]);
  const [dateInfo, setDateInfo] = useState<DateInfoType>({} as DateInfoType);
  const [entityInfo, setEntityInfo] = useState<EntityInfoType>({} as EntityInfoType);

  const prefixCls = `${PREFIX_CLS}-item`;

  const updateData = (res: Result<MsgDataType>) => {
    if (res.code === 401 || res.code === 412) {
      setExecuteTip(res.msg);
      return false;
    }
    if (res.code !== 200) {
      setExecuteTip(SEARCH_EXCEPTION_TIP);
      return false;
    }
    const { queryColumns, queryResults, queryState, queryMode, response } = res.data || {};
    if (queryState !== 'SUCCESS') {
      setExecuteTip(response && typeof response === 'string' ? response : SEARCH_EXCEPTION_TIP);
      return false;
    }
    if ((queryColumns && queryColumns.length > 0 && queryResults) || queryMode === 'WEB_PAGE') {
      setData(res.data);
      setExecuteTip('');
      return true;
    }
    setExecuteTip(SEARCH_EXCEPTION_TIP);
    return true;
  };

  const onExecute = async (parseInfoValue: ChatContextType) => {
    setExecuteMode(true);
    setExecuteLoading(true);
    try {
      const res: any = await chatExecute(msg, conversationId!, parseInfoValue);
      setExecuteLoading(false);
      const valid = updateData(res);
      onMsgDataLoaded?.(
        {
          ...res.data,
          chatContext: parseInfoValue,
        },
        valid
      );
    } catch (e) {
      setExecuteLoading(false);
      setExecuteTip(SEARCH_EXCEPTION_TIP);
    }
  };

  const sendMsg = async () => {
    setParseLoading(true);
    const parseData: any = await chatParse(msg, conversationId, modelId, agentId, filter);
    setParseLoading(false);
    const { code, data } = parseData || {};
    const { state, selectedParses, candidateParses, queryId } = data || {};
    if (
      code !== 200 ||
      state === ParseStateEnum.FAILED ||
      !selectedParses?.length ||
      (!selectedParses[0]?.properties?.type && !selectedParses[0]?.queryMode)
    ) {
      setParseTip(PARSE_ERROR_TIP);
      return;
    }
    onUpdateMessageScroll?.();
    const parseInfos = selectedParses
      .concat(candidateParses || [])
      .slice(0, 5)
      .map((item: any) => ({
        ...item,
        queryId,
      }));
    setParseInfoOptions(parseInfos || []);
    const parseInfoValue = parseInfos[0];
    setParseInfo(parseInfoValue);
    setEntityInfo(parseInfoValue.entityInfo || {});
    setDimensionFilters(parseInfoValue?.dimensionFilters || []);
    setDateInfo(parseInfoValue?.dateInfo);
    onExecute(parseInfoValue);
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
      setDimensionFilters(parseInfoValue.dimensionFilters || []);
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
    const { chatContext, entityInfo } = res.data;
    const chatContextValue = { ...(chatContext || {}), queryId: parseInfo?.queryId };
    setParseInfo(chatContextValue);
    setEntityInfo(entityInfo);
    setDimensionFilters(chatContextValue?.dimensionFilters || []);
    setDateInfo(chatContextValue?.dateInfo);
  };

  const onFiltersChange = (dimensionFilters: FilterItemType[]) => {
    setDimensionFilters(dimensionFilters);
  };

  const onDateInfoChange = (dateRange: any) => {
    setDateInfo({
      ...(dateInfo || {}),
      startDate: moment(dateRange[0]).format('YYYY-MM-DD'),
      endDate: moment(dateRange[1]).format('YYYY-MM-DD'),
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
      const dataValue = { ...res.data, chatContext: contextValue };
      onMsgDataLoaded?.(dataValue, true, true);
      setData(dataValue);
      setParseInfo(contextValue);
    }
  };

  const getEntityInfo = async (parseInfoValue: ChatContextType) => {
    const res = await queryEntityInfo(parseInfoValue.queryId, parseInfoValue.id);
    setEntityInfo(res.data);
  };

  const onSelectParseInfo = async (parseInfoValue: ChatContextType) => {
    setParseInfo(parseInfoValue);
    setDimensionFilters(parseInfoValue.dimensionFilters || []);
    setDateInfo(parseInfoValue.dateInfo);
    if (parseInfoValue.entityInfo) {
      setEntityInfo(parseInfoValue.entityInfo);
    } else {
      getEntityInfo(parseInfoValue);
    }
  };

  const onSelectQuestion = (question: SimilarQuestionType) => {
    onSendMsg?.(question.queryText);
  };

  const contentClass = classNames(`${prefixCls}-content`, {
    [`${prefixCls}-content-mobile`]: isMobile,
  });

  return (
    <div className={prefixCls}>
      {!isMobile && integrateSystem !== 'wiki' && (
        <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} />
      )}
      <div className={isMobile ? `${prefixCls}-mobile-msg-card` : `${prefixCls}-msg-card`}>
        <div className={contentClass}>
          <ParseTip
            parseLoading={parseLoading}
            parseInfoOptions={parseInfoOptions}
            parseTip={parseTip}
            currentParseInfo={parseInfo}
            agentId={agentId}
            dimensionFilters={dimensionFilters}
            dateInfo={dateInfo}
            entityInfo={entityInfo}
            integrateSystem={integrateSystem}
            onSelectParseInfo={onSelectParseInfo}
            onSwitchEntity={onSwitchEntity}
            onFiltersChange={onFiltersChange}
            onDateInfoChange={onDateInfoChange}
          />
          {executeMode && (
            <>
              {!isMobile && parseInfo?.sqlInfo && isDeveloper && integrateSystem !== 'c2' && (
                <SqlItem integrateSystem={integrateSystem} sqlInfo={parseInfo.sqlInfo} />
              )}
              <ExecuteItem
                queryId={parseInfo?.queryId}
                executeLoading={executeLoading}
                entitySwitchLoading={entitySwitchLoading}
                executeTip={executeTip}
                chartIndex={0}
                data={data}
                triggerResize={triggerResize}
                executeItemNode={executeItemNode}
                renderCustomExecuteNode={renderCustomExecuteNode}
                onRefresh={onRefresh}
              />
            </>
          )}
          {(parseTip !== '' || (executeMode && !executeLoading)) && integrateSystem !== 'c2' && (
            <SimilarQuestionItem
              queryText={msg || msgData?.queryText || ''}
              agentId={agentId}
              defaultExpanded={parseTip !== '' || executeTip !== '' || integrateSystem === 'wiki'}
              onSelectQuestion={onSelectQuestion}
            />
          )}
        </div>
        {(parseTip !== '' || (executeMode && !executeLoading)) &&
          integrateSystem !== 'c2' &&
          integrateSystem !== 'showcase' && (
            <Tools queryId={parseInfo?.queryId || 0} scoreValue={score} />
          )}
      </div>
    </div>
  );
};

export default ChatItem;

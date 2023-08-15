import { ChatContextType, MsgDataType, ParseStateEnum } from '../../common/type';
import { useEffect, useState } from 'react';
import { chatExecute, chatParse, switchEntity } from '../../service';
import { PARSE_ERROR_TIP, PREFIX_CLS, SEARCH_EXCEPTION_TIP } from '../../common/constants';
import IconFont from '../IconFont';
import ParseTip from './ParseTip';
import ExecuteItem from './ExecuteItem';

type Props = {
  msg: string;
  conversationId?: number;
  modelId?: number;
  filter?: any[];
  isLastMessage?: boolean;
  msgData?: MsgDataType;
  isMobileMode?: boolean;
  triggerResize?: boolean;
  parseOptions?: ChatContextType[];
  onMsgDataLoaded?: (data: MsgDataType, valid: boolean) => void;
  onUpdateMessageScroll?: () => void;
};

const ChatItem: React.FC<Props> = ({
  msg,
  conversationId,
  modelId,
  filter,
  isLastMessage,
  isMobileMode,
  triggerResize,
  msgData,
  parseOptions,
  onMsgDataLoaded,
  onUpdateMessageScroll,
}) => {
  const [data, setData] = useState<MsgDataType>();
  const [parseLoading, setParseLoading] = useState(false);
  const [parseInfo, setParseInfo] = useState<ChatContextType>();
  const [parseInfoOptions, setParseInfoOptions] = useState<ChatContextType[]>(parseOptions || []);
  const [parseTip, setParseTip] = useState('');
  const [executeLoading, setExecuteLoading] = useState(false);
  const [executeTip, setExecuteTip] = useState('');
  const [executeMode, setExecuteMode] = useState(false);
  const [entitySwitching, setEntitySwitching] = useState(false);

  const [chartIndex, setChartIndex] = useState(0);

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

  const onExecute = async (
    parseInfoValue: ChatContextType,
    parseInfoOptions?: ChatContextType[]
  ) => {
    setExecuteMode(true);
    setExecuteLoading(true);
    const { data } = await chatExecute(msg, conversationId!, parseInfoValue);
    setExecuteLoading(false);
    const valid = updateData(data);
    if (onMsgDataLoaded) {
      let parseOptions: ChatContextType[] = parseInfoOptions || [];
      if (
        parseInfoOptions &&
        parseInfoOptions.length > 1 &&
        (parseInfoOptions[0].queryMode.includes('METRIC') ||
          parseInfoOptions[0].queryMode.includes('ENTITY'))
      ) {
        parseOptions = parseInfoOptions.filter(
          (item, index) =>
            index === 0 ||
            (!item.queryMode.includes('METRIC') && !item.queryMode.includes('ENTITY'))
        );
      }
      onMsgDataLoaded(
        {
          ...data.data,
          chatContext: parseInfoValue,
          parseOptions: parseOptions.length > 1 ? parseOptions.slice(1) : undefined,
        },
        valid
      );
    }
  };

  const onSendMsg = async () => {
    setParseLoading(true);
    const { data: parseData } = await chatParse(msg, conversationId, modelId, filter);
    setParseLoading(false);
    const { code, data } = parseData || {};
    const { state, selectedParses } = data || {};
    if (
      code !== 200 ||
      state === ParseStateEnum.FAILED ||
      selectedParses == null ||
      selectedParses.length === 0 ||
      (selectedParses.length === 1 &&
        !selectedParses[0]?.modelName &&
        !selectedParses[0]?.properties?.CONTEXT?.plugin?.name &&
        selectedParses[0]?.queryMode !== 'WEB_PAGE')
    ) {
      setParseTip(PARSE_ERROR_TIP);
      return;
    }
    if (onUpdateMessageScroll) {
      onUpdateMessageScroll();
    }
    setParseInfoOptions(selectedParses || []);
    const parseInfoValue = selectedParses[0];
    setParseInfo(parseInfoValue);
    onExecute(parseInfoValue, selectedParses);
  };

  useEffect(() => {
    if (data !== undefined || parseOptions !== undefined || executeTip !== '') {
      return;
    }
    if (msgData) {
      setParseInfoOptions([msgData.chatContext]);
      setExecuteMode(true);
      updateData({ code: 200, data: msgData, msg: 'success' });
    } else if (msg) {
      onSendMsg();
    }
  }, [msg, msgData]);

  const onSwitchEntity = async (entityId: string) => {
    setEntitySwitching(true);
    const res = await switchEntity(entityId, data?.chatContext?.modelId, conversationId || 0);
    setEntitySwitching(false);
    setData(res.data.data);
  };

  const onChangeChart = () => {
    setChartIndex(chartIndex + 1);
  };

  const onSelectParseInfo = async (parseInfoValue: ChatContextType) => {
    setParseInfo(parseInfoValue);
    onExecute(parseInfoValue);
    if (onUpdateMessageScroll) {
      onUpdateMessageScroll();
    }
  };

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-section`}>
        <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} />
        <div className={`${prefixCls}-content`}>
          <ParseTip
            parseLoading={parseLoading}
            parseInfoOptions={parseOptions || parseInfoOptions.slice(0, 1)}
            parseTip={parseTip}
            currentParseInfo={parseInfo}
            optionMode={parseOptions !== undefined}
            onSelectParseInfo={onSelectParseInfo}
          />
        </div>
      </div>
      {executeMode && data?.queryMode !== 'WEB_PAGE' && (
        <div className={`${prefixCls}-section`}>
          <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} />
          <div className={`${prefixCls}-content`}>
            <ExecuteItem
              question={msg}
              executeLoading={executeLoading}
              executeTip={executeTip}
              chartIndex={chartIndex}
              data={data}
              isMobileMode={isMobileMode}
              isLastMessage={isLastMessage}
              triggerResize={triggerResize}
              onSwitchEntity={onSwitchEntity}
              onChangeChart={onChangeChart}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatItem;

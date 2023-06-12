import { MsgDataType, MsgValidTypeEnum, SuggestionDataType } from '../../common/type';
import { useEffect, useState } from 'react';
import Typing from './Typing';
import ChatMsg from '../ChatMsg';
import { querySuggestionInfo, chatQuery } from '../../service';
import { MSG_VALID_TIP, PARSE_ERROR_TIP, PREFIX_CLS } from '../../common/constants';
import Text from './Text';
import Suggestion from '../Suggestion';
import Tools from '../Tools';
import SemanticDetail from '../SemanticDetail';

type Props = {
  msg: string;
  conversationId?: number;
  classId?: number;
  isLastMessage?: boolean;
  suggestionEnable?: boolean;
  msgData?: MsgDataType;
  onLastMsgDataLoaded?: (data: MsgDataType) => void;
  onSelectSuggestion?: (value: string) => void;
  onUpdateMessageScroll?: () => void;
};

const ChatItem: React.FC<Props> = ({
  msg,
  conversationId,
  classId,
  isLastMessage,
  suggestionEnable,
  msgData,
  onLastMsgDataLoaded,
  onSelectSuggestion,
  onUpdateMessageScroll,
}) => {
  const [data, setData] = useState<MsgDataType>();
  const [suggestionData, setSuggestionData] = useState<SuggestionDataType>();
  const [loading, setLoading] = useState(false);
  const [metricInfoList, setMetricInfoList] = useState<any[]>([]);
  const [tip, setTip] = useState('');

  const setMsgData = (value: MsgDataType) => {
    setData(value);
  };

  const updateData = (res: Result<MsgDataType>) => {
    if (res.code === 401) {
      setTip(res.msg);
      return false;
    }
    if (res.code !== 200) {
      setTip(PARSE_ERROR_TIP);
      return false;
    }
    const { queryColumns, queryResults, queryState } = res.data || {};
    if (queryState !== MsgValidTypeEnum.NORMAL && queryState !== MsgValidTypeEnum.EMPTY) {
      setTip(MSG_VALID_TIP[queryState || MsgValidTypeEnum.INVALID]);
      return false;
    }
    if (queryColumns && queryColumns.length > 0 && queryResults) {
      setMsgData(res.data);
      setTip('');
      return true;
    }
    setTip(PARSE_ERROR_TIP);
    return false;
  };

  const updateSuggestionData = (semanticRes: MsgDataType, suggestionRes: any) => {
    const { aggregateType, queryColumns, entityInfo } = semanticRes;
    setSuggestionData({
      currentAggregateType: aggregateType,
      columns: queryColumns || [],
      mainEntity: entityInfo,
      suggestions: suggestionRes,
    });
  };

  const getSuggestions = async (domainId: number, semanticResData: MsgDataType) => {
    if (!domainId) {
      return;
    }
    const res = await querySuggestionInfo(domainId);
    updateSuggestionData(semanticResData, res.data.data);
  };

  const onSendMsg = async () => {
    setLoading(true);
    const semanticRes = await chatQuery(msg, conversationId, classId);
    const semanticValid = updateData(semanticRes.data);
    if (suggestionEnable && semanticValid) {
      const semanticResData = semanticRes.data.data;
      await getSuggestions(semanticResData.entityInfo?.domainInfo?.itemId, semanticRes.data.data);
    } else {
      setSuggestionData(undefined);
    }
    if (onLastMsgDataLoaded) {
      onLastMsgDataLoaded(semanticRes.data.data);
    }
    setLoading(false);
  };

  useEffect(() => {
    if (msgData) {
      updateData({ code: 200, data: msgData, msg: 'success' });
    } else if (msg) {
      onSendMsg();
    }
  }, [msg, msgData]);

  if (loading) {
    return <Typing />;
  }

  if (tip) {
    return <Text data={tip} />;
  }

  if (!data) {
    return null;
  }

  const onCheckMetricInfo = (data: any) => {
    setMetricInfoList([...metricInfoList, data]);
    if (onUpdateMessageScroll) {
      onUpdateMessageScroll();
    }
  };

  const prefixCls = `${PREFIX_CLS}-item`;

  return (
    <div>
      <ChatMsg data={data} onCheckMetricInfo={onCheckMetricInfo} />
      <Tools isLastMessage={isLastMessage} />
      {suggestionEnable && suggestionData && isLastMessage && (
        <Suggestion {...suggestionData} onSelect={onSelectSuggestion} />
      )}
      <div className={`${prefixCls}-metric-info-list`}>
        {metricInfoList.map(item => (
          <SemanticDetail
            dataSource={item}
            onDimensionSelect={(value: string) => {
              if (onSelectSuggestion) {
                onSelectSuggestion(value);
              }
            }}
          />
        ))}
      </div>
    </div>
  );
};

export default ChatItem;

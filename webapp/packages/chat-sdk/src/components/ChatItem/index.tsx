import { MsgDataType, MsgValidTypeEnum } from '../../common/type';
import { useEffect, useState } from 'react';
import Typing from './Typing';
import ChatMsg from '../ChatMsg';
import { chatQuery } from '../../service';
import { MSG_VALID_TIP, PARSE_ERROR_TIP, PREFIX_CLS } from '../../common/constants';
import Text from './Text';
import Tools from '../Tools';
import SemanticDetail from '../SemanticDetail';
import IconFont from '../IconFont';

type Props = {
  msg: string;
  followQuestions?: string[];
  conversationId?: number;
  domainId?: number;
  isLastMessage?: boolean;
  msgData?: MsgDataType;
  isMobileMode?: boolean;
  triggerResize?: boolean;
  onMsgDataLoaded?: (data: MsgDataType) => void;
  onSelectSuggestion?: (value: string) => void;
  onUpdateMessageScroll?: () => void;
};

const ChatItem: React.FC<Props> = ({
  msg,
  followQuestions,
  conversationId,
  domainId,
  isLastMessage,
  isMobileMode,
  triggerResize,
  msgData,
  onMsgDataLoaded,
  onSelectSuggestion,
  onUpdateMessageScroll,
}) => {
  const [data, setData] = useState<MsgDataType>();
  const [loading, setLoading] = useState(false);
  const [metricInfoList, setMetricInfoList] = useState<any[]>([]);
  const [tip, setTip] = useState('');

  const updateData = (res: Result<MsgDataType>) => {
    if (res.code === 401 || res.code === 412) {
      setTip(res.msg);
      return false;
    }
    if (res.code !== 200) {
      setTip(PARSE_ERROR_TIP);
      return false;
    }
    const { queryColumns, queryResults, queryState, queryMode } = res.data || {};
    if (queryState !== MsgValidTypeEnum.NORMAL && queryState !== MsgValidTypeEnum.EMPTY) {
      setTip(MSG_VALID_TIP[queryState || MsgValidTypeEnum.INVALID]);
      return false;
    }
    if ((queryColumns && queryColumns.length > 0 && queryResults) || queryMode === 'INSTRUCTION') {
      setData(res.data);
      setTip('');
      return true;
    }
    setTip(PARSE_ERROR_TIP);
    return false;
  };

  const onSendMsg = async () => {
    setLoading(true);
    const semanticRes = await chatQuery(msg, conversationId, domainId);
    updateData(semanticRes.data);
    if (onMsgDataLoaded) {
      onMsgDataLoaded(semanticRes.data.data);
    }
    setLoading(false);
  };

  useEffect(() => {
    if (data !== undefined) {
      return;
    }
    if (msgData) {
      updateData({ code: 200, data: msgData, msg: 'success' });
    } else if (msg) {
      onSendMsg();
    }
  }, [msg, msgData]);

  const prefixCls = `${PREFIX_CLS}-item`;

  if (loading) {
    return (
      <div className={prefixCls}>
        <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} />
        <Typing />
      </div>
    );
  }

  if (tip) {
    return (
      <div className={prefixCls}>
        <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} />
        <Text data={tip} />
      </div>
    );
  }

  if (!data || data.queryMode === 'INSTRUCTION') {
    return null;
  }

  const onCheckMetricInfo = (data: any) => {
    setMetricInfoList([...metricInfoList, data]);
    if (onUpdateMessageScroll) {
      onUpdateMessageScroll();
    }
  };

  return (
    <div className={prefixCls}>
      <IconFont type="icon-zhinengsuanfa" className={`${prefixCls}-avatar`} />
      <div className={`${prefixCls}-content`}>
        <ChatMsg
          question={msg}
          followQuestions={followQuestions}
          data={data}
          isMobileMode={isMobileMode}
          triggerResize={triggerResize}
          onCheckMetricInfo={onCheckMetricInfo}
        />
        <Tools data={data} isLastMessage={isLastMessage} isMobileMode={isMobileMode} />
        {metricInfoList.length > 0 && (
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
        )}
      </div>
    </div>
  );
};

export default ChatItem;

import { MsgDataType } from '../../common/type';
import { useEffect, useState } from 'react';
import Typing from './Typing';
import ChatMsg from '../ChatMsg';
import { chatQuery } from '../../service';
import { PARSE_ERROR_TIP, PREFIX_CLS } from '../../common/constants';
import Text from './Text';
import Tools from '../Tools';
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
}) => {
  const [data, setData] = useState<MsgDataType>();
  const [loading, setLoading] = useState(false);
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
    if (queryState !== 'SUCCESS') {
      setTip(PARSE_ERROR_TIP);
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

  const isMetricCard =
    (data.queryMode === 'METRIC_DOMAIN' || data.queryMode === 'METRIC_FILTER') &&
    data.queryResults?.length === 1;

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
        />
        {!isMetricCard && (
          <Tools data={data} isLastMessage={isLastMessage} isMobileMode={isMobileMode} />
        )}
      </div>
    </div>
  );
};

export default ChatItem;

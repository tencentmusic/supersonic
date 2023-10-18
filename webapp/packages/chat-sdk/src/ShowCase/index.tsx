import { useEffect, useState } from 'react';
import styles from './style.module.less';
import { ShowCaseMapType } from './type';
import { queryShowCase } from './service';
import Text from '../Chat/components/Text';
import ChatItem from '../components/ChatItem';
import { HistoryMsgItemType } from '../common/type';
import { Spin } from 'antd';

type Props = {
  agentId: number;
  onSendMsg?: (msg: string) => void;
};

const ShowCase: React.FC<Props> = ({ agentId, onSendMsg }) => {
  const [showCaseMap, setShowCaseMap] = useState<ShowCaseMapType>({});
  const [loading, setLoading] = useState(false);

  const updateData = async (pageNo: number) => {
    if (pageNo === 1) {
      setLoading(true);
    }
    const res = await queryShowCase(agentId, pageNo, 30);
    if (pageNo === 1) {
      setLoading(false);
    }
    setShowCaseMap(
      pageNo === 1 ? res.data.showCaseMap : { ...showCaseMap, ...res.data.showCaseMap }
    );
  };

  useEffect(() => {
    if (agentId) {
      updateData(1);
    }
  }, [agentId]);

  return (
    <Spin spinning={loading} size="large">
      <div className={styles.showCase}>
        {Object.keys(showCaseMap || {}).map(key => {
          const showCaseItem = showCaseMap?.[key] || [];
          return (
            <div key={key} className={styles.showCaseItem}>
              {showCaseItem
                .filter((chatItem: HistoryMsgItemType) => !!chatItem.queryResult)
                .slice(0, 10)
                .map((chatItem: HistoryMsgItemType) => {
                  return (
                    <div className={styles.showCaseChatItem} key={chatItem.questionId}>
                      <Text position="right" data={chatItem.queryText} anonymousUser />
                      <ChatItem
                        msg={chatItem.queryText}
                        msgData={chatItem.queryResult}
                        conversationId={chatItem.chatId}
                        agentId={agentId}
                        integrateSystem="showcase"
                        onSendMsg={onSendMsg}
                      />
                    </div>
                  );
                })}
            </div>
          );
        })}
      </div>
    </Spin>
  );
};

export default ShowCase;

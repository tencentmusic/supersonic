import { useEffect, useState } from 'react';
import styles from './style.module.less';
import { ShowCaseType } from './type';
import { queryShowCase } from './service';
import Text from '../Chat/components/Text';
import ChatItem from '../components/ChatItem';
import { HistoryMsgItemType } from '../common/type';
import { Spin } from 'antd';

type Props = {
  agentId: number;
};

const ShowCase: React.FC<Props> = ({ agentId }) => {
  const [data, setData] = useState<ShowCaseType>();
  const [loading, setLoading] = useState(false);

  const updateData = async () => {
    setLoading(true);
    const res = await queryShowCase(agentId);
    setLoading(false);
    setData(res.data);
  };

  useEffect(() => {
    if (agentId) {
      updateData();
    }
  }, [agentId]);

  return (
    <Spin spinning={loading}>
      <div className={styles.showCase}>
        {data &&
          Object.keys(data.showCaseMap || []).map(key => {
            const showCaseItem = data.showCaseMap[key];
            return (
              <div key={key} className={styles.showCaseItem}>
                {showCaseItem
                  .filter((chatItem: HistoryMsgItemType) => !!chatItem.queryResult)
                  .slice(0, 10)
                  .map((chatItem: HistoryMsgItemType) => {
                    return (
                      <div className={styles.showCaseChatItem} key={key}>
                        <Text position="right" data={chatItem.queryText} />
                        <ChatItem
                          msg={chatItem.queryText}
                          msgData={chatItem.queryResult}
                          conversationId={chatItem.chatId}
                          agentId={agentId}
                          integrateSystem="showcase"
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

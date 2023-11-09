import { useEffect, useRef, useState } from 'react';
import styles from './style.module.less';
import { ShowCaseItemType } from './type';
import { queryShowCase } from './service';
import Text from '../Chat/components/Text';
import ChatItem from '../components/ChatItem';
import { HistoryMsgItemType } from '../common/type';
import { Spin } from 'antd';
import classNames from 'classnames';
import { isMobile } from '../utils/utils';
import { useThrottleFn } from 'ahooks';

type Props = {
  height?: number | string;
  agentId: number;
  onSendMsg?: (msg: string) => void;
};

const ShowCase: React.FC<Props> = ({ height, agentId, onSendMsg }) => {
  const [showCaseList, setShowCaseList] = useState<ShowCaseItemType[]>([]);
  const [loading, setLoading] = useState(false);
  const [pageNo, setPageNo] = useState(1);

  const showcaseRef = useRef<any>(null);

  const updateData = async (pageNoValue: number) => {
    if (pageNoValue === 1) {
      setLoading(true);
    }
    const res = await queryShowCase(agentId, pageNoValue, isMobile ? 10 : 20);
    if (pageNoValue === 1) {
      setLoading(false);
    }
    const showCaseMapRes: any = res.data.showCaseMap;
    const list = Object.keys(showCaseMapRes)
      .reduce((result: ShowCaseItemType[], key: string) => {
        result.push({ msgList: showCaseMapRes[key], caseId: key });
        return result;
      }, [])
      .sort((a, b) => {
        return (b.msgList?.[0]?.score || 3) - (a.msgList?.[0]?.score || 3);
      });
    setShowCaseList(pageNoValue === 1 ? list : [...showCaseList, ...list]);
  };

  const { run: handleScroll } = useThrottleFn(
    e => {
      const bottom =
        e.target.scrollHeight - e.target.scrollTop === e.target.clientHeight ||
        e.target.scrollHeight - e.target.scrollTop === e.target.clientHeight + 0.5;
      if (bottom) {
        updateData(pageNo + 1);
        setPageNo(pageNo + 1);
      }
    },
    {
      leading: true,
      trailing: true,
      wait: 200,
    }
  );

  useEffect(() => {
    if (isMobile) {
      return;
    }
    const el = showcaseRef.current;
    el?.addEventListener('scroll', handleScroll);
    return () => {
      el.removeEventListener('scroll', handleScroll);
    };
  }, []);

  useEffect(() => {
    if (agentId) {
      setShowCaseList([]);
      updateData(1);
      setPageNo(1);
    }
  }, [agentId]);

  const showCaseClass = classNames(styles.showCase, { [styles.mobile]: isMobile });

  return (
    <Spin spinning={loading} size="large">
      <div className={showCaseClass} style={{ height }} ref={showcaseRef}>
        <div className={styles.showCaseContent}>
          {showCaseList.map(showCaseItem => {
            return (
              <div key={showCaseItem.caseId} className={styles.showCaseItem}>
                {showCaseItem.msgList
                  .filter((chatItem: HistoryMsgItemType) => !!chatItem.queryResult)
                  .slice(0, 1)
                  .map((chatItem: HistoryMsgItemType) => {
                    return (
                      <div className={styles.showCaseChatItem} key={chatItem.questionId}>
                        <Text position="right" data={chatItem.queryText} anonymousUser />
                        <ChatItem
                          msg={chatItem.queryText}
                          parseInfos={chatItem.parseInfos}
                          msgData={chatItem.queryResult}
                          conversationId={chatItem.chatId}
                          agentId={agentId}
                          integrateSystem="showcase"
                          score={chatItem.score}
                          onSendMsg={onSendMsg}
                        />
                      </div>
                    );
                  })}
              </div>
            );
          })}
        </div>
      </div>
    </Spin>
  );
};

export default ShowCase;

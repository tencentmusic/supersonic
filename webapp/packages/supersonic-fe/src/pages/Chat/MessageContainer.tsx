import Text from './components/Text';
import { memo, useCallback, useEffect, useState } from 'react';
import { isEqual } from 'lodash';
import { ChatItem } from 'supersonic-chat-sdk';
import type { MsgDataType } from 'supersonic-chat-sdk';
import { MessageItem, MessageTypeEnum } from './type';
import classNames from 'classnames';
import { Skeleton } from 'antd';
import styles from './style.less';

type Props = {
  id: string;
  chatId: number;
  messageList: MessageItem[];
  miniProgramLoading: boolean;
  isMobileMode?: boolean;
  onClickMessageContainer: () => void;
  onMsgDataLoaded: (data: MsgDataType, questionId: string | number) => void;
  onSelectSuggestion: (value: string) => void;
  onCheckMore: (data: MsgDataType) => void;
  onUpdateMessageScroll: () => void;
};

const MessageContainer: React.FC<Props> = ({
  id,
  chatId,
  messageList,
  miniProgramLoading,
  isMobileMode,
  onClickMessageContainer,
  onMsgDataLoaded,
  onSelectSuggestion,
  onUpdateMessageScroll,
}) => {
  const [triggerResize, setTriggerResize] = useState(false);

  const onResize = useCallback(() => {
    setTriggerResize(true);
    setTimeout(() => {
      setTriggerResize(false);
    }, 0);
  }, []);

  useEffect(() => {
    window.addEventListener('resize', onResize);
    return () => {
      window.removeEventListener('resize', onResize);
    };
  }, []);

  const messageListClass = classNames(styles.messageList, {
    [styles.miniProgramLoading]: miniProgramLoading,
  });

  const getFollowQuestions = (index: number) => {
    const followQuestions: string[] = [];
    const currentMsg = messageList[index];
    const currentMsgData = currentMsg.msgData;
    const msgs = messageList.slice(0, index).reverse();

    for (let i = 0; i < msgs.length; i++) {
      const msg = msgs[i];
      const msgDomainId = msg.msgData?.chatContext?.domainId;
      const msgEntityId = msg.msgData?.entityInfo?.entityId;
      const currentMsgDomainId = currentMsgData?.chatContext?.domainId;
      const currentMsgEntityId = currentMsgData?.entityInfo?.entityId;

      if (
        (msg.type === MessageTypeEnum.QUESTION || msg.type === MessageTypeEnum.INSTRUCTION) &&
        !!currentMsgDomainId &&
        !!currentMsgEntityId &&
        msgDomainId === currentMsgDomainId &&
        msgEntityId === currentMsgEntityId &&
        msg.msg
      ) {
        followQuestions.push(msg.msg);
      } else {
        break;
      }
    }
    return followQuestions;
  };

  return (
    <div id={id} className={styles.messageContainer} onClick={onClickMessageContainer}>
      {miniProgramLoading && <Skeleton className={styles.messageLoading} paragraph={{ rows: 5 }} />}
      <div className={messageListClass}>
        {messageList.map((msgItem: MessageItem, index: number) => {
          const { id: msgId, domainId, type, msg, msgValue, identityMsg, msgData } = msgItem;

          const followQuestions = getFollowQuestions(index);

          return (
            <div key={msgId} id={`${msgId}`} className={styles.messageItem}>
              {type === MessageTypeEnum.TEXT && <Text position="left" data={msg} />}
              {type === MessageTypeEnum.QUESTION && (
                <>
                  <Text position="right" data={msg} />
                  {identityMsg && <Text position="left" data={identityMsg} />}
                  <ChatItem
                    msg={msgValue || msg || ''}
                    followQuestions={followQuestions}
                    msgData={msgData}
                    conversationId={chatId}
                    domainId={domainId}
                    isLastMessage={index === messageList.length - 1}
                    isMobileMode={isMobileMode}
                    triggerResize={triggerResize}
                    onMsgDataLoaded={(data: MsgDataType) => {
                      onMsgDataLoaded(data, msgId);
                    }}
                    onSelectSuggestion={onSelectSuggestion}
                    onUpdateMessageScroll={onUpdateMessageScroll}
                  />
                </>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

function areEqual(prevProps: Props, nextProps: Props) {
  if (
    prevProps.id === nextProps.id &&
    isEqual(prevProps.messageList, nextProps.messageList) &&
    prevProps.miniProgramLoading === nextProps.miniProgramLoading
  ) {
    return true;
  }
  return false;
}

export default memo(MessageContainer, areEqual);

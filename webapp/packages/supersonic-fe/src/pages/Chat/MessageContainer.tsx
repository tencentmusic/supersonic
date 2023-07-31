import Text from './components/Text';
import { memo, useCallback, useEffect, useState } from 'react';
import { isEqual } from 'lodash';
import { ChatItem } from 'supersonic-chat-sdk';
import type { MsgDataType } from 'supersonic-chat-sdk';
import { MessageItem, MessageTypeEnum } from './type';
import styles from './style.less';
import RecommendQuestions from './components/RecommendQuestions';

type Props = {
  id: string;
  chatId: number;
  messageList: MessageItem[];
  isMobileMode?: boolean;
  conversationCollapsed: boolean;
  onClickMessageContainer: () => void;
  onMsgDataLoaded: (data: MsgDataType, questionId: string | number) => void;
  onSelectSuggestion: (value: string) => void;
  onCheckMore: (data: MsgDataType) => void;
};

const MessageContainer: React.FC<Props> = ({
  id,
  chatId,
  messageList,
  isMobileMode,
  conversationCollapsed,
  onClickMessageContainer,
  onMsgDataLoaded,
  onSelectSuggestion,
}) => {
  const [triggerResize, setTriggerResize] = useState(false);

  const onResize = useCallback(() => {
    setTriggerResize(true);
    setTimeout(() => {
      setTriggerResize(false);
    }, 0);
  }, []);

  useEffect(() => {
    onResize();
  }, [conversationCollapsed]);

  useEffect(() => {
    window.addEventListener('resize', onResize);
    return () => {
      window.removeEventListener('resize', onResize);
    };
  }, []);

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
      <div className={styles.messageList}>
        {messageList.map((msgItem: MessageItem, index: number) => {
          const { id: msgId, domainId, type, msg, msgValue, identityMsg, msgData } = msgItem;

          const followQuestions = getFollowQuestions(index);

          return (
            <div key={msgId} id={`${msgId}`} className={styles.messageItem}>
              {type === MessageTypeEnum.RECOMMEND_QUESTIONS && (
                <RecommendQuestions onSelectQuestion={onSelectSuggestion} />
              )}
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
    prevProps.conversationCollapsed === nextProps.conversationCollapsed
  ) {
    return true;
  }
  return false;
}

export default memo(MessageContainer, areEqual);

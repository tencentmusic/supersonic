import Text from '../components/Text';
import { memo, useCallback, useEffect, useState } from 'react';
import { isEqual } from 'lodash';
import { ChatItem } from 'supersonic-chat-sdk';
import type { MsgDataType } from 'supersonic-chat-sdk';
import { AgentType, MessageItem, MessageTypeEnum } from '../type';
import { isMobile, updateMessageContainerScroll } from '@/utils/utils';
import styles from './style.less';
import AgentTip from '../components/AgentTip';
import classNames from 'classnames';

type Props = {
  id: string;
  chatId: number;
  messageList: MessageItem[];
  historyVisible: boolean;
  currentAgent?: AgentType;
  chatVisible?: boolean;
  onMsgDataLoaded: (
    data: MsgDataType,
    questionId: string | number,
    question: string,
    valid: boolean,
  ) => void;
  onApplyAuth: (model: string) => void;
  onSendMsg: (value: string) => void;
};

const MessageContainer: React.FC<Props> = ({
  id,
  chatId,
  messageList,
  historyVisible,
  currentAgent,
  chatVisible,
  onMsgDataLoaded,
  onSendMsg,
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

  useEffect(() => {
    onResize();
  }, [historyVisible, chatVisible]);

  const getFilters = (modelId?: number, entityId?: string) => {
    if (!modelId || !entityId) {
      return undefined;
    }
    return [
      {
        value: entityId,
      },
    ];
  };

  const messageContainerClass = classNames(styles.messageContainer, { [styles.mobile]: isMobile });

  return (
    <div id={id} className={messageContainerClass}>
      <div className={styles.messageList}>
        {messageList.map((msgItem: MessageItem, index: number) => {
          const {
            id: msgId,
            modelId,
            agentId,
            entityId,
            type,
            msg,
            msgValue,
            identityMsg,
            msgData,
            score,
            isHistory,
            parseOptions,
          } = msgItem;

          return (
            <div key={msgId} id={`${msgId}`} className={styles.messageItem}>
              {type === MessageTypeEnum.TEXT && <Text position="left" data={msg} />}
              {type === MessageTypeEnum.AGENT_LIST && (
                <AgentTip currentAgent={currentAgent} onSendMsg={onSendMsg} />
              )}
              {type === MessageTypeEnum.QUESTION && (
                <>
                  <Text position="right" data={msg} />
                  {identityMsg && <Text position="left" data={identityMsg} />}
                  <ChatItem
                    msg={msgValue || msg || ''}
                    msgData={msgData}
                    conversationId={chatId}
                    modelId={modelId}
                    agentId={agentId}
                    filter={getFilters(modelId, entityId)}
                    isLastMessage={index === messageList.length - 1}
                    isHistory={isHistory}
                    triggerResize={triggerResize}
                    onMsgDataLoaded={(data: MsgDataType, valid: boolean) => {
                      onMsgDataLoaded(data, msgId, msgValue || msg || '', valid);
                    }}
                    onUpdateMessageScroll={updateMessageContainerScroll}
                  />
                </>
              )}
              {type === MessageTypeEnum.PARSE_OPTIONS && (
                <ChatItem
                  msg={msgValue || msg || ''}
                  conversationId={chatId}
                  modelId={modelId}
                  agentId={agentId}
                  filter={getFilters(modelId, entityId)}
                  isLastMessage={index === messageList.length - 1}
                  isHistory={isHistory}
                  triggerResize={triggerResize}
                  parseOptions={parseOptions}
                  onMsgDataLoaded={(data: MsgDataType, valid: boolean) => {
                    onMsgDataLoaded(data, msgId, msgValue || msg || '', valid);
                  }}
                  onUpdateMessageScroll={updateMessageContainerScroll}
                />
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
    prevProps.historyVisible === nextProps.historyVisible &&
    prevProps.currentAgent === nextProps.currentAgent &&
    prevProps.chatVisible === nextProps.chatVisible
  ) {
    return true;
  }
  return false;
}

export default memo(MessageContainer, areEqual);

import Text from './components/Text';
import { memo, useCallback, useEffect, useState } from 'react';
import { isEqual } from 'lodash';
import { ChatItem } from 'supersonic-chat-sdk';
import type { MsgDataType } from 'supersonic-chat-sdk';
import { AgentType, MessageItem, MessageTypeEnum } from './type';
import Plugin from './components/Plugin';
import { updateMessageContainerScroll } from '@/utils/utils';
import styles from './style.less';
import { MODEL_MODEL_ENTITY_ID_FILTER_MAP } from './constants';
import AgentList from './components/AgentList';
import RecommendQuestions from './components/RecommendQuestions';

type Props = {
  id: string;
  chatId: number;
  messageList: MessageItem[];
  isMobileMode?: boolean;
  conversationCollapsed: boolean;
  copilotFullscreen?: boolean;
  agentList: AgentType[];
  onClickMessageContainer: () => void;
  onMsgDataLoaded: (
    data: MsgDataType,
    questionId: string | number,
    question: string,
    valid: boolean,
  ) => void;
  onCheckMore: (data: MsgDataType) => void;
  onApplyAuth: (model: string) => void;
  onSendMsg: (value: string) => void;
  onSelectAgent: (agent: AgentType) => void;
};

const MessageContainer: React.FC<Props> = ({
  id,
  chatId,
  messageList,
  isMobileMode,
  conversationCollapsed,
  copilotFullscreen,
  agentList,
  onClickMessageContainer,
  onMsgDataLoaded,
  onCheckMore,
  onSendMsg,
  onSelectAgent,
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
  }, [conversationCollapsed]);

  useEffect(() => {
    onResize();
    updateMessageContainerScroll();
  }, [copilotFullscreen]);

  const getFollowQuestions = (index: number) => {
    const followQuestions: string[] = [];
    const currentMsg = messageList[index];
    const currentMsgData = currentMsg.msgData;
    const msgs = messageList.slice(0, index).reverse();

    for (let i = 0; i < msgs.length; i++) {
      const msg = msgs[i];
      const msgModelId = msg.msgData?.chatContext?.modelId;
      const msgEntityId = msg.msgData?.entityInfo?.entityId;
      const currentMsgModelId = currentMsgData?.chatContext?.modelId;
      const currentMsgEntityId = currentMsgData?.entityInfo?.entityId;

      if (
        (msg.type === MessageTypeEnum.QUESTION || msg.type === MessageTypeEnum.PLUGIN) &&
        !!currentMsgModelId &&
        msgModelId === currentMsgModelId &&
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

  const getFilters = (modelId?: number, entityId?: string) => {
    if (!modelId || !entityId) {
      return undefined;
    }
    return [
      {
        ...MODEL_MODEL_ENTITY_ID_FILTER_MAP[modelId],
        value: entityId,
      },
    ];
  };

  return (
    <div id={id} className={styles.messageContainer} onClick={onClickMessageContainer}>
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

          const followQuestions = getFollowQuestions(index);

          return (
            <div key={msgId} id={`${msgId}`} className={styles.messageItem}>
              {type === MessageTypeEnum.TEXT && <Text position="left" data={msg} />}
              {type === MessageTypeEnum.RECOMMEND_QUESTIONS && (
                <RecommendQuestions onSelectQuestion={onSendMsg} />
              )}
              {type === MessageTypeEnum.AGENT_LIST && (
                <AgentList
                  currentAgentName={msg!}
                  data={agentList}
                  copilotFullscreen={copilotFullscreen}
                  onSelectAgent={onSelectAgent}
                />
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
                    isMobileMode={isMobileMode}
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
                  isMobileMode={isMobileMode}
                  triggerResize={triggerResize}
                  parseOptions={parseOptions}
                  onMsgDataLoaded={(data: MsgDataType, valid: boolean) => {
                    onMsgDataLoaded(data, msgId, msgValue || msg || '', valid);
                  }}
                  onUpdateMessageScroll={updateMessageContainerScroll}
                />
              )}
              {type === MessageTypeEnum.PLUGIN && (
                <>
                  <Plugin
                    id={msgId}
                    followQuestions={followQuestions}
                    data={msgData!}
                    scoreValue={score}
                    msg={msgValue || msg || ''}
                    isHistory={isHistory}
                    isLastMessage={index === messageList.length - 1}
                    isMobileMode={isMobileMode}
                    onReportLoaded={(height: number) => {
                      updateMessageContainerScroll(true, height);
                    }}
                    onCheckMore={onCheckMore}
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
    prevProps.conversationCollapsed === nextProps.conversationCollapsed &&
    prevProps.copilotFullscreen === nextProps.copilotFullscreen &&
    prevProps.agentList === nextProps.agentList
  ) {
    return true;
  }
  return false;
}

export default memo(MessageContainer, areEqual);

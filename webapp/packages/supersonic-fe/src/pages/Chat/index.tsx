import { updateMessageContainerScroll, isMobile, uuid } from '@/utils/utils';
import { useEffect, useRef, useState } from 'react';
import { Helmet, useDispatch } from 'umi';
import MessageContainer from './MessageContainer';
import styles from './style.less';
import {
  ConversationDetailType,
  DefaultEntityType,
  ModelType,
  MessageItem,
  MessageTypeEnum,
  AgentType,
} from './type';
import { queryAgentList } from './service';
import { useThrottleFn } from 'ahooks';
import Conversation from './Conversation';
import ChatFooter from './ChatFooter';
import classNames from 'classnames';
import { CHAT_TITLE, DEFAULT_CONVERSATION_NAME, WEB_TITLE } from './constants';
import { HistoryMsgItemType, MsgDataType, getHistoryMsg } from 'supersonic-chat-sdk';
import { cloneDeep } from 'lodash';
import 'supersonic-chat-sdk/dist/index.css';
import { setToken as setChatSdkToken } from 'supersonic-chat-sdk';
import AgentList from './AgentList';
import { AUTH_TOKEN_KEY } from '@/common/constants';

type Props = {
  isCopilotMode?: boolean;
  defaultModelName?: string;
  defaultEntityFilter?: DefaultEntityType;
  copilotSendMsg?: string;
  triggerNewConversation?: boolean;
  chatVisible?: boolean;
  onNewConversationTriggered?: () => void;
  onCurrentModelChange?: (model?: ModelType) => void;
};

const Chat: React.FC<Props> = ({
  isCopilotMode,
  defaultModelName,
  defaultEntityFilter,
  copilotSendMsg,
  triggerNewConversation,
  chatVisible,
  onNewConversationTriggered,
}) => {
  const isMobileMode = isMobile || isCopilotMode;

  const [messageList, setMessageList] = useState<MessageItem[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  const [pageNo, setPageNo] = useState(1);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [historyInited, setHistoryInited] = useState(false);
  const [currentConversation, setCurrentConversation] = useState<
    ConversationDetailType | undefined
  >(isMobile ? { chatId: 0, chatName: `${CHAT_TITLE}问答` } : undefined);
  const [historyVisible, setHistoryVisible] = useState(false);
  const [defaultEntity, setDefaultEntity] = useState<DefaultEntityType>();
  const [agentList, setAgentList] = useState<AgentType[]>([]);
  const [currentAgent, setCurrentAgent] = useState<AgentType>();
  const dispatch = useDispatch();

  const conversationRef = useRef<any>();
  const chatFooterRef = useRef<any>();

  const initAgentList = async () => {
    const res = await queryAgentList();
    const agentListValue = (res.data || []).filter((item) => item.status === 1);
    setAgentList(agentListValue);
    if (agentListValue.length > 0) {
      setCurrentAgent(agentListValue[0]);
    }
  };

  useEffect(() => {
    setChatSdkToken(localStorage.getItem(AUTH_TOKEN_KEY) || '');
    initAgentList();
  }, []);

  useEffect(() => {
    if (chatVisible) {
      updateMessageContainerScroll();
    }
  }, [chatVisible]);

  useEffect(() => {
    if (triggerNewConversation) {
      setCurrentAgent(agentList?.find((item) => item.name === '做分析'));
      conversationRef.current?.onAddConversation({
        type: 'CUSTOMIZE',
        modelId: defaultEntityFilter?.modelId,
        entityId: defaultEntityFilter?.entityId,
        agent: agentList?.find((item) => item.name === '做分析'),
      });
      setTimeout(() => {
        onNewConversationTriggered?.();
      }, 200);
    }
  }, [triggerNewConversation]);

  useEffect(() => {
    if (!currentConversation) {
      return;
    }
    const { initMsg, modelId, entityId, agent } = currentConversation;
    if (initMsg) {
      inputFocus();
      if (initMsg === 'CUSTOMIZE' && copilotSendMsg) {
        onSendMsg(
          copilotSendMsg,
          [],
          modelId,
          entityId,
          agentList.find((item) => item.name === '做分析'),
        );
        dispatch({
          type: 'globalState/setCopilotSendMsg',
          payload: '',
        });
        return;
      }
      if (initMsg === DEFAULT_CONVERSATION_NAME || initMsg.includes('CUSTOMIZE')) {
        if (agent) {
          setCurrentAgent(agent);
        }
        sendHelloRsp(agent);
        return;
      }
      onSendMsg(
        initMsg,
        [],
        modelId,
        entityId,
        initMsg.includes('商业线索') ? agentList.find((item) => item.name === '做分析') : undefined,
      );
      return;
    }
    updateHistoryMsg(1);
    setPageNo(1);
  }, [currentConversation]);

  useEffect(() => {
    setDefaultEntity(defaultEntityFilter);
  }, [defaultEntityFilter]);

  useEffect(() => {
    if (historyInited) {
      const messageContainerEle = document.getElementById('messageContainer');
      messageContainerEle?.addEventListener('scroll', handleScroll);
    }
    return () => {
      const messageContainerEle = document.getElementById('messageContainer');
      messageContainerEle?.removeEventListener('scroll', handleScroll);
    };
  }, [historyInited]);

  const sendHelloRsp = (agent?: AgentType) => {
    setMessageList([
      {
        id: uuid(),
        type: MessageTypeEnum.AGENT_LIST,
        msg: agent?.name || currentAgent?.name || '查信息',
      },
    ]);
  };

  const convertHistoryMsg = (list: HistoryMsgItemType[]) => {
    return list.map((item: HistoryMsgItemType) => ({
      id: item.questionId,
      type: MessageTypeEnum.QUESTION,
      msg: item.queryText,
      msgData: item.queryResult,
      score: item.score,
      isHistory: true,
    }));
  };

  const updateHistoryMsg = async (page: number) => {
    const res = await getHistoryMsg(page, currentConversation!.chatId, 3);
    const { hasNextPage, list } = res.data?.data || { hasNextPage: false, list: [] };
    const msgList = [...convertHistoryMsg(list), ...(page === 1 ? [] : messageList)];
    setMessageList(msgList);
    setHasNextPage(hasNextPage);
    if (page === 1) {
      if (list.length === 0) {
        sendHelloRsp();
      }
      updateMessageContainerScroll();
      setHistoryInited(true);
      inputFocus();
    } else {
      const msgEle = document.getElementById(`${messageList[0]?.id}`);
      msgEle?.scrollIntoView();
    }
  };

  const { run: handleScroll } = useThrottleFn(
    (e) => {
      if (e.target.scrollTop === 0 && hasNextPage) {
        updateHistoryMsg(pageNo + 1);
        setPageNo(pageNo + 1);
      }
    },
    {
      leading: true,
      trailing: true,
      wait: 200,
    },
  );

  const inputFocus = () => {
    if (!isMobile) {
      chatFooterRef.current?.inputFocus();
    }
  };

  const inputBlur = () => {
    chatFooterRef.current?.inputBlur();
  };

  const onSendMsg = async (
    msg?: string,
    list?: MessageItem[],
    modelId?: number,
    entityId?: string,
    agent?: AgentType,
  ) => {
    const currentMsg = msg || inputMsg;
    if (currentMsg.trim() === '') {
      setInputMsg('');
      return;
    }

    const msgAgent = agentList.find((item) => currentMsg.indexOf(item.name) === 1);
    const certainAgent = currentMsg[0] === '/' && msgAgent;
    const agentIdValue = certainAgent ? msgAgent.id : undefined;
    if (agent || certainAgent) {
      setCurrentAgent(agent || msgAgent);
    }

    const msgs = [
      ...(list || messageList),
      {
        id: uuid(),
        msg: currentMsg,
        msgValue: certainAgent
          ? currentMsg.replace(`/${certainAgent.name}`, '').trim()
          : currentMsg,
        modelId: modelId === -1 ? undefined : modelId,
        agentId: agent?.id || agentIdValue || currentAgent?.id,
        entityId: entityId || defaultEntity?.entityId,
        type: MessageTypeEnum.QUESTION,
      },
    ];
    setMessageList(msgs);
    updateMessageContainerScroll();
    setInputMsg('');
  };

  const onInputMsgChange = (value: string) => {
    const inputMsgValue = value || '';
    setInputMsg(inputMsgValue);
  };

  const saveConversationToLocal = (conversation: ConversationDetailType) => {
    if (conversation) {
      if (conversation.chatId !== -1) {
        localStorage.setItem('CONVERSATION_ID', `${conversation.chatId}`);
      }
    } else {
      localStorage.removeItem('CONVERSATION_ID');
    }
  };

  const onSelectConversation = (
    conversation: ConversationDetailType,
    name?: string,
    modelId?: number,
    entityId?: string,
    agent?: AgentType,
  ) => {
    if (!isMobileMode) {
      window.history.replaceState('', '', `?q=${conversation.chatName}&cid=${conversation.chatId}`);
    }
    setCurrentConversation({
      ...conversation,
      initMsg: name,
      modelId,
      entityId,
      agent,
    });
    saveConversationToLocal(conversation);
  };

  const reportMsgEvent = (msg: string, valid: boolean) => {};

  const onMsgDataLoaded = (
    data: MsgDataType,
    questionId: string | number,
    question: string,
    valid: boolean,
  ) => {
    reportMsgEvent(question, valid);
    if (!isMobile) {
      conversationRef?.current?.updateData();
    }
    if (!data) {
      return;
    }
    let parseOptionsItem: any;
    if (data.parseOptions && data.parseOptions.length > 0) {
      parseOptionsItem = {
        id: uuid(),
        msg: messageList[messageList.length - 1]?.msg,
        type: MessageTypeEnum.PARSE_OPTIONS,
        parseOptions: data.parseOptions,
      };
    }
    const msgs = cloneDeep(messageList);
    const msg = msgs.find((item) => item.id === questionId);
    if (msg) {
      msg.msgData = data;
      const msgList = [...msgs, ...(parseOptionsItem ? [parseOptionsItem] : [])];
      setMessageList(msgList);
    }
    updateMessageContainerScroll(`${questionId}`);
  };

  const onToggleHistoryVisible = () => {
    setHistoryVisible(!historyVisible);
  };

  const onApplyAuth = (model: string) => {};

  const onAddConversation = (agent?: AgentType) => {
    conversationRef.current?.onAddConversation({ agent });
    inputFocus();
  };

  const onSelectAgent = (agent: AgentType) => {
    if (agent.id === currentAgent?.id) {
      return;
    }
    setCurrentAgent(agent);
    updateMessageContainerScroll();
  };

  const sendMsg = (msg: string, modelId?: number) => {
    onSendMsg(msg, messageList, modelId);
    if (isMobile) {
      inputBlur();
    }
  };

  const onCloseConversation = () => {
    setHistoryVisible(false);
  };

  const chatClass = classNames(styles.chat, {
    [styles.mobile]: isMobile,
    [styles.historyVisible]: historyVisible,
  });

  return (
    <div className={chatClass}>
      {!isMobileMode && <Helmet title={WEB_TITLE} />}
      <div className={styles.chatSection}>
        {!isMobile && (
          <AgentList
            agentList={agentList}
            currentAgent={currentAgent}
            onSelectAgent={onSelectAgent}
          />
        )}
        <div className={styles.chatApp}>
          {currentConversation && (
            <div className={styles.chatBody}>
              <div className={styles.chatContent}>
                {currentAgent && !isMobile && (
                  <div className={styles.chatHeader}>
                    <div className={styles.chatHeaderTitle}>{currentAgent.name}</div>
                    <div className={styles.chatHeaderTip}>{currentAgent.description}</div>
                  </div>
                )}
                <MessageContainer
                  id="messageContainer"
                  messageList={messageList}
                  chatId={currentConversation?.chatId}
                  historyVisible={historyVisible}
                  currentAgent={currentAgent}
                  chatVisible={chatVisible}
                  onMsgDataLoaded={onMsgDataLoaded}
                  onApplyAuth={onApplyAuth}
                  onSendMsg={onSendMsg}
                />
                <ChatFooter
                  inputMsg={inputMsg}
                  chatId={currentConversation?.chatId}
                  agentList={agentList}
                  currentAgent={currentAgent}
                  onToggleHistoryVisible={onToggleHistoryVisible}
                  onInputMsgChange={onInputMsgChange}
                  onSendMsg={sendMsg}
                  onAddConversation={onAddConversation}
                  onSelectAgent={onSelectAgent}
                  ref={chatFooterRef}
                />
              </div>
            </div>
          )}
        </div>
        <Conversation
          agentList={agentList}
          currentAgent={currentAgent}
          currentConversation={currentConversation}
          historyVisible={historyVisible}
          isCopilotMode={isCopilotMode}
          defaultEntityFilter={defaultEntityFilter}
          triggerNewConversation={triggerNewConversation}
          onNewConversationTriggered={onNewConversationTriggered}
          onSelectConversation={onSelectConversation}
          onCloseConversation={onCloseConversation}
          ref={conversationRef}
        />
      </div>
    </div>
  );
};

export default Chat;

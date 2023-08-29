import { updateMessageContainerScroll, isMobile, uuid, getLeafList } from '@/utils/utils';
import { useEffect, useRef, useState } from 'react';
import { Helmet, useDispatch, useLocation } from 'umi';
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
import { getModelList, queryAgentList } from './service';
import { useThrottleFn } from 'ahooks';
import Conversation from './Conversation';
import ChatFooter from './ChatFooter';
import classNames from 'classnames';
import { CHAT_TITLE, DEFAULT_CONVERSATION_NAME, WEB_TITLE } from './constants';
import { HistoryMsgItemType, MsgDataType, getHistoryMsg } from 'supersonic-chat-sdk';
import { cloneDeep } from 'lodash';
import 'supersonic-chat-sdk/dist/index.css';
import { setToken as setChatSdkToken } from 'supersonic-chat-sdk';
import { AUTH_TOKEN_KEY } from '@/common/constants';

type Props = {
  isCopilotMode?: boolean;
  copilotFullscreen?: boolean;
  defaultModelName?: string;
  defaultEntityFilter?: DefaultEntityType;
  copilotSendMsg?: string;
  triggerNewConversation?: boolean;
  onNewConversationTriggered?: () => void;
  onCurrentModelChange?: (model?: ModelType) => void;
  onCancelCopilotFilter?: () => void;
  onCheckMoreDetail?: () => void;
};

const Chat: React.FC<Props> = ({
  isCopilotMode,
  copilotFullscreen,
  defaultModelName,
  defaultEntityFilter,
  copilotSendMsg,
  triggerNewConversation,
  onNewConversationTriggered,
  onCurrentModelChange,
  onCancelCopilotFilter,
  onCheckMoreDetail,
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
  const [conversationCollapsed, setConversationCollapsed] = useState(isMobileMode);
  const [models, setModels] = useState<ModelType[]>([]);
  const [currentModel, setCurrentModel] = useState<ModelType>();
  const [defaultEntity, setDefaultEntity] = useState<DefaultEntityType>();
  const [applyAuthVisible, setApplyAuthVisible] = useState(false);
  const [applyAuthModel, setApplyAuthModel] = useState('');
  const [initialModelName, setInitialModelName] = useState('');
  const [agentList, setAgentList] = useState<AgentType[]>([]);
  const [currentAgent, setCurrentAgent] = useState<AgentType>();
  const location = useLocation();
  const dispatch = useDispatch();
  const { modelName } = (location as any).query;

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
    initModels();
    initAgentList();
  }, []);

  useEffect(() => {
    if (models.length > 0 && initialModelName && !currentModel) {
      changeModel(models.find((model) => model.name === initialModelName));
    }
  }, [models]);

  useEffect(() => {
    if (modelName) {
      setInitialModelName(modelName);
    }
  }, [modelName]);

  useEffect(() => {
    if (defaultModelName !== undefined && models.length > 0) {
      changeModel(models.find((model) => model.name === defaultModelName));
    }
  }, [defaultModelName]);

  useEffect(() => {
    if (!currentConversation) {
      return;
    }
    const { initMsg, modelId, entityId } = currentConversation;
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
        sendHelloRsp();
        return;
      }
      onSendMsg(initMsg, [], modelId, entityId);
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

  useEffect(() => {
    inputFocus();
  }, [copilotFullscreen]);

  const sendHelloRsp = () => {
    setMessageList([
      {
        // id: uuid(),
        // type: MessageTypeEnum.RECOMMEND_QUESTIONS,
        type: MessageTypeEnum.AGENT_LIST,
        msg: currentAgent?.name || '查信息',
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

  const changeModel = (model?: ModelType) => {
    setCurrentModel(model);
    if (onCurrentModelChange) {
      onCurrentModelChange(model);
    }
  };

  const changeAgent = (agent?: AgentType) => {
    setCurrentAgent(agent);
  };

  const initModels = async () => {
    const res = await getModelList();
    const modelList = getLeafList(res.data);
    setModels([{ id: -1, name: '全部', bizName: 'all', parentId: 0 }, ...modelList].slice(0, 11));
    if (defaultModelName !== undefined) {
      changeModel(modelList.find((model) => model.name === defaultModelName));
    }
  };

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
    const msgModel = models.find((item) => currentMsg.includes(item.name));
    const certainModel = currentMsg[0] === '@' && msgModel;
    let modelChanged = false;

    if (certainModel) {
      const toModel = msgModel.id === -1 ? undefined : msgModel;
      changeModel(toModel);
      modelChanged = currentModel?.id !== toModel?.id;
    }
    const modelIdValue = modelId || msgModel?.id || currentModel?.id;

    const msgAgent = agentList.find((item) => currentMsg.indexOf(item.name) === 1);
    const certainAgent = currentMsg[0] === '/' && msgAgent;
    const agentIdValue = certainAgent ? msgAgent.id : undefined;
    if (agent || certainAgent) {
      changeAgent(agent || msgAgent);
    }

    const msgs = [
      ...(list || messageList),
      {
        id: uuid(),
        msg: currentMsg,
        msgValue: certainModel
          ? currentMsg.replace(`@${msgModel.name}`, '').trim()
          : certainAgent
          ? currentMsg.replace(`/${certainAgent.name}`, '').trim()
          : currentMsg,
        modelId: modelIdValue === -1 ? undefined : modelIdValue,
        agentId: agent?.id || agentIdValue || currentAgent?.id,
        entityId: entityId || (modelChanged ? undefined : defaultEntity?.entityId),
        identityMsg: certainModel ? getIdentityMsgText(msgModel) : undefined,
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
  ) => {
    if (!isMobileMode) {
      window.history.replaceState('', '', `?q=${conversation.chatName}&cid=${conversation.chatId}`);
    }
    setCurrentConversation({
      ...conversation,
      initMsg: name,
      modelId,
      entityId,
    });
    saveConversationToLocal(conversation);
  };

  const updateChatFilter = (data: MsgDataType) => {
    const { queryMode, dimensionFilters, elementMatches, modelName, model } = data.chatContext;
    if (queryMode !== 'ENTITY_LIST_FILTER') {
      return;
    }
    const entityId = dimensionFilters?.length > 0 ? dimensionFilters[0].value : undefined;
    const entityName = elementMatches?.find((item: any) => item.element?.type === 'ID')?.element
      ?.name;

    if (typeof entityId === 'string' && entityName) {
      setCurrentModel(model);
      setDefaultEntity({
        entityId,
        entityName,
        modelName,
      });
    }
  };

  const onMsgDataLoaded = (
    data: MsgDataType,
    questionId: string | number,
    question: string,
    valid: boolean,
  ) => {
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
      updateChatFilter(data, msgList);
    }
    updateMessageContainerScroll(`${questionId}`);
  };

  const onCheckMore = (data: MsgDataType) => {
    setMessageList([
      ...messageList,
      {
        id: uuid(),
        msg: data.response.name,
        type: MessageTypeEnum.PLUGIN,
        msgData: data,
      },
    ]);
    updateMessageContainerScroll();
    if (onCheckMoreDetail) {
      onCheckMoreDetail();
    }
  };

  const onToggleCollapseBtn = () => {
    setConversationCollapsed(!conversationCollapsed);
    localStorage.setItem('CONVERSATION_COLLAPSED', `${!conversationCollapsed}`);
  };

  const getIdentityMsgText = (model?: ModelType) => {
    return model
      ? `您好，我当前身份是【${model.name}】主题专家，我将尽力帮您解答相关问题～`
      : '您好，我将尽力帮您解答所有主题相关问题～';
  };

  const onApplyAuth = (model: string) => {
    setApplyAuthModel(model);
    setApplyAuthVisible(true);
  };

  const onAddConversation = () => {
    conversationRef.current?.onAddConversation();
    inputFocus();
  };

  const onSelectAgent = (agent: AgentType) => {
    setCurrentAgent(agent);
    setMessageList([
      ...messageList,
      {
        id: uuid(),
        type: MessageTypeEnum.TEXT,
        msg: `您好，智能助理【${agent.name}】将与您对话，可输入“/”切换助理`,
      },
    ]);
    updateMessageContainerScroll();
  };

  const chatClass = classNames(styles.chat, {
    [styles.mobileMode]: isMobileMode,
    [styles.mobile]: isMobile,
    [styles.copilotFullscreen]: copilotFullscreen,
    [styles.conversationCollapsed]: conversationCollapsed,
  });

  return (
    <div className={chatClass}>
      {!isMobileMode && <Helmet title={WEB_TITLE} />}
      <div className={styles.topSection} />
      <div className={styles.chatSection}>
        <Conversation
          currentConversation={currentConversation}
          collapsed={conversationCollapsed}
          isCopilotMode={isCopilotMode}
          defaultModelName={defaultModelName}
          defaultEntityFilter={defaultEntityFilter}
          triggerNewConversation={triggerNewConversation}
          onNewConversationTriggered={onNewConversationTriggered}
          onSelectConversation={onSelectConversation}
          ref={conversationRef}
        />
        <div className={styles.chatApp}>
          {currentConversation && (
            <div className={styles.chatBody}>
              <div className={styles.chatContent}>
                <MessageContainer
                  id="messageContainer"
                  messageList={messageList}
                  chatId={currentConversation?.chatId}
                  isMobileMode={isMobileMode}
                  conversationCollapsed={conversationCollapsed}
                  copilotFullscreen={copilotFullscreen}
                  agentList={agentList}
                  onClickMessageContainer={inputFocus}
                  onMsgDataLoaded={onMsgDataLoaded}
                  onCheckMore={onCheckMore}
                  onApplyAuth={onApplyAuth}
                  onSendMsg={onSendMsg}
                  onSelectAgent={onSelectAgent}
                />
                <ChatFooter
                  inputMsg={inputMsg}
                  chatId={currentConversation?.chatId}
                  models={models}
                  agentList={agentList}
                  currentModel={currentModel}
                  currentAgent={currentAgent}
                  defaultEntity={defaultEntity}
                  collapsed={conversationCollapsed}
                  isCopilotMode={isCopilotMode}
                  copilotFullscreen={copilotFullscreen}
                  onToggleCollapseBtn={onToggleCollapseBtn}
                  onInputMsgChange={onInputMsgChange}
                  onSendMsg={(msg: string, modelId?: number) => {
                    onSendMsg(msg, messageList, modelId);
                    if (isMobile) {
                      inputBlur();
                    }
                  }}
                  onAddConversation={onAddConversation}
                  onCancelDefaultFilter={() => {
                    changeModel(undefined);
                    setDefaultEntity(undefined);
                    if (onCancelCopilotFilter) {
                      onCancelCopilotFilter();
                    }
                  }}
                  onSelectAgent={onSelectAgent}
                  ref={chatFooterRef}
                />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Chat;

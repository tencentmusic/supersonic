import { updateMessageContainerScroll, isMobile, uuid, getLeafList } from '@/utils/utils';
import { useEffect, useRef, useState } from 'react';
import { Helmet, useDispatch, useLocation } from 'umi';
import MessageContainer from './MessageContainer';
import styles from './style.less';
import {
  ConversationDetailType,
  DefaultEntityType,
  DomainType,
  MessageItem,
  MessageTypeEnum,
} from './type';
import { getDomainList } from './service';
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
  defaultDomainName?: string;
  defaultEntityFilter?: DefaultEntityType;
  copilotSendMsg?: string;
  triggerNewConversation?: boolean;
  onNewConversationTriggered?: () => void;
  onCurrentDomainChange?: (domain?: DomainType) => void;
  onCancelCopilotFilter?: () => void;
  onCheckMoreDetail?: () => void;
};

const Chat: React.FC<Props> = ({
  isCopilotMode,
  copilotFullscreen,
  defaultDomainName,
  defaultEntityFilter,
  copilotSendMsg,
  triggerNewConversation,
  onNewConversationTriggered,
  onCurrentDomainChange,
  onCancelCopilotFilter,
  onCheckMoreDetail,
}) => {
  const isMobileMode = isMobile || isCopilotMode;
  const localConversationCollapsed = localStorage.getItem('CONVERSATION_COLLAPSED');

  const [messageList, setMessageList] = useState<MessageItem[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  const [pageNo, setPageNo] = useState(1);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [historyInited, setHistoryInited] = useState(false);
  const [currentConversation, setCurrentConversation] = useState<
    ConversationDetailType | undefined
  >(isMobile ? { chatId: 0, chatName: `${CHAT_TITLE}问答` } : undefined);
  const [conversationCollapsed, setConversationCollapsed] = useState(
    !localConversationCollapsed ? true : localConversationCollapsed === 'true',
  );
  const [domains, setDomains] = useState<DomainType[]>([]);
  const [currentDomain, setCurrentDomain] = useState<DomainType>();
  const [defaultEntity, setDefaultEntity] = useState<DefaultEntityType>();
  const [applyAuthVisible, setApplyAuthVisible] = useState(false);
  const [applyAuthDomain, setApplyAuthDomain] = useState('');
  const [initialDomainName, setInitialDomainName] = useState('');
  const location = useLocation();
  const dispatch = useDispatch();
  const { domainName } = (location as any).query;

  const conversationRef = useRef<any>();
  const chatFooterRef = useRef<any>();

  useEffect(() => {
    setChatSdkToken(localStorage.getItem(AUTH_TOKEN_KEY) || '');
    initDomains();
  }, []);

  useEffect(() => {
    if (domains.length > 0 && initialDomainName && !currentDomain) {
      changeDomain(domains.find((domain) => domain.name === initialDomainName));
    }
  }, [domains]);

  useEffect(() => {
    if (domainName) {
      setInitialDomainName(domainName);
    }
  }, [domainName]);

  useEffect(() => {
    if (defaultDomainName !== undefined && domains.length > 0) {
      changeDomain(domains.find((domain) => domain.name === defaultDomainName));
    }
  }, [defaultDomainName]);

  useEffect(() => {
    if (!currentConversation) {
      return;
    }
    const { initMsg, domainId, entityId } = currentConversation;
    if (initMsg) {
      inputFocus();
      if (initMsg === 'CUSTOMIZE' && copilotSendMsg) {
        onSendMsg(copilotSendMsg, [], domainId, entityId);
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
      onSendMsg(initMsg, [], domainId, entityId);
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
        id: uuid(),
        type: MessageTypeEnum.TEXT,
        msg: defaultDomainName
          ? `您好，请输入关于${
              defaultEntityFilter?.entityName
                ? `${defaultDomainName?.slice(0, defaultDomainName?.length - 1)}【${
                    defaultEntityFilter?.entityName
                  }】`
                : `【${defaultDomainName}】`
            }的问题`
          : '您好，请问有什么我能帮您吗？',
      },
    ]);
  };

  const convertHistoryMsg = (list: HistoryMsgItemType[]) => {
    return list.map((item: HistoryMsgItemType) => ({
      id: item.questionId,
      type:
        item.queryResult?.queryMode === MessageTypeEnum.PLUGIN ||
        item.queryResult?.queryMode === MessageTypeEnum.WEB_PAGE
          ? MessageTypeEnum.PLUGIN
          : MessageTypeEnum.QUESTION,
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

  const changeDomain = (domain?: DomainType) => {
    setCurrentDomain(domain);
    if (onCurrentDomainChange) {
      onCurrentDomainChange(domain);
    }
  };

  const initDomains = async () => {
    const res = await getDomainList();
    const domainList = getLeafList(res.data);
    setDomains([{ id: -1, name: '全部', bizName: 'all', parentId: 0 }, ...domainList].slice(0, 11));
    if (defaultDomainName !== undefined) {
      changeDomain(domainList.find((domain) => domain.name === defaultDomainName));
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
    domainId?: number,
    entityId?: string,
  ) => {
    const currentMsg = msg || inputMsg;
    if (currentMsg.trim() === '') {
      setInputMsg('');
      return;
    }
    const msgDomain = domains.find((item) => currentMsg.includes(item.name));
    const certainDomain = currentMsg[0] === '@' && msgDomain;
    let domainChanged = false;

    if (certainDomain) {
      const toDomain = msgDomain.id === -1 ? undefined : msgDomain;
      changeDomain(toDomain);
      domainChanged = currentDomain?.id !== toDomain?.id;
    }
    const domainIdValue = domainId || msgDomain?.id || currentDomain?.id;
    const msgs = [
      ...(list || messageList),
      {
        id: uuid(),
        msg: currentMsg,
        msgValue: certainDomain ? currentMsg.replace(`@${msgDomain.name}`, '').trim() : currentMsg,
        domainId: domainIdValue === -1 ? undefined : domainIdValue,
        entityId: entityId || (domainChanged ? undefined : defaultEntity?.entityId),
        identityMsg: certainDomain ? getIdentityMsgText(msgDomain) : undefined,
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
    domainId?: number,
    entityId?: string,
  ) => {
    if (!isMobileMode) {
      window.history.replaceState('', '', `?q=${conversation.chatName}&cid=${conversation.chatId}`);
    }
    setCurrentConversation({
      ...conversation,
      initMsg: name,
      domainId,
      entityId,
    });
    saveConversationToLocal(conversation);
  };

  const onMsgDataLoaded = (data: MsgDataType, questionId: string | number) => {
    if (!isMobile) {
      conversationRef?.current?.updateData();
    }
    if (!data) {
      return;
    }
    if (data.queryMode === 'WEB_PAGE') {
      setMessageList([
        ...messageList,
        {
          id: uuid(),
          msg: messageList[messageList.length - 1]?.msg,
          type: MessageTypeEnum.PLUGIN,
          msgData: data,
        },
      ]);
    } else {
      const msgs = cloneDeep(messageList);
      const msg = msgs.find((item) => item.id === questionId);
      if (msg) {
        msg.msgData = data;
        setMessageList(msgs);
      }
      updateMessageContainerScroll();
    }
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

  const getIdentityMsgText = (domain?: DomainType) => {
    return domain
      ? `您好，我当前身份是【${domain.name}】主题专家，我将尽力帮您解答相关问题～`
      : '您好，我将尽力帮您解答所有主题相关问题～';
  };

  const onApplyAuth = (domain: string) => {
    setApplyAuthDomain(domain);
    setApplyAuthVisible(true);
  };

  const onAddConversation = () => {
    conversationRef.current?.onAddConversation();
    inputFocus();
  };

  const chatClass = classNames(styles.chat, {
    [styles.mobile]: isMobileMode,
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
          defaultDomainName={defaultDomainName}
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
                  onClickMessageContainer={inputFocus}
                  onMsgDataLoaded={onMsgDataLoaded}
                  onCheckMore={onCheckMore}
                  onApplyAuth={onApplyAuth}
                />
                <ChatFooter
                  inputMsg={inputMsg}
                  chatId={currentConversation?.chatId}
                  domains={domains}
                  currentDomain={currentDomain}
                  defaultEntity={defaultEntity}
                  collapsed={conversationCollapsed}
                  isCopilotMode={isCopilotMode}
                  copilotFullscreen={copilotFullscreen}
                  onToggleCollapseBtn={onToggleCollapseBtn}
                  onInputMsgChange={onInputMsgChange}
                  onSendMsg={(msg: string, domainId?: number) => {
                    onSendMsg(msg, messageList, domainId);
                    if (isMobile) {
                      inputBlur();
                    }
                  }}
                  onAddConversation={onAddConversation}
                  onCancelDefaultFilter={() => {
                    changeDomain(undefined);
                    if (onCancelCopilotFilter) {
                      onCancelCopilotFilter();
                    }
                  }}
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

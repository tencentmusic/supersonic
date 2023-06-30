import { updateMessageContainerScroll, isMobile, uuid, getLeafList } from '@/utils/utils';
import { useEffect, useRef, useState } from 'react';
import { Helmet } from 'umi';
import MessageContainer from './MessageContainer';
import styles from './style.less';
import { ConversationDetailType, DomainType, MessageItem, MessageTypeEnum } from './type';
import { getDomainList, updateConversationName } from './service';
import { useThrottleFn } from 'ahooks';
import RightSection from './RightSection';
import ChatFooter from './ChatFooter';
import classNames from 'classnames';
import { CHAT_TITLE, DEFAULT_CONVERSATION_NAME, WEB_TITLE } from './constants';
import { cloneDeep } from 'lodash';
import { HistoryMsgItemType, MsgDataType, getHistoryMsg } from 'supersonic-chat-sdk';
import 'supersonic-chat-sdk/dist/index.css';
import { setToken as setChatSdkToken } from 'supersonic-chat-sdk';
import { TOKEN_KEY } from '@/services/request';

type Props = {
  isCopilotMode?: boolean;
};

const Chat: React.FC<Props> = ({ isCopilotMode }) => {
  const isMobileMode = (isMobile || isCopilotMode) as boolean;

  const [messageList, setMessageList] = useState<MessageItem[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  const [pageNo, setPageNo] = useState(1);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [historyInited, setHistoryInited] = useState(false);
  const [currentConversation, setCurrentConversation] = useState<
    ConversationDetailType | undefined
  >(isMobile ? { chatId: 0, chatName: `${CHAT_TITLE}问答` } : undefined);
  const [currentEntity, setCurrentEntity] = useState<MsgDataType>();
  const [miniProgramLoading, setMiniProgramLoading] = useState(false);
  const [domains, setDomains] = useState<DomainType[]>([]);
  const [currentDomain, setCurrentDomain] = useState<DomainType>();
  const conversationRef = useRef<any>();
  const chatFooterRef = useRef<any>();

  const sendHelloRsp = () => {
    setMessageList([
      {
        id: uuid(),
        type: MessageTypeEnum.TEXT,
        msg: '您好，请问有什么我能帮您吗？',
      },
    ]);
  };

  const existInstuctionMsg = (list: HistoryMsgItemType[]) => {
    return list.some((msg) => msg.queryResponse.queryMode === MessageTypeEnum.INSTRUCTION);
  };

  const updateScroll = (list: HistoryMsgItemType[]) => {
    if (existInstuctionMsg(list)) {
      setMiniProgramLoading(true);
      setTimeout(() => {
        setMiniProgramLoading(false);
        updateMessageContainerScroll();
      }, 3000);
    } else {
      updateMessageContainerScroll();
    }
  };

  const updateHistoryMsg = async (page: number) => {
    const res = await getHistoryMsg(page, currentConversation!.chatId, 3);
    const { hasNextPage, list } = res.data?.data || { hasNextPage: false, list: [] };
    setMessageList([
      ...list.map((item: HistoryMsgItemType) => ({
        id: item.questionId,
        type:
          item.queryResponse?.queryMode === MessageTypeEnum.INSTRUCTION
            ? MessageTypeEnum.INSTRUCTION
            : MessageTypeEnum.QUESTION,
        msg: item.queryText,
        msgData: item.queryResponse,
        isHistory: true,
      })),
      ...(page === 1 ? [] : messageList),
    ]);
    setHasNextPage(hasNextPage);
    if (page === 1) {
      if (list.length === 0) {
        sendHelloRsp();
      } else {
        setCurrentEntity(list[list.length - 1].queryResponse);
      }
      updateScroll(list);
      setHistoryInited(true);
      inputFocus();
    }
    if (page > 1) {
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

  const initDomains = async () => {
    try {
      const res = await getDomainList();
      const domainList = getLeafList(res.data);
      setDomains(
        [{ id: -1, name: '全部', bizName: 'all', parentId: 0 }, ...domainList].slice(0, 11),
      );
    } catch (e) {}
  };

  useEffect(() => {
    setChatSdkToken(localStorage.getItem(TOKEN_KEY) || '');
    initDomains();
  }, []);

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

  const inputFocus = () => {
    if (!isMobile) {
      chatFooterRef.current?.inputFocus();
    }
  };

  const inputBlur = () => {
    chatFooterRef.current?.inputBlur();
  };

  useEffect(() => {
    if (!currentConversation) {
      return;
    }
    setCurrentEntity(undefined);
    const { initMsg, domainId } = currentConversation;
    if (initMsg) {
      inputFocus();
      if (initMsg === DEFAULT_CONVERSATION_NAME) {
        sendHelloRsp();
        return;
      }
      onSendMsg(currentConversation.initMsg, [], domainId);
      return;
    }
    updateHistoryMsg(1);
    setPageNo(1);
  }, [currentConversation]);

  const modifyConversationName = async (name: string) => {
    await updateConversationName(name, currentConversation!.chatId);
    if (!isMobileMode) {
      conversationRef?.current?.updateData();
      window.history.replaceState('', '', `?q=${name}&cid=${currentConversation!.chatId}`);
    }
  };

  const onSendMsg = async (msg?: string, list?: MessageItem[], domainId?: number) => {
    const currentMsg = msg || inputMsg;
    if (currentMsg.trim() === '') {
      setInputMsg('');
      return;
    }
    const msgDomain = domains.find((item) => currentMsg.includes(item.name));
    const certainDomain = currentMsg[0] === '@' && msgDomain;
    if (certainDomain) {
      setCurrentDomain(msgDomain.id === -1 ? undefined : msgDomain);
    }
    const domainIdValue = domainId || msgDomain?.id || currentDomain?.id;
    const msgs = [
      ...(list || messageList),
      {
        id: uuid(),
        msg: currentMsg,
        msgValue: certainDomain ? currentMsg.replace(`@${msgDomain.name}`, '').trim() : currentMsg,
        domainId: domainIdValue === -1 ? undefined : domainIdValue,
        identityMsg: certainDomain ? getIdentityMsgText(msgDomain) : undefined,
        type: MessageTypeEnum.QUESTION,
      },
    ];
    setMessageList(msgs);
    updateMessageContainerScroll();
    setInputMsg('');
    modifyConversationName(currentMsg);
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

  const onSelectConversation = (conversation: ConversationDetailType, name?: string) => {
    if (!isMobileMode) {
      window.history.replaceState('', '', `?q=${conversation.chatName}&cid=${conversation.chatId}`);
    }
    setCurrentConversation({
      ...conversation,
      initMsg: name,
    });
    saveConversationToLocal(conversation);
    setCurrentDomain(undefined);
  };

  const onMsgDataLoaded = (data: MsgDataType, questionId: string | number) => {
    if (!data) {
      return;
    }
    if (data.queryMode === 'INSTRUCTION') {
      setMessageList([
        ...messageList.slice(0, messageList.length - 1),
        {
          id: uuid(),
          msg: data.response.name || messageList[messageList.length - 1]?.msg,
          type: MessageTypeEnum.INSTRUCTION,
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
    setCurrentEntity(data);
  };

  const onCheckMore = (data: MsgDataType) => {
    setMessageList([
      ...messageList,
      {
        id: uuid(),
        msg: data.response.name,
        type: MessageTypeEnum.INSTRUCTION,
        msgData: data,
      },
    ]);
    updateMessageContainerScroll();
  };

  const getIdentityMsgText = (domain?: DomainType) => {
    return domain
      ? `您好，我当前身份是【${domain.name}】主题专家，我将尽力帮您解答相关问题～`
      : '您好，我将尽力帮您解答所有主题相关问题～';
  };

  const getIdentityMsg = (domain?: DomainType) => {
    return {
      id: uuid(),
      type: MessageTypeEnum.TEXT,
      msg: getIdentityMsgText(domain),
    };
  };

  const onSelectDomain = (domain: DomainType) => {
    const domainValue = currentDomain?.id === domain.id ? undefined : domain;
    setCurrentDomain(domainValue);
    setCurrentEntity(undefined);
    setMessageList([...messageList, getIdentityMsg(domainValue)]);
    updateMessageContainerScroll();
    inputFocus();
  };

  const chatClass = classNames(styles.chat, {
    [styles.mobile]: isMobileMode,
    [styles.copilot]: isCopilotMode,
  });

  return (
    <div className={chatClass}>
      {!isMobileMode && <Helmet title={WEB_TITLE} />}
      <div className={styles.topSection} />
      <div className={styles.chatSection}>
        <div className={styles.chatApp}>
          {currentConversation && (
            <div className={styles.chatBody}>
              <div className={styles.chatContent}>
                <MessageContainer
                  id="messageContainer"
                  messageList={messageList}
                  chatId={currentConversation?.chatId}
                  miniProgramLoading={miniProgramLoading}
                  isMobileMode={isMobileMode}
                  onClickMessageContainer={() => {
                    inputFocus();
                  }}
                  onMsgDataLoaded={onMsgDataLoaded}
                  onSelectSuggestion={onSendMsg}
                  onCheckMore={onCheckMore}
                  onUpdateMessageScroll={updateMessageContainerScroll}
                />
                <ChatFooter
                  inputMsg={inputMsg}
                  chatId={currentConversation?.chatId}
                  domains={domains}
                  currentDomain={currentDomain}
                  isMobileMode={isMobileMode}
                  onInputMsgChange={onInputMsgChange}
                  onSendMsg={(msg: string, domainId?: number) => {
                    onSendMsg(msg, messageList, domainId);
                    if (isMobile) {
                      inputBlur();
                    }
                  }}
                  onAddConversation={() => {
                    conversationRef.current?.onAddConversation();
                    inputFocus();
                  }}
                  ref={chatFooterRef}
                />
              </div>
            </div>
          )}
        </div>
        {!isMobileMode && (
          <RightSection
            domains={domains}
            currentEntity={currentEntity}
            currentDomain={currentDomain}
            currentConversation={currentConversation}
            onSelectDomain={onSelectDomain}
            onSelectConversation={onSelectConversation}
            conversationRef={conversationRef}
          />
        )}
      </div>
    </div>
  );
};

export default Chat;

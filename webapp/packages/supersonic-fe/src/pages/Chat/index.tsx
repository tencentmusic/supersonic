import { updateMessageContainerScroll, isMobile, uuid } from '@/utils/utils';
import { useEffect, useRef, useState } from 'react';
import { Helmet } from 'umi';
import MessageContainer from './MessageContainer';
import styles from './style.less';
import { ConversationDetailType, MessageItem, MessageTypeEnum } from './type';
import { updateConversationName } from './service';
import { useThrottleFn } from 'ahooks';
import Conversation from './Conversation';
import RightSection from './RightSection';
import ChatFooter from './ChatFooter';
import classNames from 'classnames';
import { DEFAULT_CONVERSATION_NAME, WEB_TITLE } from '@/common/constants';
import { HistoryMsgItemType, MsgDataType, getHistoryMsg, queryContext } from 'supersonic-chat-sdk';
import { getConversationContext } from './utils';

const Chat = () => {
  const [messageList, setMessageList] = useState<MessageItem[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  const [pageNo, setPageNo] = useState(1);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [historyInited, setHistoryInited] = useState(false);
  const [currentConversation, setCurrentConversation] = useState<
    ConversationDetailType | undefined
  >(isMobile ? { chatId: 0, chatName: '问答对话' } : undefined);
  const [currentEntity, setCurrentEntity] = useState<MsgDataType>();
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

  const updateHistoryMsg = async (page: number) => {
    const res = await getHistoryMsg(page, currentConversation!.chatId);
    const { hasNextPage, list } = res.data.data;
    setMessageList([
      ...list.map((item: HistoryMsgItemType) => ({
        id: item.questionId,
        type: MessageTypeEnum.QUESTION,
        msg: item.queryText,
        msgData: item.queryResponse,
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
      updateMessageContainerScroll();
      setHistoryInited(true);
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
      onSendMsg(currentConversation.initMsg, [], domainId, true);
      return;
    }
    updateHistoryMsg(1);
    setPageNo(1);
  }, [currentConversation]);

  const modifyConversationName = async (name: string) => {
    await updateConversationName(name, currentConversation!.chatId);
    conversationRef?.current?.updateData();
    window.history.replaceState('', '', `?q=${name}&cid=${currentConversation!.chatId}`);
  };

  const onSendMsg = async (
    msg?: string,
    list?: MessageItem[],
    domainId?: number,
    firstMsg?: boolean,
  ) => {
    const currentMsg = msg || inputMsg;
    if (currentMsg.trim() === '') {
      setInputMsg('');
      return;
    }
    let quote = '';
    if (currentEntity && !firstMsg) {
      const { data } = await queryContext(currentMsg, currentConversation!.chatId);
      if (data.code === 200 && data.data.domainId === currentEntity.chatContext?.domainId) {
        quote = getConversationContext(data.data);
      }
    }
    setMessageList([
      ...(list || messageList),
      { id: uuid(), msg: currentMsg, domainId, type: MessageTypeEnum.QUESTION, quote },
    ]);
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
    window.history.replaceState('', '', `?q=${conversation.chatName}&cid=${conversation.chatId}`);
    setCurrentConversation({
      ...conversation,
      initMsg: name,
    });
    saveConversationToLocal(conversation);
  };

  const onMsgDataLoaded = (data: MsgDataType) => {
    setCurrentEntity(data);
    updateMessageContainerScroll();
  };

  const chatClass = classNames(styles.chat, {
    [styles.external]: true,
    [styles.mobile]: isMobile,
  });

  return (
    <div className={chatClass}>
      <Helmet title={WEB_TITLE} />
      <div className={styles.topSection} />
      <div className={styles.chatSection}>
        {!isMobile && (
          <Conversation
            currentConversation={currentConversation}
            onSelectConversation={onSelectConversation}
            ref={conversationRef}
          />
        )}
        <div className={styles.chatApp}>
          {currentConversation && (
            <div className={styles.chatBody}>
              <div className={styles.chatContent}>
                <MessageContainer
                  id="messageContainer"
                  messageList={messageList}
                  chatId={currentConversation?.chatId}
                  onClickMessageContainer={() => {
                    inputFocus();
                  }}
                  onMsgDataLoaded={onMsgDataLoaded}
                  onSelectSuggestion={onSendMsg}
                  onUpdateMessageScroll={updateMessageContainerScroll}
                />
                <ChatFooter
                  inputMsg={inputMsg}
                  chatId={currentConversation?.chatId}
                  onInputMsgChange={onInputMsgChange}
                  onSendMsg={(msg: string, domainId?: number) => {
                    onSendMsg(msg, messageList, domainId);
                    if (isMobile) {
                      inputBlur();
                    }
                  }}
                  ref={chatFooterRef}
                />
              </div>
            </div>
          )}
        </div>
        {!isMobile && <RightSection currentEntity={currentEntity} />}
      </div>
    </div>
  );
};

export default Chat;

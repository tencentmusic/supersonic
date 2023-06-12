import Text from './components/Text';
import { memo, useCallback, useEffect } from 'react';
import { isEqual } from 'lodash';
import styles from './style.less';
import { connect, Dispatch } from 'umi';
import { ChatItem } from 'supersonic-chat-sdk';
import type { MsgDataType } from 'supersonic-chat-sdk';
import { MessageItem, MessageTypeEnum } from './type';

type Props = {
  id: string;
  chatId: number;
  messageList: MessageItem[];
  dispatch: Dispatch;
  onClickMessageContainer: () => void;
  onMsgDataLoaded: (data: MsgDataType) => void;
  onSelectSuggestion: (value: string) => void;
  onUpdateMessageScroll: () => void;
};

const MessageContainer: React.FC<Props> = ({
  id,
  chatId,
  messageList,
  dispatch,
  onClickMessageContainer,
  onMsgDataLoaded,
  onSelectSuggestion,
  onUpdateMessageScroll,
}) => {
  const onWindowResize = useCallback(() => {
    dispatch({
      type: 'windowResize/setTriggerResize',
      payload: true,
    });
    setTimeout(() => {
      dispatch({
        type: 'windowResize/setTriggerResize',
        payload: false,
      });
    }, 0);
  }, []);

  useEffect(() => {
    window.addEventListener('resize', onWindowResize);
    return () => {
      window.removeEventListener('resize', onWindowResize);
    };
  }, []);

  return (
    <div id={id} className={styles.messageContainer} onClick={onClickMessageContainer}>
      <div className={styles.messageList}>
        {messageList.map((msgItem: MessageItem, index: number) => {
          return (
            <div key={`${msgItem.id}`} id={`${msgItem.id}`} className={styles.messageItem}>
              {msgItem.type === MessageTypeEnum.TEXT && <Text position="left" data={msgItem.msg} />}
              {msgItem.type === MessageTypeEnum.QUESTION && (
                <>
                  <Text position="right" data={msgItem.msg} quote={msgItem.quote} />
                  <ChatItem
                    msg={msgItem.msg || ''}
                    msgData={msgItem.msgData}
                    conversationId={chatId}
                    classId={msgItem.domainId}
                    isLastMessage={index === messageList.length - 1}
                    onLastMsgDataLoaded={onMsgDataLoaded}
                    onSelectSuggestion={onSelectSuggestion}
                    onUpdateMessageScroll={onUpdateMessageScroll}
                    suggestionEnable
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
  if (prevProps.id === nextProps.id && isEqual(prevProps.messageList, nextProps.messageList)) {
    return true;
  }
  return false;
}

export default connect()(memo(MessageContainer, areEqual));

import { CloseOutlined } from '@ant-design/icons';
import moment from 'moment';
import type { ConversationDetailType } from '../../type';
import styles from './style.less';

type Props = {
  conversations: ConversationDetailType[];
  onSelectConversation: (conversation: ConversationDetailType) => void;
  onClose: () => void;
};

const ConversationHistory: React.FC<Props> = ({ conversations, onSelectConversation, onClose }) => {
  return (
    <div className={styles.conversationHistory}>
      <div className={styles.header}>
        <div className={styles.headerTitle}>历史记录</div>
        <CloseOutlined className={styles.headerClose} onClick={onClose} />
      </div>
      <div className={styles.conversationContent}>
        {conversations.slice(0, 1000).map((conversation) => {
          return (
            <div
              key={conversation.chatId}
              className={styles.conversationItem}
              onClick={() => {
                onSelectConversation(conversation);
              }}
            >
              <div className={styles.conversationName} title={conversation.chatName}>
                {conversation.chatName}
              </div>
              <div className={styles.conversationTime}>
                更新时间：{moment(conversation.lastTime).format('YYYY-MM-DD')}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ConversationHistory;

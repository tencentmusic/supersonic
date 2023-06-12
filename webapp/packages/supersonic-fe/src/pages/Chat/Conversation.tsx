import IconFont from '@/components/IconFont';
import { Dropdown, Menu, message } from 'antd';
import classNames from 'classnames';
import {
  useEffect,
  useState,
  forwardRef,
  ForwardRefRenderFunction,
  useImperativeHandle,
} from 'react';
import { useLocation } from 'umi';
import ConversationHistory from './components/ConversationHistory';
import ConversationModal from './components/ConversationModal';
import { deleteConversation, getAllConversations, saveConversation } from './service';
import styles from './style.less';
import { ConversationDetailType } from './type';

type Props = {
  currentConversation?: ConversationDetailType;
  onSelectConversation: (conversation: ConversationDetailType, name?: string) => void;
};

const Conversation: ForwardRefRenderFunction<any, Props> = (
  { currentConversation, onSelectConversation },
  ref,
) => {
  const location = useLocation();
  const { q, cid } = (location as any).query;
  const [originConversations, setOriginConversations] = useState<ConversationDetailType[]>([]);
  const [conversations, setConversations] = useState<ConversationDetailType[]>([]);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editConversation, setEditConversation] = useState<ConversationDetailType>();
  const [historyVisible, setHistoryVisible] = useState(false);

  useImperativeHandle(ref, () => ({
    updateData,
    onAddConversation,
  }));

  const updateData = async () => {
    const { data } = await getAllConversations();
    const conversationList = (data || []).slice(0, 5);
    setOriginConversations(data || []);
    setConversations(conversationList);
    return conversationList;
  };

  const initData = async () => {
    const data = await updateData();
    if (data.length > 0) {
      const chatId = localStorage.getItem('CONVERSATION_ID') || cid;
      if (chatId) {
        const conversation = data.find((item: any) => item.chatId === +chatId);
        if (conversation) {
          onSelectConversation(conversation);
        } else {
          onSelectConversation(data[0]);
        }
      } else {
        onSelectConversation(data[0]);
      }
    } else {
      onAddConversation();
    }
  };

  useEffect(() => {
    if (q && cid === undefined) {
      onAddConversation(q);
    } else {
      initData();
    }
  }, [q]);

  const addConversation = async (name?: string) => {
    await saveConversation(name || '新问答对话');
    return updateData();
  };

  const onDeleteConversation = async (id: number) => {
    await deleteConversation(id);
    initData();
  };

  const onAddConversation = async (name?: string) => {
    const data = await addConversation(name);
    onSelectConversation(data[0], name);
  };

  const onOperate = (key: string, conversation: ConversationDetailType) => {
    if (key === 'editName') {
      setEditConversation(conversation);
      setEditModalVisible(true);
    } else if (key === 'delete') {
      onDeleteConversation(conversation.chatId);
    }
  };

  const onNewChat = () => {
    onAddConversation('新问答对话');
  };

  const onShowHistory = () => {
    setHistoryVisible(true);
  };

  const onShare = () => {
    message.info('正在开发中，敬请期待');
  };

  return (
    <div className={styles.conversation}>
      <div className={styles.leftSection}>
        <div className={styles.conversationList}>
          {conversations.map((item) => {
            const conversationItemClass = classNames(styles.conversationItem, {
              [styles.activeConversationItem]: currentConversation?.chatId === item.chatId,
            });
            return (
              <Dropdown
                key={item.chatId}
                overlay={
                  <Menu
                    items={[
                      { label: '修改对话名称', key: 'editName' },
                      { label: '删除', key: 'delete' },
                    ]}
                    onClick={({ key }) => {
                      onOperate(key, item);
                    }}
                  />
                }
                trigger={['contextMenu']}
              >
                <div
                  key={item.chatId}
                  className={conversationItemClass}
                  onClick={() => {
                    onSelectConversation(item);
                  }}
                >
                  <div className={styles.conversationItemContent}>
                    <IconFont type="icon-chat1" className={styles.conversationIcon} />
                    <div className={styles.conversationContent} title={item.chatName}>
                      {item.chatName}
                    </div>
                  </div>
                </div>
              </Dropdown>
            );
          })}
          <div className={styles.conversationItem} onClick={onShowHistory}>
            <div className={styles.conversationItemContent}>
              <IconFont
                type="icon-more2"
                className={`${styles.conversationIcon} ${styles.historyIcon}`}
              />
              <div className={styles.conversationContent}>查看更多对话</div>
            </div>
          </div>
        </div>
        <div className={styles.operateSection}>
          <div className={styles.operateItem} onClick={onNewChat}>
            <IconFont type="icon-add" className={`${styles.operateIcon} ${styles.addIcon}`} />
            <div className={styles.operateLabel}>新建对话</div>
          </div>
          <div className={styles.operateItem} onClick={onShare}>
            <IconFont
              type="icon-fenxiang2"
              className={`${styles.operateIcon} ${styles.shareIcon}`}
            />
            <div className={styles.operateLabel}>分享</div>
          </div>
        </div>
      </div>
      {historyVisible && (
        <ConversationHistory
          conversations={originConversations}
          onSelectConversation={(conversation) => {
            onSelectConversation(conversation);
            setHistoryVisible(false);
          }}
          onClose={() => {
            setHistoryVisible(false);
          }}
        />
      )}
      <ConversationModal
        visible={editModalVisible}
        editConversation={editConversation}
        onClose={() => {
          setEditModalVisible(false);
        }}
        onFinish={() => {
          setEditModalVisible(false);
          updateData();
        }}
      />
    </div>
  );
};

export default forwardRef(Conversation);

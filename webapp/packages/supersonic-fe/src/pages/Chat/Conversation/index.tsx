import IconFont from '@/components/IconFont';
import { Dropdown, Input, Menu } from 'antd';
import classNames from 'classnames';
import {
  useEffect,
  useState,
  forwardRef,
  ForwardRefRenderFunction,
  useImperativeHandle,
} from 'react';
import { useLocation } from 'umi';
import ConversationModal from './ConversationModal';
import { deleteConversation, getAllConversations, saveConversation } from '../service';
import styles from './style.less';
import { ConversationDetailType } from '../type';
import moment from 'moment';
import { SearchOutlined } from '@ant-design/icons';
import { DEFAULT_CONVERSATION_NAME } from '@/common/constants';

type Props = {
  currentConversation?: ConversationDetailType;
  collapsed?: boolean;
  onSelectConversation: (
    conversation: ConversationDetailType,
    name?: string,
    domainId?: number,
    entityId?: string,
  ) => void;
};

const Conversation: ForwardRefRenderFunction<any, Props> = (
  { currentConversation, collapsed, onSelectConversation },
  ref,
) => {
  const location = useLocation();
  const { q, cid, domainId, entityId } = (location as any).query;
  const [conversations, setConversations] = useState<ConversationDetailType[]>([]);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editConversation, setEditConversation] = useState<ConversationDetailType>();
  const [searchValue, setSearchValue] = useState('');

  useImperativeHandle(ref, () => ({
    updateData,
    onAddConversation,
  }));

  const updateData = async () => {
    const { data } = await getAllConversations();
    const conversationList = data || [];
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
    if (q && cid === undefined && window.location.href.includes('/workbench/chat')) {
      onAddConversation({ name: q, domainId: domainId ? +domainId : undefined, entityId });
    } else {
      initData();
    }
  }, [q]);

  const addConversation = async (name?: string) => {
    await saveConversation(name || DEFAULT_CONVERSATION_NAME);
    return updateData();
  };

  const onDeleteConversation = async (id: number) => {
    await deleteConversation(id);
    initData();
  };

  const onAddConversation = async ({
    name,
    domainId,
    entityId,
    type,
  }: {
    name?: string;
    domainId?: number;
    entityId?: string;
    type?: string;
  } = {}) => {
    const data = await addConversation(name);
    onSelectConversation(data[0], type || name, domainId, entityId);
  };

  const onOperate = (key: string, conversation: ConversationDetailType) => {
    if (key === 'editName') {
      setEditConversation(conversation);
      setEditModalVisible(true);
    } else if (key === 'delete') {
      onDeleteConversation(conversation.chatId);
    }
  };

  const conversationClass = classNames(styles.conversation, {
    [styles.collapsed]: collapsed,
  });

  const convertTime = (date: string) => {
    moment.locale('zh-cn');
    const now = moment();
    const inputDate = moment(date);
    const diffMinutes = now.diff(inputDate, 'minutes');
    if (diffMinutes < 1) {
      return '刚刚';
    } else if (inputDate.isSame(now, 'day')) {
      return inputDate.format('HH:mm');
    } else if (inputDate.isSame(now.subtract(1, 'day'), 'day')) {
      return '昨天';
    }
    return inputDate.format('MM/DD');
  };

  const onSearchValueChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(e.target.value);
  };

  return (
    <div className={conversationClass}>
      <div className={styles.leftSection}>
        <div className={styles.searchConversation}>
          <Input
            placeholder="搜索"
            prefix={<SearchOutlined className={styles.searchIcon} />}
            className={styles.searchTask}
            value={searchValue}
            onChange={onSearchValueChange}
            allowClear
          />
        </div>
        <div className={styles.conversationList}>
          {conversations
            .filter(
              (conversation) =>
                searchValue === '' ||
                conversation.chatName.toLowerCase().includes(searchValue.toLowerCase()),
            )
            .map((item) => {
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
                    className={conversationItemClass}
                    onClick={() => {
                      onSelectConversation(item);
                    }}
                  >
                    <IconFont type="icon-chat1" className={styles.conversationIcon} />
                    <div className={styles.conversationContent}>
                      <div className={styles.topTitleBar}>
                        <div className={styles.conversationName}>{item.chatName}</div>
                        <div className={styles.conversationTime}>
                          {convertTime(item.lastTime || '')}
                        </div>
                      </div>
                      <div className={styles.subTitle}>{item.lastQuestion}</div>
                    </div>
                  </div>
                </Dropdown>
              );
            })}
        </div>
      </div>
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

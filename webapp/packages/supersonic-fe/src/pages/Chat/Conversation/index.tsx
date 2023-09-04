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
import ConversationModal from '../components/ConversationModal';
import { deleteConversation, getAllConversations, saveConversation } from '../service';
import styles from './style.less';
import { AgentType, ConversationDetailType, DefaultEntityType } from '../type';
import { DEFAULT_CONVERSATION_NAME } from '../constants';
import moment from 'moment';
import { CloseOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';

type Props = {
  agentList?: AgentType[];
  currentAgent?: AgentType;
  currentConversation?: ConversationDetailType;
  historyVisible?: boolean;
  isCopilotMode?: boolean;
  defaultEntityFilter?: DefaultEntityType;
  triggerNewConversation?: boolean;
  onNewConversationTriggered?: () => void;
  onSelectConversation: (
    conversation: ConversationDetailType,
    name?: string,
    modelId?: number,
    entityId?: string,
    agent?: AgentType,
  ) => void;
  onCloseConversation: () => void;
};

const Conversation: ForwardRefRenderFunction<any, Props> = (
  {
    agentList,
    currentAgent,
    currentConversation,
    historyVisible,
    isCopilotMode,
    defaultEntityFilter,
    triggerNewConversation,
    onNewConversationTriggered,
    onSelectConversation,
    onCloseConversation,
  },
  ref,
) => {
  const location = useLocation();
  const { q, cid, modelId, entityId } = (location as any).query;
  const [conversations, setConversations] = useState<ConversationDetailType[]>([]);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editConversation, setEditConversation] = useState<ConversationDetailType>();
  const [searchValue, setSearchValue] = useState('');

  useImperativeHandle(ref, () => ({
    updateData,
    onAddConversation,
  }));

  const updateData = async (agent?: AgentType) => {
    const { data } = await getAllConversations(agent?.id || currentAgent?.id);
    const conversationList = data || [];
    setConversations(conversationList.slice(0, 200));
    return conversationList;
  };

  const initData = async () => {
    const data = await updateData();
    if (data.length > 0) {
      const chatId = cid;
      if (chatId) {
        const conversation = data.find((item: any) => item.chatId === +chatId);
        onSelectConversation(conversation || data[0]);
      } else {
        onSelectConversation(data[0]);
      }
    } else {
      onAddConversation();
    }
  };

  // useEffect(() => {
  //   if (triggerNewConversation) {
  //     return;
  //   }
  //   if (q && cid === undefined && window.location.href.includes('/chat')) {
  //     onAddConversation({
  //       name: q,
  //       modelId: modelId ? +modelId : undefined,
  //       entityId,
  //     });
  //   } else {
  //     initData();
  //   }
  // }, [q]);

  useEffect(() => {
    if (currentAgent && !triggerNewConversation) {
      initData();
    }
  }, [currentAgent]);

  const addConversation = async (name?: string, agent?: AgentType) => {
    await saveConversation(name || DEFAULT_CONVERSATION_NAME, agent?.id || currentAgent!.id);
    return updateData(agent);
  };

  const onDeleteConversation = async (id: number) => {
    await deleteConversation(id);
    initData();
  };

  const onAddConversation = async ({
    name,
    modelId,
    entityId,
    type,
    agent,
  }: {
    name?: string;
    modelId?: number;
    entityId?: string;
    type?: string;
    agent?: AgentType;
  } = {}) => {
    const data = await addConversation(name, agent);
    if (data.length > 0) {
      onSelectConversation(
        data[0],
        type || name || DEFAULT_CONVERSATION_NAME,
        modelId,
        entityId,
        agent,
      );
    }
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
    [styles.historyVisible]: historyVisible,
    [styles.copilotMode]: isCopilotMode,
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
      <div className={styles.rightSection}>
        <div className={styles.titleBar}>
          <div className={styles.title}>历史对话</div>
          <div className={styles.rightOperation}>
            <div
              className={styles.newConversation}
              onClick={() => {
                addConversation();
              }}
            >
              新对话
            </div>
            <CloseOutlined className={styles.closeIcon} onClick={onCloseConversation} />
          </div>
        </div>
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
                    <div className={styles.conversationContent}>
                      <div className={styles.topTitleBar}>
                        <div className={styles.conversationTitleBar}>
                          <div className={styles.conversationName}>{item.chatName}</div>
                          {currentConversation?.chatId === item.chatId && (
                            <div className={styles.currentConversation}>当前对话</div>
                          )}
                        </div>
                        <div className={styles.conversationTime}>
                          {convertTime(item.lastTime || '')}
                        </div>
                      </div>
                      <div className={styles.bottomSection}>
                        <div className={styles.subTitle}>{item.lastQuestion}</div>
                        <DeleteOutlined
                          className={styles.deleteIcon}
                          onClick={(e) => {
                            e.stopPropagation();
                            onDeleteConversation(item.chatId);
                          }}
                        />
                      </div>
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

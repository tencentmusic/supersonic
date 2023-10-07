import { Dropdown, Input, Menu } from 'antd';
import classNames from 'classnames';
import {
  useEffect,
  useState,
  forwardRef,
  ForwardRefRenderFunction,
  useImperativeHandle,
  memo,
} from 'react';
import ConversationModal from '../components/ConversationModal';
import { deleteConversation, getAllConversations, saveConversation } from '../service';
import styles from './style.module.less';
import { AgentType, ConversationDetailType } from '../type';
import { DEFAULT_CONVERSATION_NAME } from '../constants';
import moment from 'moment';
import { CloseOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';

type Props = {
  currentAgent?: AgentType;
  currentConversation?: ConversationDetailType;
  historyVisible?: boolean;
  onSelectConversation: (
    conversation: ConversationDetailType,
    sendMsgParams?: any,
    isAdd?: boolean
  ) => void;
  onCloseConversation: () => void;
};

const Conversation: ForwardRefRenderFunction<any, Props> = (
  { currentAgent, currentConversation, historyVisible, onSelectConversation, onCloseConversation },
  ref
) => {
  const [conversations, setConversations] = useState<ConversationDetailType[]>([]);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editConversation, setEditConversation] = useState<ConversationDetailType>();
  const [searchValue, setSearchValue] = useState('');

  useImperativeHandle(ref, () => ({
    updateData,
    onAddConversation,
  }));

  const updateData = async (agentId?: number) => {
    const { data } = await getAllConversations(agentId || currentAgent!.id);
    const conversationList = data || [];
    setConversations(conversationList.slice(0, 200));
    return conversationList;
  };

  const initData = async () => {
    const data = await updateData();
    if (data.length > 0) {
      onSelectConversation(data[0]);
    } else {
      onAddConversation();
    }
  };

  useEffect(() => {
    if (currentAgent) {
      if (currentAgent.initialSendMsgParams) {
        onAddConversation(currentAgent.initialSendMsgParams);
      } else {
        initData();
      }
    }
  }, [currentAgent]);

  const addConversation = async (sendMsgParams?: any) => {
    const agentId = sendMsgParams?.agentId || currentAgent!.id;
    await saveConversation(DEFAULT_CONVERSATION_NAME, agentId);
    return updateData(agentId);
  };

  const onDeleteConversation = async (id: number) => {
    await deleteConversation(id);
    initData();
  };

  const onAddConversation = async (sendMsgParams?: any) => {
    const data = await addConversation(sendMsgParams);
    if (data.length > 0) {
      onSelectConversation(data[0], sendMsgParams, true);
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
              conversation =>
                searchValue === '' ||
                conversation.chatName.toLowerCase().includes(searchValue.toLowerCase())
            )
            .map(item => {
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
                          onClick={e => {
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

function areEqual(prevProps: Props, nextProps: Props) {
  if (
    prevProps.currentAgent?.id === nextProps.currentAgent?.id &&
    prevProps.currentConversation?.chatId === nextProps.currentConversation?.chatId &&
    prevProps.historyVisible === nextProps.historyVisible
  ) {
    return true;
  }
  return false;
}

export default memo(forwardRef(Conversation), areEqual);

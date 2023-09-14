import IconFont from '@/components/IconFont';
import { CaretRightOutlined, CloseOutlined } from '@ant-design/icons';
import classNames from 'classnames';
import { useEffect, useState } from 'react';
import Chat from '../Chat';
import { DefaultEntityType, ModelType } from '../Chat/type';
import styles from './style.less';

type Props = {
  globalCopilotFilter: DefaultEntityType;
  copilotSendMsg: string;
};

const Copilot: React.FC<Props> = ({ globalCopilotFilter, copilotSendMsg }) => {
  const [chatVisible, setChatVisible] = useState(false);
  const [defaultModelName, setDefaultModelName] = useState('');
  const [defaultEntityFilter, setDefaultEntityFilter] = useState<DefaultEntityType>();
  const [triggerNewConversation, setTriggerNewConversation] = useState(false);
  const [copilotMinimized, setCopilotMinimized] = useState(false);

  useEffect(() => {
    if (globalCopilotFilter && globalCopilotFilter.entityId !== defaultEntityFilter?.entityId) {
      setTriggerNewConversation(true);
    }
    setDefaultEntityFilter(globalCopilotFilter);
    if (globalCopilotFilter?.modelName) {
      setDefaultModelName(globalCopilotFilter.modelName);
    }
  }, [globalCopilotFilter]);

  useEffect(() => {
    if (copilotSendMsg) {
      updateChatVisible(true);
      setTriggerNewConversation(true);
    }
  }, [copilotSendMsg]);

  const updateChatVisible = (visible: boolean) => {
    setChatVisible(visible);
  };

  const onToggleChatVisible = () => {
    const chatVisibleValue = !chatVisible;
    updateChatVisible(chatVisibleValue);
  };

  const onCloseChat = () => {
    updateChatVisible(false);
  };

  const onTransferChat = () => {
    window.open('/chat');
  };

  const onCurrentModelChange = (model?: ModelType) => {
    setDefaultModelName(model?.name || '');
  };

  const onNewConversationTriggered = () => {
    setTriggerNewConversation(false);
  };

  const onMinimizeCopilot = (e: any) => {
    e.stopPropagation();
    updateChatVisible(false);
    setCopilotMinimized(true);
  };

  const copilotClass = classNames(styles.copilot, {
    [styles.copilotMinimized]: copilotMinimized,
  });

  return (
    <>
      <div
        className={copilotClass}
        onMouseEnter={() => {
          setCopilotMinimized(false);
        }}
        onClick={onToggleChatVisible}
      >
        <IconFont type="icon-copilot-fill" />
        <div className={styles.minimizeWrapper} onClick={onMinimizeCopilot}>
          <div className={styles.minimize}>-</div>
        </div>
      </div>
      <div className={styles.copilotContent} style={{ display: chatVisible ? 'block' : 'none' }}>
        <div className={styles.chatPopover}>
          <div className={styles.header}>
            <div className={styles.leftSection}>
              <CloseOutlined className={styles.close} onClick={onCloseChat} />
              <IconFont
                type="icon-weibiaoti-"
                className={styles.transfer}
                onClick={onTransferChat}
              />
            </div>
            <div className={styles.title}>Copilot</div>
          </div>
          <div className={styles.chat}>
            <Chat
              copilotSendMsg={copilotSendMsg}
              defaultModelName={defaultModelName}
              defaultEntityFilter={defaultEntityFilter}
              triggerNewConversation={triggerNewConversation}
              chatVisible={chatVisible}
              isCopilotMode
              onNewConversationTriggered={onNewConversationTriggered}
              onCurrentModelChange={onCurrentModelChange}
            />
          </div>
        </div>
        <CaretRightOutlined className={styles.rightArrow} />
      </div>
    </>
  );
};

export default Copilot;

import IconFont from '@/components/IconFont';
import {
  CaretRightOutlined,
  CloseOutlined,
  FullscreenExitOutlined,
  FullscreenOutlined,
} from '@ant-design/icons';
import classNames from 'classnames';
import { useEffect, useState } from 'react';
import Chat from '../Chat';
import { ModelType } from '../Chat/type';
import styles from './style.less';
import { useDispatch } from 'umi';

type Props = {
  copilotSendMsg: string;
};

const Copilot: React.FC<Props> = ({ copilotSendMsg }) => {
  const [chatVisible, setChatVisible] = useState(false);
  const [defaultModelName, setDefaultModelName] = useState('');
  const [fullscreen, setFullscreen] = useState(false);
  const [triggerNewConversation, setTriggerNewConversation] = useState(false);
  const dispatch = useDispatch();

  useEffect(() => {
    const chatVisibleValue = localStorage.getItem('CHAT_VISIBLE') === 'true';
    if (chatVisibleValue) {
      setTimeout(() => {
        setChatVisible(true);
      }, 500);
    }
  }, []);

  useEffect(() => {
    if (copilotSendMsg) {
      updateChatVisible(true);
      setTriggerNewConversation(true);
    }
  }, [copilotSendMsg]);

  const updateChatVisible = (visible: boolean) => {
    setChatVisible(visible);
    localStorage.setItem('CHAT_VISIBLE', visible ? 'true' : 'false');
  };

  const onToggleChatVisible = () => {
    const chatVisibleValue = !chatVisible;
    updateChatVisible(chatVisibleValue);
    if (!chatVisibleValue) {
      document.body.style.overflow = 'auto';
    } else {
      if (fullscreen) {
        document.body.style.overflow = 'hidden';
      } else {
        document.body.style.overflow = 'auto';
      }
    }
  };

  const onCloseChat = () => {
    updateChatVisible(false);
    document.body.style.overflow = 'auto';
  };

  const onTransferChat = () => {
    window.open(
      `${window.location.href.includes('webapp') ? '/webapp' : ''}/chat?cid=${localStorage.getItem(
        'CONVERSATION_ID',
      )}${defaultModelName ? `&modelName=${defaultModelName}` : ''}`,
    );
  };

  const onCurrentModelChange = (model?: ModelType) => {
    setDefaultModelName(model?.name || '');
    if (model?.name !== defaultModelName) {
      onCancelCopilotFilter();
    }
  };

  const onEnterFullscreen = () => {
    setFullscreen(true);
    document.body.style.overflow = 'hidden';
  };

  const onExitFullscreen = () => {
    setFullscreen(false);
    document.body.style.overflow = 'auto';
  };

  const onCheckMoreDetail = () => {
    if (!fullscreen) {
      onEnterFullscreen();
    }
  };

  const onCancelCopilotFilter = () => {
    dispatch({
      type: 'globalState/setGlobalCopilotFilter',
      payload: undefined,
    });
  };

  const onNewConversationTriggered = () => {
    setTriggerNewConversation(false);
  };

  const chatPopoverClass = classNames(styles.chatPopover, {
    [styles.fullscreen]: fullscreen,
  });

  return (
    <>
      <div className={styles.copilot} onClick={onToggleChatVisible}>
        <IconFont type="icon-copilot-fill" />
      </div>
      {chatVisible && (
        <div className={styles.copilotContent}>
          <div className={chatPopoverClass}>
            <div className={styles.header}>
              <div className={styles.leftSection}>
                <CloseOutlined className={styles.close} onClick={onCloseChat} />
                {fullscreen ? (
                  <FullscreenExitOutlined
                    className={styles.fullscreen}
                    onClick={onExitFullscreen}
                  />
                ) : (
                  <FullscreenOutlined className={styles.fullscreen} onClick={onEnterFullscreen} />
                )}
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
                defaultModelName={defaultModelName}
                copilotSendMsg={copilotSendMsg}
                isCopilotMode
                copilotFullscreen={fullscreen}
                triggerNewConversation={triggerNewConversation}
                onNewConversationTriggered={onNewConversationTriggered}
                onCurrentModelChange={onCurrentModelChange}
                onCancelCopilotFilter={onCancelCopilotFilter}
                onCheckMoreDetail={onCheckMoreDetail}
              />
            </div>
          </div>
          <CaretRightOutlined className={styles.rightArrow} />
        </div>
      )}
    </>
  );
};

export default Copilot;

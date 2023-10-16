import IconFont from '../components/IconFont';
import { CaretRightOutlined, CloseOutlined } from '@ant-design/icons';
import classNames from 'classnames';
import {
  ForwardRefRenderFunction,
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import Chat from '../Chat';
import { AgentType } from '../Chat/type';
import { setToken } from '../utils/utils';
import { SendMsgParamsType } from '../common/type';
import styles from './style.module.less';
import { copilotTitle } from '../common/env';

type Props = {
  token?: string;
  agentIds?: number[];
  noInput?: boolean;
  isDeveloper?: boolean;
  integrateSystem?: string;
  apiUrl?: string;
  onReportMsgEvent?: (msg: string, valid: boolean) => void;
  onOpenChatPage?: (agentId?: number) => void;
};

const Copilot: ForwardRefRenderFunction<any, Props> = (
  {
    token,
    agentIds,
    noInput,
    isDeveloper,
    integrateSystem,
    apiUrl,
    onReportMsgEvent,
    onOpenChatPage,
  },
  ref
) => {
  const [chatVisible, setChatVisible] = useState(false);
  const [copilotMinimized, setCopilotMinimized] = useState(false);
  const [currentAgent, setCurrentAgent] = useState<AgentType>();

  const chatRef = useRef<any>();

  useImperativeHandle(ref, () => ({
    sendCopilotMsg,
  }));

  useEffect(() => {
    if (token) {
      setToken(token);
    }
  }, [token]);

  useEffect(() => {
    if (apiUrl) {
      localStorage.setItem('SUPERSONIC_CHAT_API_URL', apiUrl);
    }
  }, [apiUrl]);

  const sendCopilotMsg = (params: SendMsgParamsType) => {
    chatRef?.current?.sendCopilotMsg(params);
    updateChatVisible(true);
  };

  const updateChatVisible = (visible: boolean) => {
    setChatVisible(visible);
  };

  const onToggleChatVisible = () => {
    updateChatVisible(!chatVisible);
  };

  const onCloseChat = () => {
    updateChatVisible(false);
  };

  const onTransferChat = () => {
    onOpenChatPage?.(currentAgent?.id);
  };

  const onMinimizeCopilot = (e: any) => {
    e.stopPropagation();
    updateChatVisible(false);
    setCopilotMinimized(true);
  };

  const copilotClass = classNames(styles.copilot, {
    [styles.copilotMinimized]: copilotMinimized,
  });

  const chatPopoverClass = classNames(styles.chatPopover, {
    [styles.c2System]: integrateSystem === 'c2',
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
        <div className={chatPopoverClass}>
          <div className={styles.header}>
            <div className={styles.leftSection}>
              <CloseOutlined className={styles.close} onClick={onCloseChat} />
              {onOpenChatPage && (
                <IconFont
                  type="icon-weibiaoti-"
                  className={styles.transfer}
                  onClick={onTransferChat}
                />
              )}
            </div>
            <div className={styles.title}>{copilotTitle}</div>
          </div>
          <div className={styles.chat}>
            <Chat
              chatVisible={chatVisible}
              agentIds={agentIds}
              noInput={noInput}
              isDeveloper={isDeveloper}
              integrateSystem={integrateSystem}
              isCopilot
              onCurrentAgentChange={setCurrentAgent}
              onReportMsgEvent={onReportMsgEvent}
              ref={chatRef}
            />
          </div>
        </div>
        <CaretRightOutlined className={styles.rightArrow} />
      </div>
    </>
  );
};

export default forwardRef(Copilot);

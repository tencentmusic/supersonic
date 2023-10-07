import LeftAvatar from '../CopilotAvatar';
import Message from '../Message';
import styles from './style.module.less';
import { AgentType } from '../../type';
import { isMobile } from '../../../utils/utils';

type Props = {
  currentAgent?: AgentType;
  onSendMsg: (value: string) => void;
};

const AgentTip: React.FC<Props> = ({ currentAgent, onSendMsg }) => {
  if (!currentAgent) {
    return null;
  }
  return (
    <div className={styles.agentTip}>
      {!isMobile && <LeftAvatar />}
      <Message position="left" bubbleClassName={styles.agentTipMsg}>
        <div className={styles.title}>
          您好，智能助理【{currentAgent.name}
          】将与您对话，试着问：
        </div>
        <div className={styles.content}>
          <div className={styles.examples}>
            {currentAgent.examples?.length > 0 ? (
              currentAgent.examples.map(example => (
                <div
                  key={example}
                  className={styles.example}
                  onClick={() => {
                    onSendMsg(example);
                  }}
                >
                  “{example}”
                </div>
              ))
            ) : (
              <div className={styles.example}>{currentAgent.description}</div>
            )}
          </div>
        </div>
      </Message>
    </div>
  );
};

export default AgentTip;

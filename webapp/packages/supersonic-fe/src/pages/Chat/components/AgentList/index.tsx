import LeftAvatar from '../CopilotAvatar';
import Message from '../Message';
import styles from './style.less';
import { AgentType } from '../../type';
import classNames from 'classnames';

type Props = {
  currentAgentName: string;
  data: AgentType[];
  copilotFullscreen?: boolean;
  onSelectAgent: (agent: AgentType) => void;
};

const AgentList: React.FC<Props> = ({
  currentAgentName,
  data,
  copilotFullscreen,
  onSelectAgent,
}) => {
  const agentClass = classNames(styles.agent, {
    [styles.fullscreen]: copilotFullscreen,
  });
  return (
    <div className={styles.agentList}>
      <LeftAvatar />
      <Message position="left" bubbleClassName={styles.agentListMsg}>
        <div className={styles.title}>
          您好，智能助理【{currentAgentName}
          】将与您对话，点击以下卡片或者输入“/”可切换助理：
        </div>
        <div className={styles.content}>
          {data.map((agent) => (
            <div
              key={agent.id}
              className={agentClass}
              onClick={() => {
                onSelectAgent(agent);
              }}
            >
              <div className={styles.topBar}>
                <div className={styles.agentName}>{agent.name}</div>
                <div className={styles.tip}>您可以这样问：</div>
              </div>
              <div className={styles.examples}>
                {agent.examples?.length > 0 ? (
                  agent.examples.map((example) => (
                    <div key={example} className={styles.example}>
                      “{example}”
                    </div>
                  ))
                ) : (
                  <div className={styles.example}>{agent.description}</div>
                )}
              </div>
            </div>
          ))}
        </div>
      </Message>
    </div>
  );
};

export default AgentList;

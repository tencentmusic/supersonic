import IconFont from '@/components/IconFont';
import { Drawer } from 'antd';
import classNames from 'classnames';
import { AGENT_ICONS } from '../constants';
import { AgentType } from '../type';
import styles from './style.less';

type Props = {
  open: boolean;
  agentList: AgentType[];
  currentAgent?: AgentType;
  onSelectAgent: (agent: AgentType) => void;
  onClose: () => void;
};

const MobileAgents: React.FC<Props> = ({
  open,
  agentList,
  currentAgent,
  onSelectAgent,
  onClose,
}) => {
  return (
    <Drawer
      title="智能助理"
      placement="bottom"
      open={open}
      height="85%"
      className={styles.mobileAgents}
      onClose={onClose}
    >
      <div className={styles.agentListContent}>
        {agentList.map((agent, index) => {
          const agentItemClass = classNames(styles.agentItem, {
            [styles.active]: currentAgent?.id === agent.id,
          });
          return (
            <div
              key={agent.id}
              className={agentItemClass}
              onClick={() => {
                onSelectAgent(agent);
                onClose();
              }}
            >
              <div className={styles.agentTitleBar}>
                <IconFont
                  type={AGENT_ICONS[index % AGENT_ICONS.length]}
                  className={styles.avatar}
                />
                <div className={styles.agentName}>{agent.name}</div>
              </div>
              <div className={styles.agentDesc}>{agent.description}</div>
            </div>
          );
        })}
      </div>
    </Drawer>
  );
};

export default MobileAgents;

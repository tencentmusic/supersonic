import { PlusCircleOutlined } from '@ant-design/icons';
import { AgentType } from '../type';
import styles from './style.less';
import classNames from 'classnames';
import { message } from 'antd';
import IconFont from '@/components/IconFont';

const agents = [
  'icon-fukuanbaobiaochaxun',
  'icon-hangweifenxi1',
  'icon-xiaofeifenxi',
  'icon-renwuchaxun',
  'icon-liushuichaxun',
  'icon-baobiao',
  'icon-cangkuchaxun',
  'icon-xiaoshoushuju',
  'icon-tongji',
  'icon-shujutongji',
  'icon-mendiankanban',
];

type Props = {
  agentList: AgentType[];
  currentAgent?: AgentType;
  onSelectAgent: (agent: AgentType) => void;
};

const AgentList: React.FC<Props> = ({ agentList, currentAgent, onSelectAgent }) => {
  const onAddAgent = () => {
    message.info('正在开发中，敬请期待');
  };

  return (
    <div className={styles.agentList}>
      <div className={styles.header}>
        <div className={styles.headerTitle}>智能助理</div>
        <PlusCircleOutlined className={styles.plusIcon} onClick={onAddAgent} />
      </div>
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
              }}
            >
              {/* <img src={agents[index % agents.length]} alt="avatar" className={styles.avatar} /> */}
              <IconFont type={agents[index % agents.length]} className={styles.avatar} />
              <div className={styles.agentInfo}>
                <div className={styles.agentName}>{agent.name}</div>
                <div className={styles.agentDesc}>{agent.description}</div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default AgentList;

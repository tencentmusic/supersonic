import { DeleteOutlined, EditOutlined, PlusOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Input, Popconfirm, Switch } from 'antd';
import classNames from 'classnames';
import { useEffect, useState } from 'react';
import styles from './style.less';
import { AgentType } from './type';

const { Search } = Input;

type Props = {
  agents: AgentType[];
  currentAgent?: AgentType;
  loading: boolean;
  onSelectAgent: (agent: AgentType) => void;
  onDeleteAgent: (id: number) => void;
  onEditAgent: (agent?: AgentType) => void;
  onSaveAgent: (agent: AgentType, noTip?: boolean) => Promise<void>;
};

const AgentsSection: React.FC<Props> = ({
  agents,
  currentAgent,
  onSelectAgent,
  onDeleteAgent,
  onEditAgent,
  onSaveAgent,
}) => {
  // const [searchName, setSearchName] = useState('');
  const [showAgents, setShowAgents] = useState<AgentType[]>([]);

  useEffect(() => {
    setShowAgents(agents);
  }, [agents]);

  return (
    <div className={styles.agentsSection}>
      {/* <div className={styles.sectionTitle}>问答助理</div> */}
      <div className={styles.content}>
        <div className={styles.searchBar}>
          {/* <Search
            placeholder="请输入助理名称搜索"
            className={styles.searchControl}
            value={searchName}
            onChange={(e) => {
              setSearchName(e.target.value);
            }}
            onSearch={(value) => {
              setShowAgents(
                agents.filter((agent) =>
                  agent.name?.trim().toLowerCase().includes(value.trim().toLowerCase()),
                ),
              );
            }}
          /> */}
          <Button
            type="primary"
            onClick={() => {
              onEditAgent(undefined);
            }}
          >
            <PlusOutlined />
            新建助理
          </Button>
        </div>
        <div className={styles.agentsContainer}>
          {showAgents.map((agent) => {
            const agentItemClass = classNames(styles.agentItem, {
              [styles.agentActive]: agent.id === currentAgent?.id,
            });
            return (
              <div
                className={agentItemClass}
                key={agent.id}
                onClick={() => {
                  onSelectAgent(agent);
                }}
              >
                <UserOutlined className={styles.agentIcon} />
                <div className={styles.agentContent}>
                  <div className={styles.agentNameBar}>
                    <div className={styles.agentName}>{agent.name}</div>
                    <div className={styles.operateIcons}>
                      <EditOutlined
                        className={styles.operateIcon}
                        onClick={(e) => {
                          e.stopPropagation();
                          onEditAgent(agent);
                        }}
                      />
                      <Popconfirm
                        title="确定删除吗？"
                        onCancel={(e) => {
                          e?.stopPropagation();
                        }}
                        onConfirm={(e) => {
                          e?.stopPropagation();
                          onDeleteAgent(agent.id!);
                        }}
                      >
                        <DeleteOutlined
                          className={styles.operateIcon}
                          onClick={(e) => {
                            e.stopPropagation();
                          }}
                        />
                      </Popconfirm>
                    </div>
                  </div>
                  <div className={styles.bottomBar}>
                    <div className={styles.agentDescription} title={agent.description}>
                      {agent.description}
                    </div>
                    <div className={styles.toggleStatus}>
                      {agent.status === 0 ? (
                        '已禁用'
                      ) : (
                        <span className={styles.online}>已启用</span>
                      )}
                      <span
                        onClick={(e) => {
                          e.stopPropagation();
                        }}
                      >
                        <Switch
                          size="small"
                          defaultChecked={agent.status === 1}
                          onChange={(value) => {
                            onSaveAgent({ ...agent, status: value ? 1 : 0 }, true);
                          }}
                        />
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default AgentsSection;

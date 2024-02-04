import { DeleteOutlined, EditOutlined, PlusOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Popconfirm, Switch, Table } from 'antd';
import classNames from 'classnames';
import moment from 'moment';
import { useEffect, useState } from 'react';
import styles from './style.less';
import { AgentType } from './type';

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
  const [showAgents, setShowAgents] = useState<AgentType[]>([]);
  const [showType, setShowType] = useState(localStorage.getItem('AGENT_SHOW_TYPE') || 'list');

  useEffect(() => {
    setShowAgents(agents);
  }, [agents]);

  const columns = [
    {
      title: '助理名称',
      dataIndex: 'name',
      key: 'name',
      render: (value: string, agent: AgentType) => {
        return (
          <a
            onClick={() => {
              onSelectAgent(agent);
            }}
          >
            {value}
          </a>
        );
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number, agent: AgentType) => {
        return (
          <div className={styles.toggleStatus}>
            {status === 0 ? '已禁用' : <span className={styles.online}>已启用</span>}
            <span
              onClick={(e) => {
                e.stopPropagation();
              }}
            >
              <Switch
                size="small"
                defaultChecked={status === 1}
                onChange={(value) => {
                  onSaveAgent({ ...agent, status: value ? 1 : 0 }, true);
                }}
              />
            </span>
          </div>
        );
      },
    },
    {
      title: '更新人',
      dataIndex: 'updatedBy',
      key: 'updatedBy',
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (value: string) => {
        return moment(value).format('YYYY-MM-DD HH:mm:ss');
      },
    },
    {
      title: '操作',
      dataIndex: 'x',
      key: 'x',
      render: (_: any, agent: AgentType) => {
        return (
          <div className={styles.operateIcons}>
            <a
              onClick={() => {
                onSelectAgent(agent);
              }}
            >
              编辑
            </a>
            <Popconfirm
              title="确定删除吗？"
              onCancel={(e) => {
                e?.stopPropagation();
              }}
              onConfirm={() => {
                onDeleteAgent(agent.id!);
              }}
            >
              <a>删除</a>
            </Popconfirm>
          </div>
        );
      },
    },
  ];

  return (
    <div className={styles.agentsSection}>
      <div className={styles.content}>
        <div className={styles.searchBar}>
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
        {showType === 'list' ? (
          <Table columns={columns} dataSource={showAgents} />
        ) : (
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
        )}
      </div>
    </div>
  );
};

export default AgentsSection;

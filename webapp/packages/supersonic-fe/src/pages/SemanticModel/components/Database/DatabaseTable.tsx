import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm, Tag, Tooltip } from 'antd';
import { SyncOutlined, WarningOutlined, PlayCircleOutlined, PauseCircleOutlined, EditOutlined } from '@ant-design/icons';
import React, { useRef, useState, useEffect } from 'react';
import DatabaseSettingModal from './DatabaseSettingModal';
import SyncConfigWizard from './SyncConfigWizard';
import { ISemantic } from '../../data';
import { getDatabaseList, deleteDatabase } from '../../service';
import { listConnections, triggerSync, pauseConnection, resumeConnection } from '@/services/connection';
import type { ConnectionDO } from '@/services/connection';

import dayjs from 'dayjs';

type Props = {};

const CONNECTION_STATUS_MAP: Record<string, { color: string; text: string }> = {
  ACTIVE: { color: 'green', text: '同步中' },
  PAUSED: { color: 'default', text: '已暂停' },
  BROKEN: { color: 'red', text: '已中断' },
  DEPRECATED: { color: 'default', text: '已废弃' },
};

const DatabaseTable: React.FC<Props> = ({}) => {
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [databaseItem, setDatabaseItem] = useState<ISemantic.IDatabaseItem>();
  const [dataBaseList, setDataBaseList] = useState<any[]>([]);
  const [connections, setConnections] = useState<ConnectionDO[]>([]);
  const [syncWizardVisible, setSyncWizardVisible] = useState<boolean>(false);
  const [syncSourceDatabase, setSyncSourceDatabase] = useState<ISemantic.IDatabaseItem | null>(null);

  const actionRef = useRef<ActionType>();

  const queryDatabaseList = async () => {
    const { code, data, msg } = await getDatabaseList();
    if (code === 200) {
      setDataBaseList(data);
    } else {
      message.error(msg);
    }
  };

  const queryConnections = async () => {
    try {
      const res = await listConnections({ current: 1, pageSize: 100 });
      setConnections(res?.records || []);
    } catch (e) {
      // non-blocking
    }
  };

  useEffect(() => {
    queryDatabaseList();
    queryConnections();
  }, []);

  // Get connection info for a database
  const getConnectionInfo = (databaseId: number) => {
    const conn = connections.find(
      (c) => c.sourceDatabaseId === databaseId || c.destinationDatabaseId === databaseId,
    );
    if (!conn) return { connection: null, role: 'none' as const };

    const isSource = conn.sourceDatabaseId === databaseId;
    return {
      connection: conn,
      role: isSource ? 'source' as const : 'destination' as const
    };
  };

  const getSyncStatus = (databaseId: number) => {
    const { connection: conn, role } = getConnectionInfo(databaseId);
    if (!conn) return null;

    const statusInfo = CONNECTION_STATUS_MAP[conn.status || 'ACTIVE'];
    const hasSchemaChange = conn.schemaChangeStatus === 'BREAKING';

    if (role === 'source') {
      return (
        <Space size={4}>
          <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
          {hasSchemaChange && (
            <Tooltip title="检测到破坏性 Schema 变更">
              <WarningOutlined style={{ color: '#ff4d4f' }} />
            </Tooltip>
          )}
        </Space>
      );
    }
    return <Tag color="blue">作为目标库</Tag>;
  };

  // Handle sync operations
  const handleTriggerSync = async (connectionId: number) => {
    try {
      await triggerSync(connectionId);
      message.success('同步任务已触发');
    } catch (e: any) {
      message.error(e?.message || '触发同步失败');
    }
  };

  const handlePauseConnection = async (connectionId: number) => {
    try {
      await pauseConnection(connectionId);
      message.success('已暂停同步');
      queryConnections();
    } catch (e: any) {
      message.error(e?.message || '暂停失败');
    }
  };

  const handleResumeConnection = async (connectionId: number) => {
    try {
      await resumeConnection(connectionId);
      message.success('已恢复同步');
      queryConnections();
    } catch (e: any) {
      message.error(e?.message || '恢复失败');
    }
  };

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      width: 80,
    },
    {
      dataIndex: 'name',
      title: '连接名称',
    },

    {
      dataIndex: 'type',
      title: '类型',
      search: false,
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
      search: false,
    },

    {
      dataIndex: 'description',
      title: '描述',
      search: false,
    },

    {
      title: '同步状态',
      width: 120,
      search: false,
      render: (_, record) => getSyncStatus(record.id) || <Tag>未配置</Tag>,
    },

    {
      dataIndex: 'updatedAt',
      title: '更新时间',
      search: false,
      render: (value: any) => {
        return value && value !== '-' ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },

    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      width: 260,
      render: (_, record) => {
        if (!record.hasEditPermission) {
          return <></>;
        }
        const { connection, role } = getConnectionInfo(record.id);
        const isSource = role === 'source';
        const isPaused = connection?.status === 'PAUSED';
        const isActive = connection?.status === 'ACTIVE';

        return (
          <Space>
            <a
              key="dimensionEditBtn"
              onClick={() => {
                setDatabaseItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </a>

            {/* 未配置且不是目标库时，显示配置同步 */}
            {!connection && (
              <a
                key="syncConfigBtn"
                onClick={() => {
                  setSyncSourceDatabase(record);
                  setSyncWizardVisible(true);
                }}
              >
                <SyncOutlined /> 配置同步
              </a>
            )}

            {/* 已配置为源库时，显示同步操作 */}
            {isSource && connection && (
              <>
                <a
                  key="editSyncBtn"
                  onClick={() => {
                    setSyncSourceDatabase(record);
                    setSyncWizardVisible(true);
                  }}
                >
                  <EditOutlined /> 编辑同步
                </a>
                <Tooltip title="立即执行一次同步">
                  <a
                    key="triggerSyncBtn"
                    onClick={() => handleTriggerSync(connection.id!)}
                  >
                    <PlayCircleOutlined /> 同步
                  </a>
                </Tooltip>
                {isActive && (
                  <Popconfirm
                    title="确认暂停同步？"
                    okText="是"
                    cancelText="否"
                    onConfirm={() => handlePauseConnection(connection.id!)}
                  >
                    <a key="pauseBtn">
                      <PauseCircleOutlined /> 暂停
                    </a>
                  </Popconfirm>
                )}
                {isPaused && (
                  <a
                    key="resumeBtn"
                    onClick={() => handleResumeConnection(connection.id!)}
                  >
                    <PlayCircleOutlined style={{ color: '#52c41a' }} /> 恢复
                  </a>
                )}
              </>
            )}

            {/* 作为目标库时不显示同步相关操作 */}

            <Popconfirm
              title="确认删除？"
              okText="是"
              cancelText="否"
              onConfirm={async () => {
                const { code, msg } = await deleteDatabase(record.id);
                if (code === 200) {
                  setDatabaseItem(undefined);
                  queryDatabaseList();
                  queryConnections();
                } else {
                  message.error(msg);
                }
              }}
            >
              <a
                key="dimensionDeleteEditBtn"
                onClick={() => {
                  setDatabaseItem(record);
                }}
              >
                删除
              </a>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ margin: 20 }}>
      <ProTable
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        dataSource={dataBaseList}
        search={false}
        tableAlertRender={() => {
          return false;
        }}
        size="small"
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setDatabaseItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建数据库连接
          </Button>,
        ]}
      />
      {createModalVisible && (
        <DatabaseSettingModal
          open={createModalVisible}
          databaseItem={databaseItem}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryDatabaseList();
            queryConnections();
          }}
        />
      )}
      {syncWizardVisible && syncSourceDatabase && (
        <SyncConfigWizard
          visible={syncWizardVisible}
          sourceDatabase={syncSourceDatabase}
          onClose={() => {
            setSyncWizardVisible(false);
            setSyncSourceDatabase(null);
          }}
          onSuccess={() => {
            queryConnections();
          }}
        />
      )}
    </div>
  );
};
export default DatabaseTable;
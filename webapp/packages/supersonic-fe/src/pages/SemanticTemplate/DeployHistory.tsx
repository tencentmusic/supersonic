import React, { useEffect, useState } from 'react';
import { Drawer, Table, Tag, Button, Space, message, Modal, Descriptions } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import moment from 'moment';
import { useModel } from '@umijs/max';
import { getDeploymentHistory, getAllDeploymentHistory, cancelDeployment, SemanticDeployment } from '@/services/semanticTemplate';

interface DeployHistoryProps {
  visible: boolean;
  onClose: () => void;
}

const statusConfig: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '待执行' },
  RUNNING: { color: 'processing', text: '执行中' },
  SUCCESS: { color: 'success', text: '成功' },
  FAILED: { color: 'error', text: '失败' },
  CANCELLED: { color: 'warning', text: '已取消' },
};

const DeployHistory: React.FC<DeployHistoryProps> = ({ visible, onClose }) => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  // 管理员可以查看所有租户的部署历史
  const isSaasAdmin = currentUser?.superAdmin || currentUser?.isAdmin === 1;

  const [deployments, setDeployments] = useState<SemanticDeployment[]>([]);
  const [loading, setLoading] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [selectedDeployment, setSelectedDeployment] = useState<SemanticDeployment | null>(null);

  useEffect(() => {
    if (visible) {
      loadHistory();
    }
  }, [visible]);

  const loadHistory = async () => {
    setLoading(true);
    try {
      // 管理员使用 getAllDeploymentHistory 查看所有租户的部署历史
      // 普通用户使用 getDeploymentHistory 只看本租户的部署历史
      const res: any = isSaasAdmin
        ? await getAllDeploymentHistory()
        : await getDeploymentHistory();
      // API response is wrapped in { code, data, msg } format
      if (res?.code === 200 && res?.data) {
        setDeployments(res.data);
      } else {
        setDeployments([]);
      }
    } catch (error) {
      message.error('加载部署历史失败');
      setDeployments([]);
    } finally {
      setLoading(false);
    }
  };

  const showDetail = (record: SemanticDeployment) => {
    setSelectedDeployment(record);
    setDetailVisible(true);
  };

  const handleCancel = async (record: SemanticDeployment) => {
    Modal.confirm({
      title: '确认取消部署',
      content: `确定要取消部署「${record.templateName}」吗？`,
      onOk: async () => {
        try {
          const res: any = await cancelDeployment(record.id);
          if (res?.code === 200) {
            message.success('已取消部署');
            loadHistory();
          } else {
            message.error(res?.msg || '取消部署失败');
          }
        } catch (error) {
          message.error('取消部署失败');
        }
      },
    });
  };

  const columns: ColumnsType<SemanticDeployment> = [
    {
      title: '模板',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 150,
    },
    // 管理员可以看到租户ID列
    ...(isSaasAdmin
      ? [
          {
            title: '租户ID',
            dataIndex: 'tenantId',
            key: 'tenantId',
            width: 80,
          },
        ]
      : []),
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={statusConfig[status]?.color || 'default'}>
          {statusConfig[status]?.text || status}
        </Tag>
      ),
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 160,
      render: (value: string) => (value ? moment(value).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 160,
      render: (value: string) => (value ? moment(value).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '创建人',
      dataIndex: 'createdBy',
      key: 'createdBy',
      width: 100,
    },
    {
      title: '操作',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => showDetail(record)}>
            详情
          </Button>
          {(record.status === 'PENDING' || record.status === 'RUNNING') && (
            <Button type="link" size="small" danger onClick={() => handleCancel(record)}>
              取消
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <Drawer
        title="部署历史"
        width={900}
        open={visible}
        onClose={onClose}
      >
        <Table
          dataSource={deployments}
          columns={columns}
          rowKey="id"
          size="small"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Drawer>

      <Modal
        title="部署详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={<Button onClick={() => setDetailVisible(false)}>关闭</Button>}
        width={700}
      >
        {selectedDeployment && (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="模板">{selectedDeployment.templateName}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={statusConfig[selectedDeployment.status]?.color}>
                {statusConfig[selectedDeployment.status]?.text}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="开始时间">
              {selectedDeployment.startTime
                ? moment(selectedDeployment.startTime).format('YYYY-MM-DD HH:mm:ss')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="结束时间">
              {selectedDeployment.endTime
                ? moment(selectedDeployment.endTime).format('YYYY-MM-DD HH:mm:ss')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建人">{selectedDeployment.createdBy}</Descriptions.Item>
            {selectedDeployment.status === 'SUCCESS' && selectedDeployment.resultDetail && (
              <>
                <Descriptions.Item label="主题域">
                  {selectedDeployment.resultDetail.domainName} (ID:{' '}
                  {selectedDeployment.resultDetail.domainId})
                </Descriptions.Item>
                <Descriptions.Item label="模型">
                  {selectedDeployment.resultDetail.models?.map((m) => (
                    <Tag key={m.id}>{m.name}</Tag>
                  ))}
                </Descriptions.Item>
                <Descriptions.Item label="数据集">
                  {selectedDeployment.resultDetail.dataSetName} (ID:{' '}
                  {selectedDeployment.resultDetail.dataSetId})
                </Descriptions.Item>
                {selectedDeployment.resultDetail.agentConfig && (
                  <Descriptions.Item label="Agent配置">
                    <Space direction="vertical" size="small">
                      <div>
                        <strong>名称:</strong> {selectedDeployment.resultDetail.agentConfig.name}
                      </div>
                      <div>
                        <strong>描述:</strong>{' '}
                        {selectedDeployment.resultDetail.agentConfig.description || '-'}
                      </div>
                      {selectedDeployment.resultDetail.agentConfig.agentId ? (
                        <Tag color="green">
                          Agent已创建 (ID: {selectedDeployment.resultDetail.agentConfig.agentId})
                        </Tag>
                      ) : (
                        <Tag color="orange">
                          Agent未自动创建，请通过问答模块手动创建
                        </Tag>
                      )}
                    </Space>
                  </Descriptions.Item>
                )}
              </>
            )}
            {(selectedDeployment.status === 'FAILED' || selectedDeployment.status === 'CANCELLED') && selectedDeployment.errorMessage && (
              <Descriptions.Item label="错误信息">
                <span style={{ color: '#ff4d4f' }}>{selectedDeployment.errorMessage}</span>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>
    </>
  );
};

export default DeployHistory;

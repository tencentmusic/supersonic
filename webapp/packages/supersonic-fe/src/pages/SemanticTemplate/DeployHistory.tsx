import React, { useEffect, useState, useMemo } from 'react';
import { Drawer, Table, Tag, Button, Space, message, Modal, Descriptions, Select } from 'antd';
import { SwapOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import moment from 'moment';
import { useModel } from '@umijs/max';
import { getDeploymentHistory, getAllDeploymentHistory, cancelDeployment, SemanticDeployment } from '@/services/semanticTemplate';
import ConfigDiff from './ConfigDiff';

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
  const isSaasAdmin = currentUser?.superAdmin || currentUser?.isAdmin === 1;

  const [deployments, setDeployments] = useState<SemanticDeployment[]>([]);
  const [loading, setLoading] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [selectedDeployment, setSelectedDeployment] = useState<SemanticDeployment | null>(null);
  // Compare modal state
  const [compareVisible, setCompareVisible] = useState(false);
  const [compareTarget, setCompareTarget] = useState<SemanticDeployment | null>(null);
  const [compareBaseId, setCompareBaseId] = useState<number | undefined>(undefined);

  useEffect(() => {
    if (visible) {
      loadHistory();
    }
  }, [visible]);

  const loadHistory = async () => {
    setLoading(true);
    try {
      const res: any = isSaasAdmin
        ? await getAllDeploymentHistory()
        : await getDeploymentHistory();
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

  /**
   * Find the previous deployment of the same template for default comparison baseline.
   */
  const findPreviousDeployment = (record: SemanticDeployment): SemanticDeployment | undefined => {
    const sameTemplate = deployments
      .filter((d) => d.templateId === record.templateId && d.id !== record.id)
      .sort((a, b) => (b.templateVersion || 0) - (a.templateVersion || 0));
    // Find the most recent deployment with a lower version
    return sameTemplate.find(
      (d) => (d.templateVersion || 0) < (record.templateVersion || 0),
    );
  };

  /**
   * Get all deployments of the same template that can serve as comparison baseline.
   */
  const getBaselineOptions = (record: SemanticDeployment) => {
    return deployments
      .filter(
        (d) =>
          d.templateId === record.templateId &&
          d.id !== record.id &&
          d.templateConfigSnapshot != null,
      )
      .sort((a, b) => (b.templateVersion || 0) - (a.templateVersion || 0));
  };

  const handleCompare = (record: SemanticDeployment) => {
    const prev = findPreviousDeployment(record);
    setCompareTarget(record);
    setCompareBaseId(prev?.id);
    setCompareVisible(true);
  };

  const compareBase = useMemo(() => {
    if (!compareBaseId) return undefined;
    return deployments.find((d) => d.id === compareBaseId);
  }, [compareBaseId, deployments]);

  const canCompare = (record: SemanticDeployment): boolean => {
    if (!record.templateConfigSnapshot) return false;
    // Must have at least one other deployment with a snapshot for the same template
    return deployments.some(
      (d) =>
        d.templateId === record.templateId &&
        d.id !== record.id &&
        d.templateConfigSnapshot != null,
    );
  };

  const columns: ColumnsType<SemanticDeployment> = [
    {
      title: '模板',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 150,
    },
    {
      title: '版本',
      dataIndex: 'templateVersion',
      key: 'templateVersion',
      width: 70,
      render: (v: number | undefined) => (v != null ? <Tag>V{v}</Tag> : '-'),
    },
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
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => showDetail(record)}>
            详情
          </Button>
          {canCompare(record) && (
            <Button
              type="link"
              size="small"
              icon={<SwapOutlined />}
              onClick={() => handleCompare(record)}
            >
              对比
            </Button>
          )}
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
        width={1000}
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

      {/* Detail Modal */}
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
            {selectedDeployment.templateVersion != null && (
              <Descriptions.Item label="模板版本">
                <Tag>V{selectedDeployment.templateVersion}</Tag>
              </Descriptions.Item>
            )}
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
            {selectedDeployment.templateConfigSnapshot && (
              <Descriptions.Item label="配置快照">
                <Button
                  type="link"
                  size="small"
                  onClick={() => {
                    Modal.info({
                      title: `配置快照 — V${selectedDeployment.templateVersion || '?'}`,
                      width: 720,
                      content: (
                        <pre
                          style={{
                            maxHeight: 480,
                            overflow: 'auto',
                            fontSize: 12,
                            background: '#f5f5f5',
                            padding: 12,
                            borderRadius: 4,
                          }}
                        >
                          {JSON.stringify(selectedDeployment.templateConfigSnapshot, null, 2)}
                        </pre>
                      ),
                    });
                  }}
                >
                  查看 JSON
                </Button>
              </Descriptions.Item>
            )}
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

      {/* Compare Modal */}
      <Modal
        title={
          compareTarget
            ? `配置变更对比 — ${compareTarget.templateName}`
            : '配置变更对比'
        }
        open={compareVisible}
        onCancel={() => setCompareVisible(false)}
        footer={<Button onClick={() => setCompareVisible(false)}>关闭</Button>}
        width={960}
      >
        {compareTarget && (
          <>
            <div style={{ marginBottom: 16 }}>
              <Space>
                <span>对比基线：</span>
                <Select
                  value={compareBaseId}
                  onChange={setCompareBaseId}
                  style={{ width: 320 }}
                  placeholder="选择基线版本"
                >
                  {getBaselineOptions(compareTarget).map((d) => (
                    <Select.Option key={d.id} value={d.id}>
                      V{d.templateVersion || '?'} — {d.startTime ? moment(d.startTime).format('YYYY-MM-DD HH:mm') : '未知时间'} ({statusConfig[d.status]?.text || d.status})
                    </Select.Option>
                  ))}
                </Select>
                <SwapOutlined />
                <Tag>
                  V{compareTarget.templateVersion || '?'} (当前)
                </Tag>
              </Space>
            </div>
            {compareBase ? (
              <ConfigDiff
                oldConfig={compareBase.templateConfigSnapshot}
                newConfig={compareTarget.templateConfigSnapshot}
                oldVersion={compareBase.templateVersion}
                newVersion={compareTarget.templateVersion}
                oldTime={
                  compareBase.startTime
                    ? moment(compareBase.startTime).format('YYYY-MM-DD HH:mm')
                    : undefined
                }
                newTime={
                  compareTarget.startTime
                    ? moment(compareTarget.startTime).format('YYYY-MM-DD HH:mm')
                    : undefined
                }
              />
            ) : (
              <div style={{ color: '#999', textAlign: 'center', padding: 24 }}>
                请选择基线版本进行对比
              </div>
            )}
          </>
        )}
      </Modal>
    </>
  );
};

export default DeployHistory;

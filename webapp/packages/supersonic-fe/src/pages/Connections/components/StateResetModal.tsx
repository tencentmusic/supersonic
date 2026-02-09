import React, { useEffect, useState } from 'react';
import {
  Modal,
  Table,
  Checkbox,
  Button,
  Space,
  Alert,
  Typography,
  Empty,
  message,
  Tag,
} from 'antd';
import {
  WarningOutlined,
  ReloadOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import { ConnectionDO, resetState } from '@/services/connection';

interface StateResetModalProps {
  visible: boolean;
  connection?: ConnectionDO;
  onClose: () => void;
  onSuccess: () => void;
}

interface StreamState {
  streamName: string;
  watermark?: string;
  selected: boolean;
}

const StateResetModal: React.FC<StateResetModalProps> = ({
  visible,
  connection,
  onClose,
  onSuccess,
}) => {
  const [streamStates, setStreamStates] = useState<StreamState[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectAll, setSelectAll] = useState(false);

  useEffect(() => {
    if (visible && connection) {
      parseConnectionState();
    }
  }, [visible, connection]);

  const parseConnectionState = () => {
    if (!connection) return;

    // Parse state JSON if available
    let stateMap: Record<string, string> = {};
    if (connection.state) {
      try {
        stateMap = JSON.parse(connection.state);
      } catch {
        stateMap = {};
      }
    }

    // Parse configured catalog to get stream names
    let streamNames: string[] = [];
    if (connection.configuredCatalog) {
      try {
        const catalog = JSON.parse(connection.configuredCatalog);
        streamNames =
          catalog.streams
            ?.filter((s: any) => s.selected !== false)
            .map((s: any) => s.streamName) || [];
      } catch {
        streamNames = [];
      }
    }

    // If no configured catalog, try discovered catalog
    if (streamNames.length === 0 && connection.discoveredCatalog) {
      try {
        const discovered = JSON.parse(connection.discoveredCatalog);
        streamNames = discovered.tables?.map((t: any) => t.tableName) || [];
      } catch {
        streamNames = [];
      }
    }

    // Build stream states
    const states: StreamState[] = streamNames.map((name) => ({
      streamName: name,
      watermark: stateMap[name],
      selected: false,
    }));

    // Also add any streams from state that aren't in catalog
    Object.keys(stateMap).forEach((name) => {
      if (!streamNames.includes(name)) {
        states.push({
          streamName: name,
          watermark: stateMap[name],
          selected: false,
        });
      }
    });

    setStreamStates(states);
    setSelectAll(false);
  };

  const handleSelectAll = (checked: boolean) => {
    setSelectAll(checked);
    setStreamStates((prev) => prev.map((s) => ({ ...s, selected: checked })));
  };

  const handleSelectStream = (streamName: string, checked: boolean) => {
    setStreamStates((prev) => {
      const updated = prev.map((s) =>
        s.streamName === streamName ? { ...s, selected: checked } : s,
      );
      const allSelected = updated.every((s) => s.selected);
      setSelectAll(allSelected);
      return updated;
    });
  };

  const handleResetSelected = async () => {
    const selectedStreams = streamStates.filter((s) => s.selected).map((s) => s.streamName);
    if (selectedStreams.length === 0) {
      message.warning('请至少选择一个数据流');
      return;
    }

    Modal.confirm({
      title: '确认重置状态',
      icon: <WarningOutlined />,
      content: (
        <div>
          <p>确定要重置以下 {selectedStreams.length} 个数据流的同步状态吗？</p>
          <p style={{ color: '#ff4d4f' }}>
            重置后，下次同步将从头开始（全量模式）或从初始位置开始（增量模式）。
          </p>
          <ul>
            {selectedStreams.slice(0, 5).map((name) => (
              <li key={name}>{name}</li>
            ))}
            {selectedStreams.length > 5 && <li>...等 {selectedStreams.length - 5} 个</li>}
          </ul>
        </div>
      ),
      okText: '确认重置',
      okType: 'danger',
      onOk: async () => {
        await executeReset(selectedStreams);
      },
    });
  };

  const handleResetAll = async () => {
    Modal.confirm({
      title: '确认重置所有状态',
      icon: <WarningOutlined />,
      content: (
        <div>
          <p style={{ color: '#ff4d4f', fontWeight: 'bold' }}>
            此操作将重置所有数据流的同步状态！
          </p>
          <p>下次同步将从头开始，可能导致数据重复同步。确定要继续吗？</p>
        </div>
      ),
      okText: '全部重置',
      okType: 'danger',
      onOk: async () => {
        await executeReset(undefined); // undefined means reset all
      },
    });
  };

  const executeReset = async (streams?: string[]) => {
    if (!connection?.id) return;

    setLoading(true);
    try {
      await resetState(connection.id, streams);
      message.success('状态重置成功');
      onSuccess();
      onClose();
    } catch (e: any) {
      message.error(e.message || '状态重置失败');
    } finally {
      setLoading(false);
    }
  };

  const selectedCount = streamStates.filter((s) => s.selected).length;
  const hasWatermarks = streamStates.some((s) => s.watermark);

  const columns = [
    {
      title: (
        <Checkbox
          checked={selectAll}
          indeterminate={selectedCount > 0 && selectedCount < streamStates.length}
          onChange={(e) => handleSelectAll(e.target.checked)}
        />
      ),
      width: 50,
      render: (_: any, record: StreamState) => (
        <Checkbox
          checked={record.selected}
          onChange={(e) => handleSelectStream(record.streamName, e.target.checked)}
        />
      ),
    },
    {
      title: '数据流名称',
      dataIndex: 'streamName',
      ellipsis: true,
    },
    {
      title: '当前水位线',
      dataIndex: 'watermark',
      width: 200,
      render: (val: string) =>
        val ? (
          <Space>
            <ClockCircleOutlined />
            <Typography.Text code>{val}</Typography.Text>
          </Space>
        ) : (
          <Tag>无状态</Tag>
        ),
    },
  ];

  return (
    <Modal
      title={
        <Space>
          <ReloadOutlined />
          <span>重置同步状态</span>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      width={700}
      footer={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button
            icon={<DeleteOutlined />}
            danger
            onClick={handleResetAll}
            loading={loading}
          >
            全部重置
          </Button>
          <Button
            type="primary"
            danger
            onClick={handleResetSelected}
            disabled={selectedCount === 0}
            loading={loading}
          >
            重置选中 ({selectedCount})
          </Button>
        </Space>
      }
    >
      <Alert
        message="什么是同步状态？"
        description={
          <div>
            <p style={{ margin: 0 }}>
              同步状态记录了每个数据流的同步进度（水位线）。重置状态后：
            </p>
            <ul style={{ marginBottom: 0 }}>
              <li>
                <strong>全量模式</strong>：下次同步将重新同步所有数据
              </li>
              <li>
                <strong>增量模式</strong>：下次同步将从初始位置开始，可能导致数据重复
              </li>
            </ul>
          </div>
        }
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      {streamStates.length === 0 ? (
        <Empty description="暂无数据流配置" />
      ) : (
        <>
          {hasWatermarks && (
            <Alert
              message={`检测到 ${streamStates.filter((s) => s.watermark).length} 个数据流有同步状态`}
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}
          <Table
            dataSource={streamStates}
            columns={columns}
            rowKey="streamName"
            size="small"
            pagination={streamStates.length > 10 ? { pageSize: 10 } : false}
          />
        </>
      )}
    </Modal>
  );
};

export default StateResetModal;

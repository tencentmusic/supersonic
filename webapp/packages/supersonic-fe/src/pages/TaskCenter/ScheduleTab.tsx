import React, { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import { Button, Table, Tag, Switch, Space, Popconfirm, message, Tooltip, Empty } from 'antd';
import {
  PlusOutlined,
  PlayCircleOutlined,
  UnorderedListOutlined,
  DeleteOutlined,
  EditOutlined,
  SendOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { history } from 'umi';
import ScheduleForm from '../ReportSchedule/components/ScheduleForm';
import ExecutionList from '../ReportSchedule/components/ExecutionList';
import {
  getScheduleList,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  pauseSchedule,
  resumeSchedule,
  triggerSchedule,
  getValidDataSetList,
} from '@/services/reportSchedule';
import type { ReportSchedule } from '@/services/reportSchedule';
import { getConfigList, DeliveryConfig, DELIVERY_TYPE_MAP } from '@/services/deliveryConfig';
import { MSG } from '@/common/messages';
import styles from './style.less';

const ScheduleTab: React.FC = () => {
  const [data, setData] = useState<ReportSchedule[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [formVisible, setFormVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<ReportSchedule | undefined>();
  const [executionDrawer, setExecutionDrawer] = useState<{
    visible: boolean;
    scheduleId?: number;
    name?: string;
  }>({ visible: false });
  const [deliveryConfigMap, setDeliveryConfigMap] = useState<Record<number, DeliveryConfig>>({});
  const [datasetNameMap, setDatasetNameMap] = useState<Record<number, string>>({});

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res = await getScheduleList({ current, pageSize });
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
    } catch (error) {
      message.error('加载调度任务失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchDeliveryConfigs = async () => {
    try {
      const res = await getConfigList({ pageNum: 1, pageSize: 100 });
      const pageData = res?.data ?? res;
      const configMap: Record<number, DeliveryConfig> = {};
      (pageData?.records || []).forEach((config: DeliveryConfig) => {
        configMap[config.id] = config;
      });
      setDeliveryConfigMap(configMap);
    } catch (error) {
      console.error('Failed to load delivery configs', error);
    }
  };

  const fetchDatasetNames = async () => {
    try {
      const res = await getValidDataSetList();
      const list = res?.data ?? res;
      const map: Record<number, string> = {};
      (Array.isArray(list) ? list : []).forEach((d: { id: number; name: string }) => {
        map[d.id] = d.name;
      });
      setDatasetNameMap(map);
    } catch (error) {
      console.error('Failed to load dataset names', error);
    }
  };

  useEffect(() => {
    fetchData();
    fetchDeliveryConfigs();
    fetchDatasetNames();
  }, []);

  const handleCreate = () => {
    setEditRecord(undefined);
    setFormVisible(true);
  };

  const handleEdit = (record: ReportSchedule) => {
    setEditRecord(record);
    setFormVisible(true);
  };

  const handleFormSubmit = async (values: Partial<ReportSchedule>) => {
    if (editRecord?.id) {
      await updateSchedule(editRecord.id, values);
      message.success(MSG.UPDATE_SUCCESS);
    } else {
      await createSchedule(values);
      message.success(MSG.CREATE_SUCCESS);
    }
    setFormVisible(false);
    fetchData(pagination.current, pagination.pageSize);
    fetchDeliveryConfigs();
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteSchedule(id);
      message.success(MSG.DELETE_SUCCESS);
      fetchData(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleToggle = async (record: ReportSchedule, checked: boolean) => {
    try {
      if (checked) { await resumeSchedule(record.id); } else { await pauseSchedule(record.id); }
      fetchData(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleTrigger = async (id: number) => {
    try {
      await triggerSchedule(id);
      message.success('已触发执行');
    } catch (error) {
      message.error('触发失败');
    }
  };

  const columns = [
    { title: '任务名称', dataIndex: 'name', width: 200 },
    {
      title: '关联数据集',
      dataIndex: 'datasetId',
      width: 180,
      render: (id: number) =>
        datasetNameMap[id] != null ? `${datasetNameMap[id]} (${id})` : id ?? '-',
    },
    {
      title: 'Cron 表达式',
      dataIndex: 'cronExpression',
      width: 150,
      render: (cron: string) => (
        <Tooltip title={cron}>
          <code>{cron}</code>
        </Tooltip>
      ),
    },
    {
      title: '输出格式',
      dataIndex: 'outputFormat',
      width: 100,
      render: (fmt: string) => <Tag>{fmt}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (enabled: boolean, record: ReportSchedule) => (
        <Switch
          checked={enabled}
          size="small"
          onChange={(checked) => handleToggle(record, checked)}
        />
      ),
    },
    {
      title: '上次执行',
      dataIndex: 'lastExecutionTime',
      width: 180,
      render: (val: string) => val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '推送渠道',
      dataIndex: 'deliveryConfigIds',
      width: 150,
      render: (ids: string) => {
        if (!ids) return <span style={{ color: '#999' }}>-</span>;
        const idList = ids
          .split(',')
          .map((id) => parseInt(id.trim(), 10))
          .filter((id) => !isNaN(id));
        if (idList.length === 0) return <span style={{ color: '#999' }}>-</span>;
        return (
          <Space size={2} wrap>
            {idList.map((id) => {
              const config = deliveryConfigMap[id];
              if (!config) return <Tag key={id}>{id}</Tag>;
              const typeInfo = DELIVERY_TYPE_MAP[config.deliveryType];
              return (
                <Tooltip key={id} title={config.name}>
                  <Tag color={typeInfo?.color} icon={<SendOutlined />}>
                    {typeInfo?.text || config.deliveryType}
                  </Tag>
                </Tooltip>
              );
            })}
          </Space>
        );
      },
    },
    {
      title: '操作',
      width: 160,
      render: (_: any, record: ReportSchedule) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          </Tooltip>
          <Tooltip title="立即执行">
            <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handleTrigger(record.id)} />
          </Tooltip>
          <Tooltip title="执行记录">
            <Button
              type="link"
              size="small"
              icon={<UnorderedListOutlined />}
              onClick={() => setExecutionDrawer({ visible: true, scheduleId: record.id, name: record.name })}
            />
          </Tooltip>
          <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.id)} okText="确认" cancelText="取消">
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className={styles.sectionHeader}>
        <div>
          <div className={styles.sectionTitle}>定时报表</div>
        </div>
        <Space>
          <Button
            icon={<SettingOutlined />}
            onClick={() => history.push('/delivery-config')}
          >
            推送配置
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            创建调度
          </Button>
        </Space>
      </div>
      <div className={styles.tableShell}>
        <Table
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: (
              <Empty description="暂无调度任务">
                <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                  创建调度
                </Button>
              </Empty>
            ),
          }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => fetchData(page, size),
          }}
        />
      </div>
      <ScheduleForm
        visible={formVisible}
        record={editRecord}
        onCancel={() => setFormVisible(false)}
        onSubmit={handleFormSubmit}
      />
      <ExecutionList
        visible={executionDrawer.visible}
        scheduleId={executionDrawer.scheduleId}
        scheduleName={executionDrawer.name}
        onClose={() => setExecutionDrawer({ visible: false })}
      />
    </div>
  );
};

export default ScheduleTab;

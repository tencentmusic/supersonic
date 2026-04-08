import React, { useEffect, useState, useCallback } from 'react';
import { Table, Tag, Space, Button, message, Empty, Tooltip } from 'antd';
import { StarFilled, StarOutlined, SendOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import FilterBar from './components/FilterBar';
import ReportDetailDrawer from './components/ReportDetailDrawer';
import ReportHistoryDrawer from './components/ReportHistoryDrawer';
import ScheduleForm from '@/pages/ReportSchedule/components/ScheduleForm';
import {
  getFixedReports,
  subscribe as apiSubscribe,
  unsubscribe as apiUnsubscribe,
} from '@/services/fixedReport';
import type { FixedReport } from '@/services/fixedReport';
import { createSchedule } from '@/services/reportSchedule';
import type { ReportSchedule } from '@/services/reportSchedule';
import { DELIVERY_TYPE_MAP } from '@/services/deliveryConfig';
import styles from './style.less';

const STATUS_CONFIG: Record<string, { color: string; text: string }> = {
  AVAILABLE: { color: 'green', text: '可查看' },
  NO_RESULT: { color: 'default', text: '暂无结果' },
  EXPIRED: { color: 'orange', text: '结果过期' },
  RECENTLY_FAILED: { color: 'red', text: '最近失败' },
  NO_DELIVERY: { color: 'volcano', text: '未配置投递' },
  PARTIAL_CHANNEL_ERROR: { color: 'orange', text: '部分渠道异常' },
};

const ReportsPage: React.FC = () => {
  const [data, setData] = useState<FixedReport[]>([]);
  const [loading, setLoading] = useState(false);

  // Filters
  const [keyword, setKeyword] = useState('');
  const [domainName, setDomainName] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [viewFilter, setViewFilter] = useState('subscribed');

  // All domain names (from unfiltered data) for the domain filter dropdown
  const [allDomains, setAllDomains] = useState<string[]>([]);

  // Detail drawer
  const [selectedReport, setSelectedReport] = useState<FixedReport | undefined>();
  const [drawerVisible, setDrawerVisible] = useState(false);

  // Schedule form
  const [scheduleFormVisible, setScheduleFormVisible] = useState(false);
  const [scheduleDatasetId, setScheduleDatasetId] = useState<number | undefined>();

  // History drawer
  const [historyDrawer, setHistoryDrawer] = useState<{
    visible: boolean;
    datasetId?: number;
    reportName?: string;
  }>({ visible: false });

  const syncSelectedReport = useCallback((updater: (report: FixedReport) => FixedReport | undefined) => {
    setSelectedReport((current) => (current ? updater(current) : current));
  }, []);

  const fetchDomainOptions = useCallback(async () => {
    try {
      const res: any = await getFixedReports();
      const list = (res?.code === 200 && res?.data) ? res.data : res;
      const items = Array.isArray(list) ? list : [];
      setAllDomains(
        [...new Set(items.map((r: FixedReport) => r.domainName).filter(Boolean))] as string[],
      );
    } catch {
      // non-blocking: keep the rest of the page usable even if the filter options fail to load
    }
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res: any = await getFixedReports({
        keyword: keyword || undefined,
        domainName: domainName || undefined,
        status: statusFilter || undefined,
        view: viewFilter || undefined,
      });
      const list = (res?.code === 200 && res?.data) ? res.data : res;
      const items = Array.isArray(list) ? list : [];
      setData(items);
    } catch {
      message.error('加载固定报表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  }, [keyword, domainName, statusFilter, viewFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    fetchDomainOptions();
  }, [fetchDomainOptions]);

  const domainOptions = allDomains;

  const handleSubscribe = async (datasetId: number) => {
    try {
      await apiSubscribe(datasetId);
      setData((current) =>
        current.map((item) =>
          item.datasetId === datasetId ? { ...item, subscribed: true } : item,
        ),
      );
      syncSelectedReport((report) =>
        report.datasetId === datasetId ? { ...report, subscribed: true } : report,
      );
      message.success('已订阅');
      fetchData();
    } catch {
      message.error('订阅失败');
    }
  };

  const handleUnsubscribe = async (datasetId: number) => {
    try {
      await apiUnsubscribe(datasetId);
      setData((current) =>
        current.filter((item) => !(viewFilter === 'subscribed' && item.datasetId === datasetId))
          .map((item) =>
            item.datasetId === datasetId ? { ...item, subscribed: false } : item,
          ),
      );
      if (viewFilter === 'subscribed' && selectedReport?.datasetId === datasetId) {
        setDrawerVisible(false);
      }
      syncSelectedReport((report) =>
        report.datasetId === datasetId ? { ...report, subscribed: false } : report,
      );
      message.success('已取消订阅');
      fetchData();
    } catch {
      message.error('取消订阅失败');
    }
  };

  const handleCreateSchedule = (datasetId: number) => {
    setScheduleDatasetId(datasetId);
    setScheduleFormVisible(true);
  };

  const handleScheduleSubmit = async (values: Partial<ReportSchedule>) => {
    try {
      const res: any = await createSchedule(values);
      if (res?.code === 200) {
        message.success('创建定时任务成功');
        setScheduleFormVisible(false);
        setScheduleDatasetId(undefined);
        fetchData();
      } else {
        message.error(res?.msg || '创建定时任务失败');
      }
    } catch {
      message.error('创建定时任务失败');
    }
  };

  const openDetail = (record: FixedReport) => {
    setSelectedReport(record);
    setDrawerVisible(true);
  };

  const openHistory = (record: FixedReport) => {
    setHistoryDrawer({ visible: true, datasetId: record.datasetId, reportName: record.reportName });
  };

  const columns = [
    {
      title: '报表名称',
      dataIndex: 'reportName',
      width: 200,
      ellipsis: true,
      render: (val: string, record: FixedReport) => (
        <a
          className={styles.reportName}
          onClick={(e) => {
            e.stopPropagation();
            openDetail(record);
          }}
        >
          {val}
        </a>
      ),
    },
    {
      title: '口径摘要',
      dataIndex: 'description',
      width: 200,
      ellipsis: true,
      render: (val?: string) => val || '—',
    },
    {
      title: '最新结果',
      dataIndex: 'latestResultTime',
      width: 170,
      render: (val?: string) =>
        val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '—',
    },
    {
      title: '状态',
      dataIndex: 'consumptionStatus',
      width: 120,
      render: (status: string) => {
        const info = STATUS_CONFIG[status] || STATUS_CONFIG.NO_RESULT;
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '投递渠道',
      dataIndex: 'deliveryChannels',
      width: 150,
      render: (channels: FixedReport['deliveryChannels']) => {
        if (!channels || channels.length === 0) {
          return <span style={{ color: 'rgba(0,10,36,0.35)' }}>未配置</span>;
        }
        return (
          <Space size={4} wrap>
            {channels.slice(0, 3).map((ch) => {
              const info = DELIVERY_TYPE_MAP[ch.deliveryType] || {
                color: 'default',
                text: ch.deliveryType,
              };
              return (
                <Tooltip key={ch.configId} title={ch.configName}>
                  <Tag color={ch.enabled ? info.color : 'default'}>
                    <SendOutlined /> {info.text}
                  </Tag>
                </Tooltip>
              );
            })}
            {channels.length > 3 && (
              <Tag>+{channels.length - 3}</Tag>
            )}
          </Space>
        );
      },
    },
    {
      title: '订阅',
      dataIndex: 'subscribed',
      width: 70,
      align: 'center' as const,
      render: (subscribed: boolean, record: FixedReport) => (
        <Button
          type="text"
          size="small"
          icon={subscribed ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
          onClick={(e) => {
            e.stopPropagation();
            if (subscribed) {
              handleUnsubscribe(record.datasetId);
            } else {
              handleSubscribe(record.datasetId);
            }
          }}
        />
      ),
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right' as const,
      render: (_: any, record: FixedReport) => (
        <Space size={4} wrap>
          <Button
            type="link"
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              openDetail(record);
            }}
          >
            查看结果
          </Button>
          <Button
            type="link"
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              openHistory(record);
            }}
          >
            查看历史
          </Button>
          <Button
            type="link"
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              handleCreateSchedule(record.datasetId);
            }}
          >
            创建任务
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.reportsPage}>
      <div className={styles.pageHeader}>
        <h3 className={styles.pageTitle}>固定报表</h3>
      </div>
      <div className={styles.filterRow}>
        <FilterBar
          keyword={keyword}
          domainName={domainName}
          statusFilter={statusFilter}
          viewFilter={viewFilter}
          domainOptions={domainOptions}
          onKeywordChange={setKeyword}
          onDomainChange={setDomainName}
          onStatusChange={setStatusFilter}
          onViewChange={setViewFilter}
        />
      </div>
      <div className={styles.tableShell}>
        <Table
          rowKey="datasetId"
          size="middle"
          bordered={false}
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: (
              <Empty
                description={
                  viewFilter === 'subscribed' && !keyword && !domainName && !statusFilter
                    ? '你还没有订阅固定报表'
                    : keyword || domainName || statusFilter || viewFilter
                      ? '筛选后无匹配结果'
                      : '暂无固定报表'
                }
              />
            ),
          }}
          pagination={{
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onRow={(record) => ({
            onClick: () => openDetail(record),
            style: { cursor: 'pointer' },
          })}
        />
      </div>

      <ReportDetailDrawer
        visible={drawerVisible}
        report={selectedReport}
        onClose={() => setDrawerVisible(false)}
        onSubscribe={handleSubscribe}
        onUnsubscribe={handleUnsubscribe}
        onCreateSchedule={handleCreateSchedule}
        onViewHistory={(datasetId) => {
          if (selectedReport?.datasetId === datasetId) {
            openHistory(selectedReport);
          }
        }}
      />

      <ReportHistoryDrawer
        visible={historyDrawer.visible}
        datasetId={historyDrawer.datasetId}
        reportName={historyDrawer.reportName}
        onClose={() => setHistoryDrawer({ visible: false })}
      />

      <ScheduleForm
        visible={scheduleFormVisible}
        initialDatasetId={scheduleDatasetId}
        onCancel={() => {
          setScheduleFormVisible(false);
          setScheduleDatasetId(undefined);
        }}
        onSubmit={handleScheduleSubmit}
      />
    </div>
  );
};

export default ReportsPage;

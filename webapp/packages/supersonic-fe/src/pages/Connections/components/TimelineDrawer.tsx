import React, { useEffect, useState } from 'react';
import { Drawer, Timeline, Spin, Empty, Tag, Typography, Pagination } from 'antd';
import {
  PlayCircleOutlined,
  CheckCircleOutlined,
  SwapOutlined,
  SearchOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { getTimeline, ConnectionEventDO } from '@/services/connection';

interface TimelineDrawerProps {
  visible: boolean;
  connectionId?: number;
  connectionName?: string;
  onClose: () => void;
}

const EVENT_CONFIG: Record<string, { color: string; icon: React.ReactNode; text: string }> = {
  SYNC_STARTED: { color: 'blue', icon: <PlayCircleOutlined />, text: '同步开始' },
  SYNC_COMPLETED: { color: 'green', icon: <CheckCircleOutlined />, text: '同步完成' },
  STATUS_CHANGED: { color: 'orange', icon: <SwapOutlined />, text: '状态变更' },
  SCHEMA_DETECTED: { color: 'purple', icon: <SearchOutlined />, text: 'Schema发现' },
  STATE_RESET: { color: 'cyan', icon: <ReloadOutlined />, text: '状态重置' },
  CONFIG_UPDATED: { color: 'geekblue', icon: <SettingOutlined />, text: '配置更新' },
};

const TimelineDrawer: React.FC<TimelineDrawerProps> = ({
  visible,
  connectionId,
  connectionName,
  onClose,
}) => {
  const [loading, setLoading] = useState(false);
  const [events, setEvents] = useState<ConnectionEventDO[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  const fetchEvents = async (current = 1, pageSize = 20) => {
    if (!connectionId) return;
    setLoading(true);
    try {
      const res = await getTimeline(connectionId, { current, pageSize });
      setEvents(res?.records || []);
      setPagination({ current, pageSize, total: res?.total || 0 });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && connectionId) {
      fetchEvents();
    }
  }, [visible, connectionId]);

  const renderEventData = (eventData?: string) => {
    if (!eventData) return null;
    try {
      const data = JSON.parse(eventData);
      return (
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: 11 }}>
            {JSON.stringify(data, null, 2)}
          </pre>
        </Typography.Text>
      );
    } catch {
      return <Typography.Text type="secondary">{eventData}</Typography.Text>;
    }
  };

  return (
    <Drawer
      title={`事件时间线 - ${connectionName || ''}`}
      open={visible}
      onClose={onClose}
      width={500}
    >
      <Spin spinning={loading}>
        {events.length === 0 ? (
          <Empty description="暂无事件" />
        ) : (
          <>
            <Timeline
              items={events.map((event) => {
                const config = EVENT_CONFIG[event.eventType] || {
                  color: 'gray',
                  icon: null,
                  text: event.eventType,
                };
                return {
                  color: config.color,
                  dot: config.icon,
                  children: (
                    <div>
                      <div>
                        <Tag color={config.color}>{config.text}</Tag>
                        <span style={{ fontSize: 12, color: '#999' }}>
                          {dayjs(event.eventTime).format('YYYY-MM-DD HH:mm:ss')}
                        </span>
                      </div>
                      {event.userName && (
                        <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                          操作人: {event.userName}
                        </div>
                      )}
                      {event.eventData && (
                        <div style={{ marginTop: 4 }}>{renderEventData(event.eventData)}</div>
                      )}
                    </div>
                  ),
                };
              })}
            />
            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Pagination
                size="small"
                current={pagination.current}
                pageSize={pagination.pageSize}
                total={pagination.total}
                onChange={(page, size) => fetchEvents(page, size)}
                showSizeChanger={false}
              />
            </div>
          </>
        )}
      </Spin>
    </Drawer>
  );
};

export default TimelineDrawer;

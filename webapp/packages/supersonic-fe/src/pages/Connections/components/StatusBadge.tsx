import React from 'react';
import { Tag } from 'antd';
import {
  CheckCircleOutlined,
  PauseCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';

interface StatusBadgeProps {
  status: string;
}

const STATUS_CONFIG: Record<
  string,
  { color: string; text: string; icon: React.ReactNode }
> = {
  ACTIVE: {
    color: 'green',
    text: '运行中',
    icon: <CheckCircleOutlined />,
  },
  PAUSED: {
    color: 'orange',
    text: '已暂停',
    icon: <PauseCircleOutlined />,
  },
  BROKEN: {
    color: 'red',
    text: '已中断',
    icon: <ExclamationCircleOutlined />,
  },
  DEPRECATED: {
    color: 'default',
    text: '已废弃',
    icon: <CloseCircleOutlined />,
  },
};

const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const config = STATUS_CONFIG[status] || STATUS_CONFIG.ACTIVE;
  return (
    <Tag color={config.color} icon={config.icon}>
      {config.text}
    </Tag>
  );
};

export default StatusBadge;

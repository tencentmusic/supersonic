import React, { useState } from 'react';
import { Button, Modal, Tag, List, Space, message } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons';
import { MsgDataType, PluginResonseType, ScheduleSummaryType, ExecutionSummaryType } from '../../../common/type';
import { PREFIX_CLS } from '../../../common/constants';
import './style.less';

type Props = {
  data: MsgDataType;
  onSendMsg?: (msg: string) => void;
};

const ReportScheduleMsg: React.FC<Props> = ({ data, onSendMsg }) => {
  const prefixCls = `${PREFIX_CLS}-report-schedule`;
  const [confirmVisible, setConfirmVisible] = useState(false);
  const response = data.response as PluginResonseType;

  if (!response) {
    return null;
  }

  const { intent, message: respMessage, success, needConfirm, schedules, executions } = response;

  const handleConfirm = () => {
    setConfirmVisible(false);
    if (onSendMsg) {
      onSendMsg('确认');
    }
  };

  const handleCancel = () => {
    setConfirmVisible(false);
    message.info('已取消操作');
  };

  const getStatusIcon = (status?: string) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'FAILED':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
      case 'RUNNING':
        return <ClockCircleOutlined style={{ color: '#1890ff' }} />;
      default:
        return <ClockCircleOutlined style={{ color: '#999' }} />;
    }
  };

  const renderScheduleList = (scheduleList: ScheduleSummaryType[]) => {
    if (!scheduleList || scheduleList.length === 0) {
      return null;
    }

    return (
      <List
        className={`${prefixCls}-list`}
        dataSource={scheduleList}
        renderItem={(item) => (
          <List.Item>
            <List.Item.Meta
              title={
                <Space>
                  <span>#{item.id} {item.name}</span>
                  {item.enabled ? (
                    <Tag icon={<PlayCircleOutlined />} color="success">运行中</Tag>
                  ) : (
                    <Tag icon={<PauseCircleOutlined />} color="default">已暂停</Tag>
                  )}
                </Space>
              }
              description={
                <div>
                  <div>频率: {item.cronDescription || item.cronExpression}</div>
                  {item.lastExecutionTime && (
                    <div>上次执行: {item.lastExecutionTime} {getStatusIcon(item.lastExecutionStatus)}</div>
                  )}
                </div>
              }
            />
          </List.Item>
        )}
      />
    );
  };

  const renderExecutionList = (executionList: ExecutionSummaryType[]) => {
    if (!executionList || executionList.length === 0) {
      return null;
    }

    return (
      <List
        className={`${prefixCls}-list`}
        dataSource={executionList}
        renderItem={(item) => (
          <List.Item>
            <Space>
              {getStatusIcon(item.status)}
              <span>{item.startTime}</span>
              {item.endTime && <span>- {item.endTime}</span>}
              {item.errorMessage && (
                <Tag color="error">{item.errorMessage}</Tag>
              )}
            </Space>
          </List.Item>
        )}
      />
    );
  };

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-message`}>
        {success === false && (
          <ExclamationCircleOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
        )}
        <pre style={{ whiteSpace: 'pre-wrap', margin: 0, fontFamily: 'inherit' }}>
          {respMessage}
        </pre>
      </div>

      {schedules && renderScheduleList(schedules)}
      {executions && renderExecutionList(executions)}

      {needConfirm && (
        <div className={`${prefixCls}-actions`} style={{ marginTop: 16 }}>
          <Space>
            <Button type="primary" onClick={() => setConfirmVisible(true)}>
              确认
            </Button>
            <Button onClick={handleCancel}>
              取消
            </Button>
          </Space>
        </div>
      )}

      <Modal
        title="确认操作"
        open={confirmVisible}
        onOk={handleConfirm}
        onCancel={() => setConfirmVisible(false)}
        okText="确认"
        cancelText="取消"
      >
        <p>确定要执行此操作吗?</p>
      </Modal>
    </div>
  );
};

export default ReportScheduleMsg;

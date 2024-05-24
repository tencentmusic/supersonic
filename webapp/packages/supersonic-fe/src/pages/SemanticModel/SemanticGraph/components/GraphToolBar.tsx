import { Button, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import React from 'react';

type Props = {
  onClick: (params?: { eventName?: string }) => void;
  [key: string]: any;
};

const GraphToolBar: React.FC<Props> = ({ onClick }) => {
  return (
    <div
      style={{
        padding: 0,
        backgroundColor: '#fff',
        display: 'flex',
        justifyContent: 'end',
        position: 'absolute',
        top: 20,
        right: 20,
        zIndex: 1,
      }}
    >
      <Space>
        <Button
          key="createDatabaseBtn"
          icon={<PlusOutlined />}
          size="small"
          onClick={() => {
            onClick?.({ eventName: 'createDatabase' });
          }}
        >
          新建模型
        </Button>
        {/* <Button
          key="createDimensionBtn"
          icon={<PlusOutlined />}
          size="small"
          onClick={() => {
            onClick?.({ eventName: 'createDimension' });
          }}
        >
          新建维度
        </Button>
        <Button
          key="createMetricBtn"
          icon={<PlusOutlined />}
          size="small"
          onClick={() => {
            onClick?.({ eventName: 'createMetric' });
          }}
        >
          新建指标
        </Button> */}
      </Space>
    </div>
  );
};

export default GraphToolBar

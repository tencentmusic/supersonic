import { Space, Tooltip } from 'antd';
import React from 'react';
import { ExclamationCircleOutlined } from '@ant-design/icons';

type Props = {
  title: string;
  tooltips: string;
};

const TableTitleTooltips: React.FC<Props> = ({ title, tooltips }) => {
  return (
    <>
      <Space>
        <span>{title}</span>
        <Tooltip title={tooltips}>
          <ExclamationCircleOutlined />
        </Tooltip>
      </Space>
    </>
  );
};

export default TableTitleTooltips;

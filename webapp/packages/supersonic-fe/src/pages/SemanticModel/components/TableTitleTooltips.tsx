import { Space, Tooltip } from 'antd';
import React, { ReactNode } from 'react';
import { ExclamationCircleOutlined } from '@ant-design/icons';

type Props = {
  title: string;
  tooltips: string | ReactNode;
  [key: string]: any;
};

const TableTitleTooltips: React.FC<Props> = ({ title, tooltips, ...rest }) => {
  return (
    <>
      <Space>
        <span>{title}</span>
        <Tooltip title={tooltips} {...rest}>
          <ExclamationCircleOutlined />
        </Tooltip>
      </Space>
    </>
  );
};

export default TableTitleTooltips;

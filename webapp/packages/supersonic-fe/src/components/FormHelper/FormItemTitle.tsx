import { Space } from 'antd';
import { ReactNode } from 'react';
export interface IProps {
  title: string | ReactNode;
  subTitle?: string;
}

const FormItemTitle: React.FC<IProps> = ({ title, subTitle }) => {
  return (
    <Space direction="vertical" size={2}>
      <span>{title}</span>
      {subTitle && <span style={{ fontSize: '12px', color: '#6a6a6a' }}>{subTitle}</span>}
    </Space>
  );
};

export default FormItemTitle;

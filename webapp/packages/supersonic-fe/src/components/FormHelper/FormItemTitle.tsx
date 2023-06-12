import { Space } from 'antd';
export interface IProps {
  title: string;
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

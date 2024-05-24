import { Space, Typography } from 'antd';
import { ReactNode } from 'react';
import styles from './index.less';
export interface IProps {
  title: string | ReactNode;
  subTitle?: string;
  subTitleEditable?: boolean;
  onSubTitleChange?: (title: string) => void;
}

const { Paragraph } = Typography;

const FormItemTitle: React.FC<IProps> = ({
  title,
  subTitle,
  subTitleEditable = false,
  onSubTitleChange,
}) => {
  return (
    <Space direction="vertical" size={2} style={{ width: '100%' }}>
      <div className={styles.title}>{title}</div>
      <div className={styles.subTitleContainer}>
        {subTitleEditable ? (
          <Paragraph
            editable={{
              onChange: (title: string) => {
                onSubTitleChange?.(title);
              },
            }}
          >
            {subTitle || '添加描述'}
          </Paragraph>
        ) : (
          subTitle && <span style={{ fontSize: '12px', color: '#7b809a' }}>{subTitle}</span>
        )}
      </div>
    </Space>
  );
};

export default FormItemTitle;

import React from 'react';
import { Space } from 'antd';

type Props = {
  title: string;
  labelStyles?: CSSStyleSheet;
};

const FormLabelRequire: React.FC<Props> = ({ title, labelStyles = {} }) => {
  return (
    <>
      <div className="ant-col ant-form-item-label" style={{ padding: 0 }}>
        <label
          htmlFor="description"
          className="ant-form-item-required"
          title={title}
          style={{ fontSize: '16px', ...labelStyles }}
        >
          <Space size={5}>
            <span style={{ color: '#ff4d4f', fontSize: '18px', position: 'relative', top: 3 }}>
              *
            </span>
            {title}
          </Space>
        </label>
      </div>
    </>
  );
};

export default FormLabelRequire;

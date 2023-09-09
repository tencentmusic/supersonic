import React from 'react';

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
          {title}
        </label>
      </div>
    </>
  );
};

export default FormLabelRequire;

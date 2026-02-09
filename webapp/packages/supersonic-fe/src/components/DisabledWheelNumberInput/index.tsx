import React from 'react';
import { InputNumber } from 'antd';

const DisabledWheelNumberInput: React.FC<any> = ({ ...rest }) => {
  return (
    <InputNumber
      {...rest}
      onWheel={(e) => e.currentTarget.blur()}
    />
  );
};

export default DisabledWheelNumberInput;

import React, { useEffect, useRef } from 'react';
import { InputNumber } from 'antd';

const DisabledWheelNumberInput: React.FC<any> = ({ ...rest }) => {
  const ref = useRef<any>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.addEventListener('wheel', handleWheel);
    }
  }, []);

  const handleWheel = (event) => {
    event.stopPropagation();
    event.preventDefault();
  };

  return <InputNumber ref={ref} {...rest} />;
};

export default DisabledWheelNumberInput;

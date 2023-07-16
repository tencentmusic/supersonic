import { Tag } from 'antd';
import React from 'react';

import { TransType } from '../enum';

type Props = {
  type: TransType;
};

const TransTypeTag: React.FC<Props> = ({ type }) => {
  return (
    <>
      {type === TransType.DIMENSION ? (
        <Tag color="blue">{'维度'}</Tag>
      ) : type === 'metric' ? (
        <Tag color="orange">{'指标'}</Tag>
      ) : (
        <></>
      )}
    </>
  );
};

export default TransTypeTag;

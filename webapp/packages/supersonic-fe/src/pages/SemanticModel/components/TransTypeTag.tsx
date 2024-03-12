import { Tag } from 'antd';
import React from 'react';

import { SemanticNodeType } from '../enum';

type Props = {
  type: SemanticNodeType;
};

const TransTypeTag: React.FC<Props> = ({ type }) => {
  return (
    <>
      {type === SemanticNodeType.DIMENSION ? (
        <Tag color="blue">{'维度'}</Tag>
      ) : type === SemanticNodeType.METRIC ? (
        <Tag color="orange">{'指标'}</Tag>
      ) : type === SemanticNodeType.DATASOURCE ? (
        <Tag color="green">{'模型'}</Tag>
      ) : type === SemanticNodeType.TAG ? (
        <Tag color="green">{'标签'}</Tag>
      ) : (
        <></>
      )}
    </>
  );
};

export default TransTypeTag;

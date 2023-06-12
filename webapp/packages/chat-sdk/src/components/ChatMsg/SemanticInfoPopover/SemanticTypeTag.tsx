import { Tag } from 'antd';
import React from 'react';
import { SemanticTypeEnum, SEMANTIC_TYPE_MAP } from '../../../common/type';

type Props = {
  infoType?: SemanticTypeEnum;
};

const SemanticTypeTag: React.FC<Props> = ({ infoType = SemanticTypeEnum.METRIC }) => {
  return (
    <Tag
      color={
        infoType === SemanticTypeEnum.DIMENSION || infoType === SemanticTypeEnum.DOMAIN
          ? 'blue'
          : infoType === SemanticTypeEnum.VALUE
            ? 'geekblue'
            : 'orange'
      }
    >
      {SEMANTIC_TYPE_MAP[infoType]}
    </Tag>
  );
};

export default SemanticTypeTag;

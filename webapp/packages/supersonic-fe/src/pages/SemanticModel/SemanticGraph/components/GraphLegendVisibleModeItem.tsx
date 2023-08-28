import { Segmented } from 'antd';

import React from 'react';
import { SemanticNodeType } from '../../enum';
import styles from '../style.less';

type Props = {
  value?: SemanticNodeType;
  onChange?: (value: SemanticNodeType) => void;
  [key: string]: any;
};

const GraphLegendVisibleModeItem: React.FC<Props> = ({ value, onChange }) => {
  return (
    <div className={styles.graphLegendVisibleModeItem}>
      <Segmented
        size="small"
        block={true}
        value={value}
        onChange={(changeValue) => {
          onChange?.(changeValue as SemanticNodeType);
        }}
        options={[
          {
            value: '',
            label: '全部',
          },
          {
            value: SemanticNodeType.DIMENSION,
            label: '仅维度',
          },
          {
            value: SemanticNodeType.METRIC,
            label: '仅指标',
          },
        ]}
      />
    </div>
  );
};

export default GraphLegendVisibleModeItem;

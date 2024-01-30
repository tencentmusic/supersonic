import { Space } from 'antd';
import React, { ReactNode } from 'react';

import styles from './style.less';

type Props = {
  components: {
    label: string;
    component: ReactNode;
  }[];
};

const TableHeaderFilter: React.FC<Props> = ({ components }) => {
  return (
    <>
      <Space className={styles.tableHeaderTitle} size={20}>
        {components.map(({ label, component }) => (
          <Space size={8} key={`TableHeaderFilter-${label}`}>
            <span className={styles.headerTitleLabel}>{label}:</span>
            {component}
          </Space>
        ))}
      </Space>
    </>
  );
};

export default TableHeaderFilter;

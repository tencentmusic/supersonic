import React, { useState, useEffect } from 'react';
import SplitPane from 'react-split-pane';
import SqlSide from './components/SqlSide';
import Pane from 'react-split-pane/lib/Pane';
import styles from './style.less';
import { RightOutlined, LeftOutlined } from '@ant-design/icons';
import { DataSourceSubmitData } from './components/SqlDetail';

type Props = {
  initialValues: any;
  onSubmitSuccess?: (dataSourceInfo: DataSourceSubmitData) => void;
};

const DEFAULT_RIGHT_SIZE = '300px';

const DataExploreView: React.FC<Props> = ({ initialValues, onSubmitSuccess }) => {
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    const exploreRightCollapsed = localStorage.getItem('exploreRightCollapsed');
    setCollapsed(exploreRightCollapsed === 'true');
  }, []);

  const onCollapse = () => {
    const collapsedValue = !collapsed;
    setCollapsed(collapsedValue);
    localStorage.setItem('exploreRightCollapsed', String(collapsedValue));
    const exploreRightSize = collapsedValue ? '0px' : localStorage.getItem('exploreRightSize');
    const sizeValue = parseInt(exploreRightSize || '0');
    if (!collapsedValue && sizeValue <= 10) {
      localStorage.setItem('exploreRightSize', DEFAULT_RIGHT_SIZE);
    }
  };

  return (
    <div
      className={`${styles.pageContainer} ${
        window.location.hash.includes('external') ? styles.externalPageContainer : ''
      }`}
    >
      <div className={styles.main}>
        <SplitPane
          split="vertical"
          onChange={(size) => {
            localStorage.setItem('exploreRightSize', size[1]);
          }}
        >
          <div className={styles.rightListSide}>
            {false && (
              <div className={styles.collapseRightBtn} onClick={onCollapse}>
                {collapsed ? <LeftOutlined /> : <RightOutlined />}
              </div>
            )}
            <SqlSide initialValues={initialValues} onSubmitSuccess={onSubmitSuccess} />
          </div>

          <Pane initialSize={0} />
        </SplitPane>
      </div>
    </div>
  );
};

export default DataExploreView;

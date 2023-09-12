import React, { useState, useRef, useEffect } from 'react';
import { Tabs } from 'antd';
import SqlDetail from './SqlDetail';

import styles from '../style.less';

type Panes = {
  title: string;
  key: string;
  type: 'add' | 'edit';
  scriptId?: number;
  sql?: string;
  isSave?: boolean; // 暂存提示保存
};

type TableRef = {
  current?: {
    fetchSqlList: () => void;
    upDateActiveItem: (key: any) => void;
  };
};

type Props = {
  initialValues: any;
  onSubmitSuccess?: (dataSourceInfo: any) => void;
};

const { TabPane } = Tabs;
const LIST_KEY = 'list';

const SqlSide: React.FC<Props> = ({ initialValues, onSubmitSuccess }) => {
  const defaultPanes: Panes[] = [
    {
      key: '数据源查询',
      title: initialValues?.name || '数据源查询',
      type: 'add',
      isSave: true,
    },
  ];

  const [activeKey, setActiveKey] = useState('数据源查询');
  const [panes, setPanes] = useState<Panes[]>(defaultPanes);
  const tableRef: TableRef = useRef();
  const panesRef = useRef<Panes[]>(defaultPanes);

  const updatePane = (list: Panes[]) => {
    setPanes(list);
    panesRef.current = list;
  };

  // 更新脚本内容
  const updateTabSql = (sql: string, targetKey: string) => {
    const newPanes = panesRef.current.slice();
    const index = newPanes.findIndex((item) => item.key === targetKey);
    const targetItem = newPanes[index];
    newPanes.splice(index, 1, {
      ...targetItem,
      sql,
      isSave: false,
    });
    updatePane(newPanes);
  };

  useEffect(() => {
    if (initialValues) {
      updateTabSql(initialValues?.datasourceDetail?.sqlQuery || '', '数据源查询');
    }
  }, [initialValues]);

  const onChange = (key: string) => {
    setActiveKey(key);
    tableRef?.current?.upDateActiveItem(key);
    if (key === LIST_KEY) {
      tableRef?.current?.fetchSqlList();
    }
  };

  return (
    <>
      <div className={styles.outside}>
        <Tabs
          type="editable-card"
          hideAdd={true}
          activeKey={activeKey}
          onChange={onChange}
          className={styles.middleArea}
        >
          {panes.map((pane) => {
            return (
              <TabPane
                tab={<div className={styles.paneName}>{pane.title}</div>}
                closable={false}
                key={pane.key}
              >
                <SqlDetail
                  onSubmitSuccess={onSubmitSuccess}
                  dataSourceItem={initialValues}
                  onUpdateSql={(sql: string) => {
                    updateTabSql(sql, pane.key);
                  }}
                  sql={pane.sql}
                />
              </TabPane>
            );
          })}
        </Tabs>
      </div>
      {/* </SplitPane> */}
    </>
  );
};

export default SqlSide;

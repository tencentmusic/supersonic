import { Tabs, Popover } from 'antd';
import React, { useEffect, useState } from 'react';
import { connect, Helmet } from 'umi';
import ProjectListTree from './components/ProjectList';
import ClassDataSourceTable from './components/ClassDataSourceTable';
import ClassDimensionTable from './components/ClassDimensionTable';
import ClassMetricTable from './components/ClassMetricTable';
import PermissionSection from './components/Permission/PermissionSection';
import DatabaseSection from './components/Database/DatabaseSection';
import styles from './components/style.less';
import type { StateType } from './model';
import { DownOutlined } from '@ant-design/icons';
import SemanticFlow from './SemanticFlows';
import type { Dispatch } from 'umi';

const { TabPane } = Tabs;

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};

const DomainManger: React.FC<Props> = ({ domainManger, dispatch }) => {
  window.RUNNING_ENV = 'semantic';
  const { selectDomainId, selectDomainName } = domainManger;

  const [open, setOpen] = useState(false);

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
  };

  useEffect(() => {
    if (selectDomainId) {
      dispatch({
        type: 'domainManger/queryDimensionList',
        payload: {
          domainId: selectDomainId,
        },
      });
      dispatch({
        type: 'domainManger/queryMetricList',
        payload: {
          domainId: selectDomainId,
        },
      });
      dispatch({
        type: 'domainManger/queryDatabaseByDomainId',
        payload: {
          domainId: selectDomainId,
        },
      });
    }
  }, [selectDomainId]);

  useEffect(() => {
    const width = document.getElementById('tab');
    const switchWarpper: any = document.getElementById('switch');
    if (width && switchWarpper) {
      switchWarpper.style.width = width.offsetWidth * 0.77 + 'px';
    }
  });

  return (
    <div className={styles.projectBody}>
      <Helmet title={'语义建模-超音数'} />
      {/* 页面改版取消侧边栏转换为popover形式后，因为popover不触发则组件不加载，需要保留原本页面初始化需要ProjectListTree向model中写入首个主题域数据逻辑，在此引入但并不显示 */}
      <div style={{ display: 'none' }}>
        <ProjectListTree />
      </div>
      <div className={styles.projectManger}>
        <h2 className={styles.title}>
          <Popover
            zIndex={1000}
            overlayInnerStyle={{
              overflow: 'scroll',
              maxHeight: '800px',
            }}
            content={
              <ProjectListTree
                onTreeSelected={() => {
                  setOpen(false);
                }}
              />
            }
            trigger="click"
            open={open}
            onOpenChange={handleOpenChange}
          >
            <div className={styles.domainSelector}>
              <span className={styles.domainTitle}>
                {selectDomainName ? `选择的主题域：${selectDomainName}` : '主题域信息'}
              </span>
              <span className={styles.downIcon}>
                <DownOutlined />
              </span>
            </div>
          </Popover>
        </h2>
        {selectDomainId ? (
          <>
            <Tabs className={styles.tab} defaultActiveKey="xflow" destroyInactiveTabPane>
              {/* <TabPane className={styles.tabPane} tab="关系可视化" key="graph">
                  <div style={{ width: '100%', height: 'calc(100vh - 200px)' }}>
                    <SemanticGraph domainId={selectDomainId} />
                  </div>
                </TabPane> */}
              <TabPane className={styles.tabPane} tab="可视化建模" key="xflow">
                <div style={{ width: '100%', height: 'calc(100vh - 200px)' }}>
                  <SemanticFlow />
                </div>
              </TabPane>
              <TabPane className={styles.tabPane} tab="数据库" key="dataBase">
                <DatabaseSection />
              </TabPane>
              <TabPane className={styles.tabPane} tab="数据源" key="dataSource">
                <ClassDataSourceTable />
              </TabPane>
              <TabPane className={styles.tabPane} tab="维度" key="dimenstion">
                <ClassDimensionTable key={selectDomainId} />
              </TabPane>
              <TabPane className={styles.tabPane} tab="指标" key="metric">
                <ClassMetricTable />
              </TabPane>
              <TabPane className={styles.tabPane} tab="权限管理" key="permissonSetting">
                <PermissionSection />
              </TabPane>
            </Tabs>
          </>
        ) : (
          <h2 className={styles.mainTip}>请选择项目</h2>
        )}
      </div>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DomainManger);

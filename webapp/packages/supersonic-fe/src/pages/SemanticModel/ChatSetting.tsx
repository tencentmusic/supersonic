import { Tabs, Popover } from 'antd';
import React, { useEffect, useState } from 'react';
import { connect, Helmet } from 'umi';
import ProjectListTree from './components/ProjectList';
import styles from './components/style.less';
import type { StateType } from './model';
import { DownOutlined } from '@ant-design/icons';
import EntitySection from './components/Entity/EntitySection';
import type { Dispatch } from 'umi';

const { TabPane } = Tabs;

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};

const ChatSetting: React.FC<Props> = ({ domainManger, dispatch }) => {
  window.RUNNING_ENV = 'chat';
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
    }
  }, [selectDomainId]);

  return (
    <div className={styles.projectBody}>
      <Helmet title={'问答设置-超音数'} />
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
            <Tabs className={styles.tab} defaultActiveKey="chatSetting" destroyInactiveTabPane>
              <TabPane className={styles.tabPane} tab="问答设置" key="chatSetting">
                <EntitySection />
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
}))(ChatSetting);

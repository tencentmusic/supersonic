import { Tabs } from 'antd';
import React, { useEffect, useState } from 'react';
import { connect, Helmet } from 'umi';
import ProjectListTree from './components/ProjectList';
import EntitySection from './components/Entity/EntitySection';
import styles from './components/style.less';
import type { StateType } from './model';
import { RightOutlined, LeftOutlined } from '@ant-design/icons';
import SplitPane from 'react-split-pane';
import Pane from 'react-split-pane/lib/Pane';
import type { Dispatch } from 'umi';

const { TabPane } = Tabs;

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};

const DEFAULT_LEFT_SIZE = '300px';

const ChatSetting: React.FC<Props> = ({ domainManger, dispatch }) => {
  window.RUNNING_ENV = 'chat';
  const [collapsed, setCollapsed] = useState(false);
  const [leftSize, setLeftSize] = useState('');
  const { selectDomainId, selectDomainName } = domainManger;
  useEffect(() => {
    const semanticLeftCollapsed = localStorage.getItem('semanticLeftCollapsed');
    const semanticLeftSize =
      semanticLeftCollapsed === 'true' ? '0px' : localStorage.getItem('semanticLeftSize');
    setCollapsed(semanticLeftCollapsed === 'true');
    setLeftSize(semanticLeftSize || DEFAULT_LEFT_SIZE);
  }, []);

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

  const onCollapse = () => {
    const collapsedValue = !collapsed;
    setCollapsed(collapsedValue);
    localStorage.setItem('semanticLeftCollapsed', String(collapsedValue));
    const semanticLeftSize = collapsedValue ? '0px' : localStorage.getItem('semanticLeftSize');
    const sizeValue = parseInt(semanticLeftSize || '0');
    if (!collapsedValue && sizeValue <= 10) {
      setLeftSize(DEFAULT_LEFT_SIZE);
      localStorage.setItem('semanticLeftSize', DEFAULT_LEFT_SIZE);
    } else {
      setLeftSize(semanticLeftSize || DEFAULT_LEFT_SIZE);
    }
  };

  useEffect(() => {
    const width = document.getElementById('tab');
    const switchWarpper: any = document.getElementById('switch');
    if (width && switchWarpper) {
      switchWarpper.style.width = width.offsetWidth * 0.77 + 'px';
    }
  });

  return (
    <div className={styles.projectBody}>
      <Helmet title={'问答设置-超音数'} />
      <SplitPane
        split="vertical"
        onChange={(size) => {
          localStorage.setItem('semanticLeftSize', size[0]);
          setLeftSize(size[0]);
        }}
      >
        <Pane initialSize={leftSize || DEFAULT_LEFT_SIZE}>
          <div className={styles.menu}>
            <ProjectListTree createDomainBtnVisible={false} queryService="chat" />
          </div>
        </Pane>

        <div className={styles.projectManger}>
          <div className={styles.collapseLeftBtn} onClick={onCollapse}>
            {collapsed ? <RightOutlined /> : <LeftOutlined />}
          </div>
          <h2 className={styles.title}>
            {selectDomainName ? `选择的主题域：${selectDomainName}` : '主题域信息'}
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
      </SplitPane>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ChatSetting);

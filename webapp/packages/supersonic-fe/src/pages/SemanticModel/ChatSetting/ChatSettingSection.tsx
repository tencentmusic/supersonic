// import { Tabs } from 'antd';
import React from 'react';
import { connect } from 'umi';

// import styles from '../components/style.less';
import type { StateType } from '../model';
import ProCard from '@ant-design/pro-card';
import EntitySection from '../components/Entity/EntitySection';
// import RecommendedQuestionsSection from '../components/Entity/RecommendedQuestionsSection';
import { ChatConfigType } from '../enum';
import type { Dispatch } from 'umi';

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};

const ChatSettingSection: React.FC<Props> = () => {
  // const isModelItem = [
  //   {
  //     label: '指标模式',
  //     key: 'metric',
  //     children: <EntitySection chatConfigType={ChatConfigType.AGG} />,
  //   },
  //   {
  //     label: '实体模式',
  //     key: 'dimenstion',
  //     children: <EntitySection chatConfigType={ChatConfigType.DETAIL} />,
  //   },
  //   {
  //     label: '推荐问题',
  //     key: 'recommendedQuestions',
  //     children: <RecommendedQuestionsSection />,
  //   },
  // ];

  return (
    <div style={{ width: 900, margin: '20px auto' }}>
      {/* <Tabs
        className={styles.chatSettingSectionTab}
        items={isModelItem}
        destroyInactiveTabPane
        tabPosition="left"
      /> */}
      <ProCard bordered title="指标模式" style={{ marginBottom: 20 }}>
        <EntitySection chatConfigType={ChatConfigType.AGG} />
      </ProCard>
      <ProCard bordered title="实体模式" style={{ marginBottom: 20 }}>
        <EntitySection chatConfigType={ChatConfigType.DETAIL} />
      </ProCard>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ChatSettingSection);

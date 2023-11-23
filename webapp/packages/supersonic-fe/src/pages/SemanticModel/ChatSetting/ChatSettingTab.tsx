import { Tabs, Button } from 'antd';
import React from 'react';
import { connect } from 'umi';

import styles from '../components/style.less';
import type { StateType } from '../model';
import { LeftOutlined } from '@ant-design/icons';
import EntitySection from '../components/Entity/EntitySection';
import RecommendedQuestionsSection from '../components/Entity/RecommendedQuestionsSection';
import { ISemantic } from '../data';

import OverView from '../components/OverView';
import { ChatConfigType } from '../enum';
import type { Dispatch } from 'umi';

type Props = {
  isModel: boolean;
  activeKey: string;
  modelList: ISemantic.IModelItem[];
  handleModelChange: (model?: ISemantic.IModelItem) => void;
  onBackDomainBtnClick?: () => void;
  onMenuChange?: (menuKey: string) => void;
  domainManger: StateType;
  dispatch: Dispatch;
};

const ChatSetting: React.FC<Props> = ({
  isModel,
  activeKey,
  modelList,
  handleModelChange,
  onBackDomainBtnClick,
  onMenuChange,
}) => {
  const defaultTabKey = 'metric';

  const isModelItem = [
    {
      label: '指标模式',
      key: 'metric',
      children: <EntitySection chatConfigType={ChatConfigType.AGG} />,
    },
    {
      label: '标签模式',
      key: 'dimenstion',
      children: <EntitySection chatConfigType={ChatConfigType.DETAIL} />,
    },
    {
      label: '推荐问题',
      key: 'recommendedQuestions',
      children: <RecommendedQuestionsSection />,
    },
  ];

  const tabItem = [
    {
      label: '模型',
      key: 'overview',
      children: (
        <OverView
          modelList={modelList}
          disabledEdit={true}
          onModelChange={(model) => {
            handleModelChange(model);
          }}
        />
      ),
    },
  ];

  return (
    <>
      <Tabs
        className={styles.tab}
        items={isModel ? isModelItem : tabItem}
        activeKey={activeKey || defaultTabKey}
        destroyInactiveTabPane
        tabBarExtraContent={
          isModel ? (
            <Button
              type="primary"
              icon={<LeftOutlined />}
              onClick={() => {
                onBackDomainBtnClick?.();
              }}
              style={{ marginRight: 10 }}
            >
              返回主题域
            </Button>
          ) : undefined
        }
        onChange={(menuKey: string) => {
          onMenuChange?.(menuKey);
        }}
      />
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ChatSetting);

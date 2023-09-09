import { Tabs, Button } from 'antd';
import React from 'react';
import { connect } from 'umi';

import ClassDataSourceTable from './ClassDataSourceTable';
import ClassDimensionTable from './ClassDimensionTable';
import ClassMetricTable from './ClassMetricTable';
import PermissionSection from './Permission/PermissionSection';
// import DatabaseSection from './Database/DatabaseSection';
import EntitySettingSection from './Entity/EntitySettingSection';
import ChatSettingSection from '../ChatSetting/ChatSettingSection';
import OverView from './OverView';
import styles from './style.less';
import type { StateType } from '../model';
import { LeftOutlined } from '@ant-design/icons';
import { ISemantic } from '../data';
import SemanticGraphCanvas from '../SemanticGraphCanvas';
import RecommendedQuestionsSection from '../components/Entity/RecommendedQuestionsSection';

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
const DomainManagerTab: React.FC<Props> = ({
  isModel,
  activeKey,
  modelList,
  domainManger,
  handleModelChange,
  onBackDomainBtnClick,
  onMenuChange,
}) => {
  const defaultTabKey = 'xflow';
  const { selectDomainId, domainList } = domainManger;
  const tabItem = [
    {
      label: '模型',
      key: 'overview',
      children: (
        <OverView
          modelList={modelList}
          onModelChange={(model) => {
            handleModelChange(model);
          }}
        />
      ),
    },
    // {
    //   label: '数据库',
    //   key: 'dataBase',
    //   children: <DatabaseSection />,
    // },
    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection permissionTarget={'domain'} />,
    },
  ].filter((item) => {
    const target = domainList.find((domain) => domain.id === selectDomainId);
    if (target?.hasEditPermission) {
      return true;
    }
    return item.key !== 'permissonSetting';
  });

  const isModelItem = [
    {
      label: '画布',
      key: 'xflow',
      children: (
        <div style={{ width: '100%', marginTop: -20 }}>
          <SemanticGraphCanvas />
        </div>
      ),
    },

    {
      label: '数据源',
      key: 'dataSource',
      children: <ClassDataSourceTable />,
    },
    {
      label: '维度',
      key: 'dimenstion',
      children: <ClassDimensionTable />,
    },
    {
      label: '指标',
      key: 'metric',
      children: <ClassMetricTable />,
    },
    {
      label: '实体',
      key: 'entity',
      children: <EntitySettingSection />,
    },
    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection permissionTarget={'model'} />,
    },
    {
      label: '问答设置',
      key: 'chatSetting',
      children: <ChatSettingSection />,
    },
    {
      label: '推荐问题',
      key: 'recommendedQuestions',
      children: <RecommendedQuestionsSection />,
    },
  ].filter((item) => {
    if (window.RUNNING_ENV === 'semantic') {
      return !['chatSetting', 'recommendedQuestions'].includes(item.key);
    }
    return item;
  });

  return (
    <>
      <Tabs
        className={styles.tab}
        items={!isModel ? tabItem : isModelItem}
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
}))(DomainManagerTab);

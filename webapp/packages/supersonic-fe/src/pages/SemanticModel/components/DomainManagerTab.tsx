import { Tabs, Breadcrumb, Space } from 'antd';
import React from 'react';
import { connect, history } from 'umi';

import ClassDimensionTable from './ClassDimensionTable';
import ClassMetricTable from './ClassMetricTable';
import PermissionSection from './Permission/PermissionSection';
import ChatSettingSection from '../ChatSetting/ChatSettingSection';
import OverView from './OverView';
import styles from './style.less';
import type { StateType } from '../model';
import { HomeOutlined, FundViewOutlined } from '@ant-design/icons';
import { ISemantic } from '../data';
import SemanticGraphCanvas from '../SemanticGraphCanvas';
import RecommendedQuestionsSection from '../components/Entity/RecommendedQuestionsSection';
import DatabaseTable from '../components/Database/DatabaseTable';

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
  const defaultTabKey = 'dimenstion';
  const { selectDomainId, domainList, selectModelId, selectModelName, selectDomainName } =
    domainManger;

  const tabItem = [
    {
      label: '模型管理',
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
    {
      label: '画布',
      key: 'xflow',
      children: (
        <div style={{ width: '100%' }}>
          <SemanticGraphCanvas />
        </div>
      ),
    },
    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection permissionTarget={'domain'} />,
    },
    {
      label: '数据库管理',
      key: 'database',
      children: <DatabaseTable />,
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
      <Breadcrumb
        className={styles.breadcrumb}
        separator=""
        items={[
          {
            path: `/webapp/model/${selectDomainId}/0/overview`,
            title: (
              <Space
                onClick={() => {
                  onBackDomainBtnClick?.();
                }}
                style={selectModelName ? {} : { color: '#296df3', fontWeight: 'bold' }}
              >
                <HomeOutlined />
                <span>{selectDomainName}</span>
              </Space>
            ),
          },
          {
            type: 'separator',
            separator: selectModelName ? '/' : '',
          },
          {
            title: selectModelName ? (
              <Space
                onClick={() => {
                  history.push(`/model/${selectDomainId}/${selectModelId}/`);
                }}
                style={{ color: '#296df3' }}
              >
                <FundViewOutlined style={{ position: 'relative', top: '2px' }} />
                <span>{selectModelName}</span>
              </Space>
            ) : undefined,
          },
        ]}
      />
      <Tabs
        className={styles.tab}
        items={!isModel ? tabItem : isModelItem}
        activeKey={activeKey || defaultTabKey}
        destroyInactiveTabPane
        size="large"
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

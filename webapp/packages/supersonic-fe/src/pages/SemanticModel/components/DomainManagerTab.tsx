import { Tabs, Breadcrumb, Space } from 'antd';
import React, { useRef, useEffect } from 'react';
import { connect, history } from 'umi';

import ClassDimensionTable from './ClassDimensionTable';
import ClassMetricTable from './ClassMetricTable';
import PermissionSection from './Permission/PermissionSection';
// import ClassTagTable from '../Insights/components/ClassTagTable';
import TagObjectTable from '../Insights/components/TagObjectTable';

import OverView from './OverView';
import styles from './style.less';
import type { StateType } from '../model';
import { HomeOutlined, FundViewOutlined } from '@ant-design/icons';
import { ISemantic } from '../data';
import SemanticGraphCanvas from '../SemanticGraphCanvas';
import HeadlessFlows from '../HeadlessFlows';
import SemanticFlows from '../SemanticFlows';
import RecommendedQuestionsSection from '../components/Entity/RecommendedQuestionsSection';
import View from '../View';
// import DatabaseTable from '../components/Database/DatabaseTable';

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
  const initState = useRef<boolean>(false);
  const defaultTabKey = 'metric';
  const { selectDomainId, selectModelId, selectModelName, selectDomainName, domainData } =
    domainManger;

  useEffect(() => {
    initState.current = false;
  }, [selectModelId]);

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
      label: '数据集管理',
      key: 'dataSetManange',
      children: (
        <View
          modelList={modelList}
          onModelChange={(model) => {
            handleModelChange(model);
          }}
        />
      ),
    },
    {
      label: '标签对象管理',
      key: 'tagObjectManange',
      hidden: !!domainData?.parentId,
      children: <TagObjectTable />,
    },
    {
      label: '画布',
      key: 'xflow',
      children: (
        <div style={{ width: '100%' }}>
          <SemanticGraphCanvas />
          {/* <HeadlessFlows /> */}
        </div>
      ),
    },
    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection permissionTarget={'domain'} />,
    },
    // {
    //   label: '数据库管理',
    //   key: 'database',
    //   children: <DatabaseTable />,
    // },
  ].filter((item) => {
    if (item.hidden) {
      return false;
    }
    if (domainData?.hasEditPermission) {
      return true;
    }
    return item.key !== 'permissonSetting';
  });

  const isModelItem = [
    {
      label: '指标管理',
      key: 'metric',
      children: (
        <ClassMetricTable
          onEmptyMetricData={() => {
            if (!initState.current) {
              initState.current = true;
              onMenuChange?.('dimenstion');
            }
          }}
        />
      ),
    },
    {
      label: '维度管理',
      key: 'dimenstion',
      children: <ClassDimensionTable />,
    },
    // {
    //   label: '标签管理',
    //   key: 'tag',
    //   children: <ClassTagTable />,
    // },

    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection permissionTarget={'model'} />,
    },
    // {
    //   label: '问答设置',
    //   key: 'chatSetting',
    //   children: <ChatSettingSection />,
    // },
    {
      label: '推荐问题',
      key: 'recommendedQuestions',
      children: <RecommendedQuestionsSection />,
    },
  ].filter((item) => {
    if (window.RUNNING_ENV === 'headless') {
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

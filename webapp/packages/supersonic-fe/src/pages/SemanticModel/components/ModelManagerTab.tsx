import { Tabs, Breadcrumb, Space, Radio } from 'antd';
import React, { useRef, useEffect, useState } from 'react';
import { history, useModel } from '@umijs/max';
import ClassDimensionTable from './ClassDimensionTable';
import ClassMetricTable from './ClassMetricTable';
import PermissionSection from './Permission/PermissionSection';
import TagObjectTable from '../Insights/components/TagObjectTable';
import TermTable from '../components/Term/TermTable';
import OverView from './OverView';
import styles from './style.less';
import { HomeOutlined, FundViewOutlined } from '@ant-design/icons';
import { ISemantic } from '../data';
import SemanticGraphCanvas from '../SemanticGraphCanvas';
import Dimension from '../Dimension';
import ModelMetric from '../components/ModelMetric';
import View from '../View';

type Props = {
  activeKey: string;
  modelList: ISemantic.IModelItem[];
  onMenuChange?: (menuKey: string) => void;
};
const ModelManagerTab: React.FC<Props> = ({ activeKey, onMenuChange }) => {
  const initState = useRef<boolean>(false);
  const defaultTabKey = 'metric';
  const modelModel = useModel('SemanticModel.modelData');

  const { selectModelId } = modelModel;

  useEffect(() => {
    initState.current = false;
  }, [selectModelId]);

  const isModelItem = [
    {
      label: '指标管理',
      key: 'metric',
      // children: <ModelMetric />,
      children: (
        <ClassMetricTable
          onEmptyMetricData={() => {
            if (!initState.current) {
              initState.current = true;
              onMenuChange?.('dimension');
            }
          }}
        />
      ),
    },
    {
      label: '维度管理',
      key: 'dimension',
      children: <ClassDimensionTable />,
      // children: <Dimension />,
    },
    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection permissionTarget={'model'} />,
    },
  ];

  const getActiveKey = () => {
    const key = activeKey || defaultTabKey;
    const tabItems = isModelItem;
    const tabItemsKeys = tabItems.map((item) => item.key);
    if (!tabItemsKeys.includes(key)) {
      return tabItemsKeys[0];
    }
    return key;
  };

  return (
    <div>
      <Tabs
        className={styles.tab}
        items={isModelItem}
        activeKey={getActiveKey()}
        size="large"
        onChange={(menuKey: string) => {
          onMenuChange?.(menuKey);
        }}
      />
    </div>
  );
};

export default ModelManagerTab;

import { Tabs, Radio } from 'antd';
import React, { useRef, useEffect, useState } from 'react';
import { useModel } from '@umijs/max';
import PermissionSection from './Permission/PermissionSection';
import TagObjectTable from '../Insights/components/TagObjectTable';
import TermTable from '../components/Term/TermTable';
import OverView from './OverView';
import styles from './style.less';
import SemanticGraphCanvas from '../SemanticGraphCanvas';
import View from '../View';

type Props = {
  activeKey: string;
  onMenuChange?: (menuKey: string) => void;
};
const DomainManagerTab: React.FC<Props> = ({ activeKey, onMenuChange }) => {
  const initState = useRef<boolean>(false);
  const defaultTabKey = 'metric';

  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');

  const { selectDomainId, selectDomain: domainData } = domainModel;
  const { selectModelId, modelList } = modelModel;

  useEffect(() => {
    initState.current = false;
  }, [selectModelId]);

  const [showModelType, setShowModelType] = useState<string>('list');
  const tabItem = [
    {
      label: '数据集管理',
      key: 'overview',
      hidden: !!domainData?.parentId,
      children: <View />,
    },
    {
      label: '模型管理',
      key: 'modelManage',
      children:
        showModelType === 'list' ? (
          <OverView
            modelList={modelList}
            // onModelChange={(model) => {
            //   handleModelChange(model);
            // }}
          />
        ) : (
          <div style={{ width: '100%' }} key={selectDomainId}>
            <SemanticGraphCanvas />
          </div>
        ),
    },

    {
      label: '标签对象管理',
      key: 'tagObjectManage',
      hidden: !!!process.env.SHOW_TAG ? true : !!domainData?.parentId,
      children: <TagObjectTable />,
    },
    {
      label: '术语管理',
      key: 'termManage',
      hidden: !!domainData?.parentId,
      children: <TermTable />,
    },
    {
      label: '权限管理',
      key: 'permissonSetting',
      hidden: !!domainData?.parentId,
      children: <PermissionSection permissionTarget={'domain'} />,
    },
  ].filter((item) => {
    if (item.hidden) {
      return false;
    }
    if (domainData?.hasEditPermission) {
      return true;
    }
    return item.key !== 'permissonSetting';
  });

  const getActiveKey = () => {
    const key = activeKey || defaultTabKey;
    const tabItems = tabItem;
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
        items={tabItem}
        activeKey={getActiveKey()}
        tabBarExtraContent={{
          right:
            getActiveKey() === 'modelManage' ? (
              <Radio.Group
                buttonStyle="solid"
                value={showModelType}
                size="small"
                style={{ marginRight: 25 }}
                onChange={(e) => {
                  const showType = e.target.value;
                  setShowModelType(showType);
                }}
              >
                {showModelType}
                <Radio.Button value="list">列表</Radio.Button>
                <Radio.Button value="canvas">画布</Radio.Button>
              </Radio.Group>
            ) : undefined,
        }}
        size="large"
        onChange={(menuKey: string) => {
          onMenuChange?.(menuKey);
        }}
      />
    </div>
  );
};

export default DomainManagerTab;

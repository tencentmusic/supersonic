import { Outlet } from '@umijs/max';
import { Tabs, Breadcrumb, Space, Radio } from 'antd';
import React, { useRef, useEffect, useState } from 'react';
import { history, useModel } from '@umijs/max';
import { HomeOutlined, FundViewOutlined } from '@ant-design/icons';
import styles from './components/style.less';

const OverviewContainerRight: React.FC = () => {
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');

  const { selectDomainId, selectDomainName, selectDomain: domainData } = domainModel;
  const { selectModelId, selectModelName, setSelectModel } = modelModel;

  return (
    <>
      <Breadcrumb
        className={styles.breadcrumb}
        separator=""
        items={[
          {
            title: (
              <Space
                onClick={() => {
                  // onBackDomainBtnClick?.();
                  setSelectModel(undefined);
                  history.push(`/model/${selectDomainId}/overview`);
                }}
                style={
                  selectModelName ? { cursor: 'pointer' } : { color: '#296df3', fontWeight: 'bold' }
                }
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
                  history.push(`/model/manager/${selectDomainId}/${selectModelId}/`);
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
      <Outlet />
    </>
  );
};

export default OverviewContainerRight;

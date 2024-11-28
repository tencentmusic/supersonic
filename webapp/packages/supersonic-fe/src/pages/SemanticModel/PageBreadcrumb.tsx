import { Tabs, Breadcrumb, Space, Radio } from 'antd';
import React, { useRef, useEffect, useState } from 'react';
import { history, useModel } from '@umijs/max';
import { HomeOutlined, FundViewOutlined } from '@ant-design/icons';
import styles from './components/style.less';
import { toDomainList, toModelList } from '@/pages/SemanticModel/utils';

const PageBreadcrumb: React.FC = () => {
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const metricModel = useModel('SemanticModel.metricData');
  const { selectDomainId, selectDomainName, selectDomain: domainData } = domainModel;
  const { selectModelId, selectModelName, setSelectModel } = modelModel;

  const { selectMetric, setSelectMetric } = metricModel;

  const items = [
    {
      title: (
        <Space
          onClick={() => {
            setSelectModel(undefined);
            toDomainList(selectDomainId, 'overview');
          }}
        >
          <HomeOutlined />
          <span>{selectDomainName}</span>
        </Space>
      ),
    },
  ];

  if (selectModelName) {
    items.push(
      {
        type: 'separator',
        separator: '/',
      },
      {
        title: (
          <Space
            onClick={() => {
              setSelectMetric(undefined);
              toModelList(selectDomainId, selectModelId);
            }}
          >
            <FundViewOutlined style={{ position: 'relative', top: '2px' }} />
            <span>{selectModelName}</span>
          </Space>
        ),
      },
    );
  }

  if (selectMetric?.name) {
    items.push(
      {
        type: 'separator',
        separator: '/',
      },
      {
        title: selectMetric?.name ? (
          <Space>
            <FundViewOutlined style={{ position: 'relative', top: '2px' }} />
            <span>{selectMetric.name}</span>
          </Space>
        ) : undefined,
      },
    );
  }
  return (
    <>
      <Breadcrumb className={styles.breadcrumb} separator="" items={items} />
    </>
  );
};

export default PageBreadcrumb;

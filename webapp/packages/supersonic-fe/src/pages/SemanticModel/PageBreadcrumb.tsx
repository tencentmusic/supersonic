import { Tabs, Breadcrumb, Space, Radio } from 'antd';
import React, { useRef, useEffect, useState } from 'react';
import { history, useModel } from '@umijs/max';
import {
  HomeOutlined,
  FundViewOutlined,
  BarChartOutlined,
  LineChartOutlined,
} from '@ant-design/icons';
import styles from './components/style.less';
import { toDomainList, toModelList } from '@/pages/SemanticModel/utils';

const PageBreadcrumb: React.FC = () => {
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const metricModel = useModel('SemanticModel.metricData');
  const dimensionModel = useModel('SemanticModel.dimensionData');
  const { selectDomainId, selectDomainName, selectDataSet, setSelectDataSet } = domainModel;
  const { selectModelId, selectModelName, setSelectModel } = modelModel;
  const { selectDimension, setSelectDimension } = dimensionModel;

  const { selectMetric, setSelectMetric } = metricModel;

  const items = [
    {
      title: (
        <Space
          onClick={() => {
            setSelectModel(undefined);
            setSelectDimension(undefined);
            setSelectMetric(undefined);
            setSelectDataSet(undefined);
            toDomainList(selectDomainId, 'overview');
          }}
        >
          <HomeOutlined />
          <span>{selectDomainName}</span>
        </Space>
      ),
    },
  ];

  if (selectDataSet) {
    items.push(
      {
        type: 'separator',
        separator: '/',
      },
      {
        title: (
          <Space onClick={() => {}}>
            <FundViewOutlined style={{ position: 'relative', top: '2px' }} />
            <span>{selectDataSet.name}</span>
          </Space>
        ),
      },
    );
  }

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
              setSelectDimension(undefined);
              setSelectMetric(undefined);
              if (selectDimension) {
                toModelList(selectDomainId, selectModelId!, 'dimension');
                return;
              }
              toModelList(selectDomainId, selectModelId!, 'metric');
            }}
          >
            <FundViewOutlined style={{ position: 'relative', top: '2px' }} />
            <span>{selectModelName}</span>
          </Space>
        ),
      },
    );
  }

  if (selectDimension) {
    items.push(
      {
        type: 'separator',
        separator: '/',
      },
      {
        title: (
          <Space onClick={() => {}}>
            <BarChartOutlined style={{ position: 'relative', top: '2px' }} />
            <span>{selectDimension.name}</span>
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
            <LineChartOutlined style={{ position: 'relative', top: '2px' }} />
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

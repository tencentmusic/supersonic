import { message } from 'antd';
import React, { useState, useEffect } from 'react';
import { getMetricData } from '../service';
import { useParams, useModel, Helmet } from '@umijs/max';
import { BASE_TITLE } from '@/common/constants';
import { ISemantic } from '../data';
import MetricInfoCreateForm from './components/MetricInfoCreateForm';
import DetailContainer from '../components/DetailContainer';
import DetailSider from '../components/DetailContainer/DetailSider';
import { ProjectOutlined, ConsoleSqlOutlined } from '@ant-design/icons';
import { MetricSettingKey, MetricSettingWording } from './constants';

type Props = Record<string, any>;

const MetricDetail: React.FC<Props> = () => {
  const params: any = useParams();
  const metricId = +params.metricId;
  const modelId = +params.modelId;
  const domainId = +params.domainId;
  const [metircData, setMetircData] = useState<ISemantic.IMetricItem>();
  const metricModel = useModel('SemanticModel.metricData');
  const { setSelectMetric } = metricModel;
  const [settingKey, setSettingKey] = useState<MetricSettingKey>(MetricSettingKey.BASIC);

  useEffect(() => {
    if (!metricId) {
      return;
    }
    queryMetricData(metricId);
  }, [metricId]);

  useEffect(() => {
    return () => {
      setSelectMetric(undefined);
    };
  }, []);

  const queryMetricData = async (metricId: number) => {
    if (!metricId) {
      return;
    }
    const { code, data, msg } = await getMetricData(metricId);
    if (code === 200) {
      setMetircData({ ...data });
      setSelectMetric({ ...data });
      return;
    }
    message.error(msg);
  };

  const settingList = [
    {
      icon: <ProjectOutlined />,
      key: MetricSettingKey.BASIC,
      text: MetricSettingWording[MetricSettingKey.BASIC],
    },
    {
      icon: <ConsoleSqlOutlined />,
      key: MetricSettingKey.SQL_CONFIG,
      text: MetricSettingWording[MetricSettingKey.SQL_CONFIG],
    },
  ];

  return (
    <>
      <Helmet
        title={`${
          metircData?.id ? `[指标]${metircData?.name}-${BASE_TITLE}` : `新建指标-${BASE_TITLE}`
        }`}
      />
      <DetailContainer
        siderNode={
          <DetailSider
            menuKey={MetricSettingKey.BASIC}
            menuList={settingList}
            detailData={metircData}
            onMenuKeyChange={(key: string) => {
              setSettingKey(key);
            }}
          />
        }
        containerNode={
          <MetricInfoCreateForm
            settingKey={settingKey}
            metricItem={metircData}
            modelId={metircData?.modelId || modelId}
            domainId={metircData?.domainId || domainId}
          />
        }
      />
    </>
  );
};

export default MetricDetail;

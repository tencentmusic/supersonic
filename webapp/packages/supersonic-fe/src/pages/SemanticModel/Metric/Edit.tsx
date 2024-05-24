import { message, Tabs, Button, Space } from 'antd';
import React, { useState, useEffect } from 'react';
import { getMetricData, getDimensionList, getDrillDownDimension } from '../service';
import { connect, useParams, history } from 'umi';
import type { StateType } from '../model';
import styles from './style.less';
import { ArrowLeftOutlined } from '@ant-design/icons';
import MetricTrendSection from '@/pages/SemanticModel/Metric/components/MetricTrendSection';
import { ISemantic } from '../data';
import MetricBasicInfo from './components/MetricBasicInfo';
import DimensionAndMetricRelationModal from '../components/DimensionAndMetricRelationModal';
import MetricInfoEditSider from './MetricInfoEditSider';
import MetricInfoCreateForm from './components/MetricInfoCreateForm';
import { MetricSettingKey, MetricSettingWording } from './constants';

type Props = Record<string, any>;

const MetricDetail: React.FC<Props> = () => {
  const params: any = useParams();
  const metricId = params.metricId;
  const [metricRelationModalOpenState, setMetricRelationModalOpenState] = useState<boolean>(false);
  const [metircData, setMetircData] = useState<ISemantic.IMetricItem>();
  const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>([]);
  const [drillDownDimension, setDrillDownDimension] = useState<ISemantic.IDrillDownDimensionItem[]>(
    [],
  );
  const [relationDimensionOptions, setRelationDimensionOptions] = useState<
    { value: string; label: string; modelId: number }[]
  >([]);

  const [settingKey, setSettingKey] = useState<MetricSettingKey>(MetricSettingKey.BASIC);

  useEffect(() => {
    if (!metricId) {
      return;
    }
    queryMetricData(metricId);
    queryDrillDownDimension(metricId);
  }, [metricId]);

  const queryMetricData = async (metricId: string) => {
    const { code, data, msg } = await getMetricData(metricId);
    if (code === 200) {
      setMetircData({ ...data });
      return;
    }
    message.error(msg);
  };

  const queryDrillDownDimension = async (metricId: number) => {
    const { code, data, msg } = await getDrillDownDimension(metricId);
    if (code === 200 && Array.isArray(data)) {
      setDrillDownDimension(data);
      const ids = data.map((item) => item.dimensionId);
      queryDimensionList(ids);
      return data;
    } else {
      setDimensionList([]);
      setRelationDimensionOptions([]);
    }
    if (code !== 200) {
      message.error(msg);
    }
    return [];
  };

  const queryDimensionList = async (ids: number[]) => {
    if (!(Array.isArray(ids) && ids.length > 0)) {
      setRelationDimensionOptions([]);
      return;
    }
    const { code, data, msg } = await getDimensionList({ ids });
    if (code === 200 && Array.isArray(data?.list)) {
      setDimensionList(data.list);
      setRelationDimensionOptions(
        data.list.map((item: ISemantic.IMetricItem) => {
          return { label: item.name, value: item.bizName, modelId: item.modelId };
        }),
      );
      return data.list;
    }
    message.error(msg);
    return [];
  };

  return (
    <>
      <div className={styles.metricEditWrapper}>
        <div className={styles.metricDetail}>
          <div className={styles.siderContainer}>
            <MetricInfoEditSider
              onSettingKeyChange={(key: string) => {
                setSettingKey(key);
              }}
              metircData={metircData}
            />
          </div>
          <div className={styles.tabContainer}>
            {/* {metircData && ( */}
            <MetricInfoCreateForm
              settingKey={settingKey}
              // domainId={metircData?.domainId}
              // modelId={metircData?.modelId}
              metricItem={metircData}
            />
            {/* )} */}
          </div>
        </div>
        <DimensionAndMetricRelationModal
          metricItem={metircData}
          relationsInitialValue={drillDownDimension}
          open={metricRelationModalOpenState}
          onCancel={() => {
            setMetricRelationModalOpenState(false);
          }}
          onSubmit={(relations) => {
            queryMetricData(metricId);
            queryDrillDownDimension(metricId);
            setMetricRelationModalOpenState(false);
          }}
        />
      </div>
    </>
  );
};

export default MetricDetail;

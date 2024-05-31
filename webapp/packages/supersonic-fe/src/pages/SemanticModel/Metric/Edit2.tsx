import { message } from 'antd';
import React, { useState, useEffect } from 'react';
import { getMetricData } from '../service';
import { useParams } from '@umijs/max';
import styles from './style.less';
import { ISemantic } from '../data';
import MetricInfoEditSider from './MetricInfoEditSider';
import MetricInfoCreateForm from './components/MetricInfoCreateForm';
import { MetricSettingKey } from './constants';

type Props = Record<string, any>;

const MetricDetail: React.FC<Props> = () => {
  const params: any = useParams();
  const metricId = params.metricId;
  const [metircData, setMetircData] = useState<ISemantic.IMetricItem>();

  const [settingKey, setSettingKey] = useState<MetricSettingKey>(MetricSettingKey.BASIC);

  useEffect(() => {
    if (!metricId) {
      return;
    }
    queryMetricData(metricId);
  }, [metricId]);

  const queryMetricData = async (metricId: string) => {
    const { code, data, msg } = await getMetricData(metricId);
    if (code === 200) {
      setMetircData({ ...data });
      return;
    }
    message.error(msg);
  };

  return (
    <>
      <div className={styles.metricWrapper}>
        <div className={styles.metricDetail}>
          {/* <div className={styles.siderContainer}>
            <MetricInfoEditSider
              onSettingKeyChange={(key: string) => {
                setSettingKey(key);
              }}
              metircData={metircData}
            />
          </div> */}
          <div className={styles.tabContainer}>
            <MetricInfoCreateForm settingKey={settingKey} metricItem={metircData} />
          </div>
        </div>
      </div>
    </>
  );
};

export default MetricDetail;

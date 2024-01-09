import { message } from 'antd';
import React, { useState, useEffect } from 'react';
import { getMetricData } from '../service';
import { connect, useParams } from 'umi';
import type { StateType } from '../model';
import styles from './style.less';
import MetricTrendSection from '@/pages/SemanticModel/Metric/components/MetricTrendSection';
import { ISemantic } from '../data';
import DimensionAndMetricRelationModal from '../components/DimensionAndMetricRelationModal';
import MetricInfoSider from './MetricInfoSider';

type Props = {
  metircData: any;
  domainManger: StateType;
  onNodeChange: (params?: { eventName?: string }) => void;
  onEditBtnClick?: (metircData: any) => void;
  [key: string]: any;
};

const siderStyle: React.CSSProperties = {
  backgroundColor: '#fff',
  width: 450,
  minHeight: '100vh',
  boxShadow:
    '6px 0 16px 0 rgba(0, 0, 0, 0.08), 3px 0 6px -4px rgba(0, 0, 0, 0.12), 9px 0 28px 8px rgba(0, 0, 0, 0.05)',
};

const MetricDetail: React.FC<Props> = ({ domainManger }) => {
  const params: any = useParams();
  const metricId = params.metricId;
  const [metricRelationModalOpenState, setMetricRelationModalOpenState] = useState<boolean>(false);
  const [metircData, setMetircData] = useState<ISemantic.IMetricItem>();
  useEffect(() => {
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
      <div className={styles.metricDetailWrapper}>
        <div className={styles.metricDetail}>
          <div className={styles.tabContainer}>
            <MetricTrendSection metircData={metircData} />
          </div>
          <div style={siderStyle}>
            <MetricInfoSider
              onDimensionRelationBtnClick={() => {
                setMetricRelationModalOpenState(true);
              }}
            />
          </div>
        </div>
      </div>
      <DimensionAndMetricRelationModal
        metricItem={metircData}
        relationsInitialValue={metircData?.relateDimension?.drillDownDimensions}
        open={metricRelationModalOpenState}
        onCancel={() => {
          setMetricRelationModalOpenState(false);
        }}
        onSubmit={(relations) => {
          queryMetricData(metricId);
          setMetricRelationModalOpenState(false);
        }}
      />
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(MetricDetail);

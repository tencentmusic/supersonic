import React, { useState, useEffect } from 'react';

import { getTagValueDistribution } from '@/pages/SemanticModel/service';

import TagTable from './Table';

import { ISemantic } from '../../data';

import { ProCard } from '@ant-design/pro-components';
import { TagGraph } from 'supersonic-insights-flow-components';
import styles from '../style.less';

const { TagValueBar } = TagGraph;

type Props = {
  relationDimensionOptions: { value: string; label: string; modelId: number }[];
  dimensionList: ISemantic.IDimensionItem[];
  tagData?: ISemantic.ITagItem;
  [key: string]: any;
};

const TagTrendSection: React.FC<Props> = ({ tagData }) => {
  const [tagTrendLoading, setTagTrendLoading] = useState<boolean>(false);

  const [barData, setBarData] = useState<any[]>([]);
  const [tableColumnConfig, setTableColumnConfig] = useState<ColumnConfig[]>([]);

  const queryTagValueDistribution = async (params: any) => {
    setTagTrendLoading(true);
    const { data, code } = await getTagValueDistribution({
      id: params.id,
      dateConf: {
        unit: 5,
      },
    });
    setTagTrendLoading(false);
    if (code === 200 && Array.isArray(data?.valueDistributionList)) {
      const { valueDistributionList, name } = data;
      const distributionOptions = valueDistributionList.map((item) => {
        const { valueMap, valueCount } = item;
        return {
          type: valueMap,
          value: valueCount,
        };
      });
      const columns = [
        {
          dataIndex: 'type',
          title: name,
        },
        {
          dataIndex: 'value',
          title: '样本数',
        },
      ];
      setTableColumnConfig(columns);
      setBarData(distributionOptions);
    }
  };

  useEffect(() => {
    if (tagData?.id) {
      queryTagValueDistribution({
        ...tagData,
      });
    }
  }, [tagData]);

  return (
    <div className={styles.metricTrendSection}>
      <div className={styles.sectionBox}>
        <ProCard
          size="small"
          title={
            <>
              <span>数据趋势</span>
              {/* {authMessage && <div style={{ color: '#d46b08' }}>{authMessage}</div>} */}
            </>
          }
        >
          <TagValueBar height={400} data={barData} />
        </ProCard>
      </div>

      <div className={styles.sectionBox} style={{ paddingBottom: 0 }}>
        <ProCard size="small" title="数据明细" collapsible>
          <div style={{ minHeight: '450px' }}>
            <TagTable
              loading={tagTrendLoading}
              columnConfig={tableColumnConfig}
              dataSource={barData}
            />
          </div>
        </ProCard>
      </div>
    </div>
  );
};

export default TagTrendSection;

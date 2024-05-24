import { message, Tabs, Button, Space } from 'antd';
import React, { useState, useEffect } from 'react';
import { getTagData } from '../service';
import { useParams, history } from 'umi';
import styles from './style.less';
import { ArrowLeftOutlined } from '@ant-design/icons';
import TagTrendSection from './components/TagTrendSection';
import { ISemantic } from '../data';
import TagInfoSider from './components/TagInfoSider';
import { getDimensionList, queryMetric } from '../service';
import type { TabsProps } from 'antd';

type Props = Record<string, any>;

const TagDetail: React.FC<Props> = () => {
  const params: any = useParams();
  const tagId = params.tagId;
  const [tagData, setTagData] = useState<ISemantic.ITagItem>();
  const [dimensionMap, setDimensionMap] = useState<Record<string, ISemantic.IDimensionItem>>({});

  const [metricMap, setMetricMap] = useState<Record<string, ISemantic.IMetricItem>>({});

  const [relationDimensionOptions, setRelationDimensionOptions] = useState<
    { value: string; label: string; modelId: number }[]
  >([]);

  useEffect(() => {
    queryTagData(tagId);
  }, [tagId]);

  const queryTagData = async (tagId: number) => {
    const { code, data, msg } = await getTagData(tagId);
    if (code === 200) {
      queryDimensionList(data.modelId);
      queryMetricList(data.modelId);
      setTagData({ ...data });
      return;
    }
    message.error(msg);
  };

  const tabItems: TabsProps['items'] = [
    {
      key: 'trend',
      label: '图表',
      children: (
        // <></>
        <TagTrendSection
          tagData={tagData}
          relationDimensionOptions={relationDimensionOptions}
          dimensionList={[]}
        />
      ),
    },
    // {
    //   key: 'metricCaliberInput',
    //   label: '基础信息',
    //   children: <></>,
    // },
    // {
    //   key: 'metricDataRemark',
    //   label: '备注',
    //   children: <></>,
    // },
  ];

  const queryDimensionList = async (modelId: number) => {
    const { code, data, msg } = await getDimensionList({ modelId });
    if (code === 200 && Array.isArray(data?.list)) {
      const { list } = data;
      setDimensionMap(
        list.reduce(
          (infoMap: Record<string, ISemantic.IDimensionItem>, item: ISemantic.IDimensionItem) => {
            infoMap[`${item.id}`] = item;
            return infoMap;
          },
          {},
        ),
      );
    } else {
      message.error(msg);
    }
  };

  const queryMetricList = async (modelId: number) => {
    const { code, data, msg } = await queryMetric({
      modelId: modelId,
    });
    const { list } = data || {};
    if (code === 200) {
      setMetricMap(
        list.reduce(
          (infoMap: Record<string, ISemantic.IMetricItem>, item: ISemantic.IMetricItem) => {
            infoMap[`${item.id}`] = item;
            return infoMap;
          },
          {},
        ),
      );
    } else {
      message.error(msg);
    }
  };

  return (
    <>
      <div className={styles.tagDetailWrapper}>
        <div className={styles.tagDetail}>
          <div className={styles.tabContainer}>
            <Tabs
              defaultActiveKey="trend"
              items={tabItems}
              tabBarExtraContent={{
                right: (
                  <Button
                    size="middle"
                    type="link"
                    key="backListBtn"
                    onClick={() => {
                      history.push('/tag/market');
                    }}
                  >
                    <Space>
                      <ArrowLeftOutlined />
                      返回列表页
                    </Space>
                  </Button>
                ),
              }}
              size="large"
              className={styles.tagDetailTab}
            />
          </div>
          <div className={styles.siderContainer}>
            <TagInfoSider
              dimensionMap={dimensionMap}
              metricMap={metricMap}
              // relationDimensionOptions={relationDimensionOptions}
              tagData={tagData}
            />
          </div>
        </div>
      </div>
    </>
  );
};

export default TagDetail;

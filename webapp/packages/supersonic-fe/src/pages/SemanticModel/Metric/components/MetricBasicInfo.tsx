import React, { useState, useEffect } from 'react';
import { Divider, Flex, Tag, Input, Table } from 'antd';
import { METRIC_DEFINE_TYPE } from '../../constant';
import styles from '../style.less';
import { ISemantic } from '../../data';

const { TextArea } = Input;
type Props = {
  metircData?: ISemantic.IMetricItem;
};

const MetricBasicInformation: React.FC<Props> = ({ metircData }) => {
  const [defineData, setDefineData] = useState<{
    name: string;
    listName: string;
    expr: string;
    list: any[];
    columns: any[];
  }>();

  const metricColumns = [
    {
      dataIndex: 'name',
      title: '指标名称',
    },
    {
      dataIndex: 'bizName',
      title: '英文名称',
    },
  ];

  const fieldColumns = [
    {
      dataIndex: 'fieldName',
      title: '字段名称',
    },
    {
      dataIndex: 'dataType',
      title: '字段类型',
    },
  ];

  const measureColumns = [
    {
      dataIndex: 'bizName',
      title: '度量名称',
      tooltip: '由模型名称_字段名称拼接而来',
    },
    {
      dataIndex: 'constraint',
      title: '限定条件',
      width: 250,
      tooltip:
        '该限定条件用于在计算指标时限定口径，作用于度量，所用于过滤的维度必须在创建模型的时候被标记为日期或者维度，不需要加where关键字。比如：维度A="值1" and 维度B="值2"',
      render: (_: any, record: any) => {
        const { constraint } = record;
        if (!constraint) {
          return '--';
        }
        return <TextArea readOnly value={constraint} />;
      },
    },
    {
      dataIndex: 'agg',
      title: '聚合函数',
      width: 80,
      render: (_: string) => {
        if (!_) {
          return '--';
        }
        return _;
      },
    },
  ];

  useEffect(() => {
    if (!metircData) {
      return;
    }
    const {
      metricDefineType,
      metricDefineByFieldParams,
      metricDefineByMeasureParams,
      metricDefineByMetricParams,
    } = metircData;
    switch (metricDefineType) {
      case METRIC_DEFINE_TYPE.FIELD:
        setDefineData({
          name: '按字段',
          listName: '字段列表',
          expr: metricDefineByFieldParams.expr,
          list: metricDefineByFieldParams.fields,
          columns: fieldColumns,
        });
        break;
      case METRIC_DEFINE_TYPE.MEASURE:
        setDefineData({
          name: '按度量',
          listName: '度量列表',
          expr: metricDefineByMeasureParams.expr,
          list: metricDefineByMeasureParams.measures,
          columns: measureColumns,
        });
        break;
      case METRIC_DEFINE_TYPE.METRIC:
        setDefineData({
          name: '按指标',
          listName: '指标列表',
          expr: metricDefineByMetricParams.expr,
          list: metricDefineByMetricParams.metrics,
          columns: metricColumns,
        });
        break;
      default:
        break;
    }
  }, [metircData]);

  return (
    <div className={styles.sectionBox} style={{ padding: '10px 20px' }}>
      <div className={styles.metricBasicInfo}>
        <Flex justify="space-between" align="center">
          <p className={styles.caliberSubTitle}>指标信息</p>
        </Flex>
        <Flex wrap="wrap">
          <div>
            <span className={styles.label}>指标ID：</span>
            {metircData?.id}
          </div>
          <div>
            <span className={styles.label}>中文名：</span>
            {metircData?.name}
          </div>
        </Flex>
        <Flex wrap="wrap">
          <div>
            <span className={styles.label}>英文名：</span>
            {metircData?.bizName}
          </div>
        </Flex>
        <Flex wrap="wrap">
          <div>
            <span className={styles.label}>别名：</span>
            {metircData?.alias &&
              metircData.alias.split(',').map((alias) => (
                <Tag color="blue" key={alias}>
                  {alias}
                </Tag>
              ))}
          </div>
        </Flex>
        <Flex wrap="wrap">
          <div>
            <span className={styles.label}>分类：</span>
            {Array.isArray(metircData?.classifications) &&
              metircData.classifications.map((className) => (
                <Tag color="blue" key={className}>
                  {className}
                </Tag>
              ))}
          </div>
        </Flex>
        <Divider />
        <Flex justify="space-between" align="center">
          <p className={styles.caliberSubTitle}>模型信息</p>
        </Flex>

        <Flex wrap="wrap">
          <div>
            <span className={styles.label}>模型名：</span>
            {metircData?.modelName}
          </div>
          <div>
            <span className={styles.label}>模型ID：</span> {metircData?.modelId}
          </div>
        </Flex>

        <Flex wrap="wrap">
          <div>
            <span className={styles.label}>模型英文名：</span>
            {metircData?.modelBizName}
          </div>
        </Flex>

        <Divider />

        <Flex justify="space-between" align="center">
          <p className={styles.caliberSubTitle}>定义信息</p>
        </Flex>
        <Flex wrap="wrap">
          <div>
            <span className={styles.label}>定义类型：</span>
            {defineData?.name}
          </div>
          <div>
            <span className={styles.label}>表达式：</span>
            {defineData?.expr}
          </div>
        </Flex>

        <div className={styles.label} style={{ marginBottom: 10 }}>
          {defineData?.listName}：
        </div>
        <Table
          className={styles.defineDataTable}
          columns={defineData?.columns}
          dataSource={defineData?.list}
          size="small"
          pagination={false}
        />
      </div>
    </div>
  );
};

export default MetricBasicInformation;

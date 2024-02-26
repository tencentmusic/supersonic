import React, { useState, useEffect } from 'react';
import { Form, Input, Modal, Table, Tag, message } from 'antd';
import TransTypeTag from '../../components/TransTypeTag';
import { SemanticNodeType } from '../../enum';
import { ISemantic } from '../../data';
import styles from '../../components/style.less';
import { StatusEnum } from '../../enum';
import { batchUpdateMetricStatus, batchUpdateDimensionStatus } from '../../service';

type Props = {
  open: boolean;
  tableDataSource: (ISemantic.IDimensionItem | ISemantic.IMetricItem)[];
  onOk?: () => void;
  onCancel?: () => void;
};

const EffectDimensionAndMetricTipsModal: React.FC<Props> = ({
  open,
  tableDataSource,
  onOk,
  onCancel,
}) => {
  const [loading, setLoading] = useState<boolean>(false);
  useEffect(() => {}, []);

  const queryBatchUpdateDimensionStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    setLoading(true);
    const { code, msg } = await batchUpdateDimensionStatus({
      ids,
      status,
    });
    setLoading(false);
    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const queryBatchUpdateMetricStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    const { code, msg } = await batchUpdateMetricStatus({
      ids,
      status,
    });
    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const updateState = async () => {
    const dimensionIds: React.Key[] = [];
    const metricIds: React.Key[] = [];
    tableDataSource.forEach((item) => {
      if (item.typeEnum === SemanticNodeType.DIMENSION) {
        dimensionIds.push(item.id);
      }
      if (item.typeEnum === SemanticNodeType.METRIC) {
        metricIds.push(item.id);
      }
    });
    if (dimensionIds.length > 0) {
      await queryBatchUpdateDimensionStatus(dimensionIds, StatusEnum.UNAVAILABLE);
    }
    if (metricIds.length > 0) {
      await queryBatchUpdateMetricStatus(metricIds, StatusEnum.UNAVAILABLE);
    }
    onOk?.();
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      width: 100,
    },
    {
      title: '英文名称',
      dataIndex: 'bizName',
      width: 100,
    },
    {
      title: '类型',
      dataIndex: 'typeEnum',
      width: 80,
      render: (transType: SemanticNodeType) => {
        return <TransTypeTag type={transType} />;
      },
    },
    {
      title: '创建人',
      width: 100,
      dataIndex: 'createdBy',
    },
  ];

  return (
    <Modal
      forceRender
      styles={{
        body: {
          padding: 12,
        },
      }}
      width={800}
      destroyOnClose
      title={`受影响的维度&指标`}
      maskClosable={false}
      open={open}
      onOk={() => {
        updateState();
      }}
      // footer={renderFooter()}
      onCancel={() => {
        onCancel?.();
      }}
    >
      <p
        className={styles.desc}
        style={{ border: 'unset', padding: 0, marginBottom: 20, marginLeft: 2 }}
      >
        检测到模型信息变更会对以下
        <Tag color="#2499ef14" className={styles.markerTag}>
          指标
        </Tag>
        和
        <Tag color="#2499ef14" className={styles.markerTag}>
          维度
        </Tag>
        产生影响。如确认保存，将会自动置为
        <Tag color="#2499ef14" className={styles.markerTag}>
          不可用状态
        </Tag>
        。
      </p>
      <Table
        size="small"
        dataSource={tableDataSource}
        columns={columns}
        rowKey="bizName"
        pagination={false}
        scroll={{ y: 500 }}
      />
    </Modal>
  );
};

export default EffectDimensionAndMetricTipsModal;

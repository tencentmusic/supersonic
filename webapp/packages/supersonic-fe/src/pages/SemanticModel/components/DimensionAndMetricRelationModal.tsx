import React, { useEffect, useState } from 'react';
import { Modal, Button } from 'antd';
import DimensionMetricRelationTableTransfer from './DimensionMetricRelationTableTransfer';
import { ISemantic } from '../data';

import FormItemTitle from '@/components/FormHelper/FormItemTitle';

type Props = {
  onCancel: () => void;
  open: boolean;
  metricItem: ISemantic.IMetricItem;
  relationsInitialValue?: ISemantic.IDrillDownDimensionItem[];
  onSubmit: (relations: ISemantic.IDrillDownDimensionItem[]) => void;
};

const DimensionAndMetricRelationModal: React.FC<Props> = ({
  open,
  metricItem,
  relationsInitialValue,
  onCancel,
  onSubmit,
}) => {
  const [relationList, setRelationList] = useState<ISemantic.IDrillDownDimensionItem[]>([]);

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          onClick={() => {
            onSubmit(relationList);
          }}
        >
          完成
        </Button>
      </>
    );
  };

  return (
    <>
      <Modal
        width={1200}
        destroyOnClose
        title={
          <FormItemTitle
            title={'维度关联'}
            subTitle={'注意：完成指标信息更新后，维度关联配置信息才会被保存'}
          />
        }
        maskClosable={false}
        open={open}
        footer={renderFooter()}
        onCancel={onCancel}
      >
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <DimensionMetricRelationTableTransfer
            metricItem={metricItem}
            relationsInitialValue={relationsInitialValue}
            onChange={(relations: ISemantic.IDrillDownDimensionItem[]) => {
              setRelationList(relations);
            }}
          />
        </div>
      </Modal>
    </>
  );
};

export default DimensionAndMetricRelationModal;

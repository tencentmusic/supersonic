import React, { useEffect, useState } from 'react';
import { Modal, Button, message } from 'antd';
import DimensionMetricRelationTableTransfer from './DimensionMetricRelationTableTransfer';
import { ISemantic } from '../data';
import { updateMetric } from '../service';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';

type Props = {
  onCancel: () => void;
  open: boolean;
  metricItem?: ISemantic.IMetricItem;
  relationsInitialValue?: ISemantic.IDrillDownDimensionItem[];
  onSubmit: (relations: ISemantic.IDrillDownDimensionItem[]) => void;
  onRefreshRelationData?: () => void;
};

const DimensionAndMetricRelationModal: React.FC<Props> = ({
  open,
  metricItem,
  relationsInitialValue,
  onCancel,
  onSubmit,
  onRefreshRelationData,
}) => {
  const [relationList, setRelationList] = useState<ISemantic.IDrillDownDimensionItem[]>([]);

  useEffect(() => {
    if (Array.isArray(relationsInitialValue)) {
      setRelationList(relationsInitialValue);
    }
  }, [relationsInitialValue]);

  const saveMetric = async (relationList: any) => {
    const queryParams = {
      ...metricItem,
      relateDimension: {
        ...(metricItem?.relateDimension || {}),
        drillDownDimensions: relationList,
      },
    };

    const { code, msg } = await updateMetric(queryParams);
    if (code === 200) {
      onSubmit(relationList);
      onRefreshRelationData?.();
      return;
    }
    message.error(msg);
  };

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          onClick={() => {
            if (metricItem?.id) {
              saveMetric(relationList);
            } else {
              onSubmit(relationList);
            }
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

import { Modal, message } from 'antd';
import React, { useState } from 'react';
import { SemanticNodeType } from '../../enum';
import { deleteDimension, deleteMetric, deleteDatasource } from '../../service';

type Props = {
  nodeData: any;
  onOkClick: () => void;
  onCancelClick: () => void;
  open: boolean;
};

const DeleteConfirmModal: React.FC<Props> = ({
  nodeData,
  onOkClick,
  onCancelClick,
  open = false,
}) => {
  const [confirmLoading, setConfirmLoading] = useState(false);
  const deleteNode = async () => {
    const { id, nodeType } = nodeData;
    let deleteQuery;
    if (nodeType === SemanticNodeType.DIMENSION) {
      deleteQuery = deleteDimension;
    }
    if (nodeType === SemanticNodeType.METRIC) {
      deleteQuery = deleteMetric;
    }
    if (nodeType === SemanticNodeType.DATASOURCE) {
      deleteQuery = deleteDatasource;
    }
    if (!deleteQuery) {
      message.error('当前节点类型不是维度，指标，模型中的一种，请确认节点数据');
      return;
    }
    setConfirmLoading(true);
    const { code, msg } = await deleteQuery(id);
    setConfirmLoading(false);
    if (code === 200) {
      onOkClick();
      message.success('删除成功!');
    } else {
      message.error(msg);
    }
  };

  const handleOk = () => {
    deleteNode();
  };

  return (
    <>
      <Modal
        title={'删除确认'}
        open={open}
        onOk={handleOk}
        confirmLoading={confirmLoading}
        onCancel={onCancelClick}
      >
        <>
          <span style={{ color: '#296DF3', fontWeight: 'bold' }}>{nodeData?.name}</span>
          将被删除，是否确认？
        </>
      </Modal>
    </>
  );
};

export default DeleteConfirmModal;

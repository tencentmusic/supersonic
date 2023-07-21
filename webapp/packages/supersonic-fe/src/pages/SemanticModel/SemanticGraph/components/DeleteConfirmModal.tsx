import { Button, Modal, message } from 'antd';
import React, { useState } from 'react';
import { SemanticNodeType } from '../../enum';
import { deleteDimension, deleteMetric } from '../../service';

type Props = {
  nodeData: any;
  nodeType: SemanticNodeType;
  onOkClick: () => void;
  onCancelClick: () => void;
  open: boolean;
};

const DeleteConfirmModal: React.FC<Props> = ({
  nodeData,
  nodeType,
  onOkClick,
  onCancelClick,
  open = false,
}) => {
  const [confirmLoading, setConfirmLoading] = useState(false);
  const deleteNode = async () => {
    setConfirmLoading(true);
    const { id } = nodeData;
    let deleteQuery = deleteDimension;
    if (nodeType === SemanticNodeType.METRIC) {
      deleteQuery = deleteMetric;
    }
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

import { Modal } from 'antd';
import React, { forwardRef, useImperativeHandle, useState } from 'react';
import { SqlInfoType } from '../../common/type';
import SqlItem from './SqlItem';

export type SqlItemModalHandle = {
  show: () => void;
};

type Props = {
  llmReq?: any;
  llmResp?: any;
  integrateSystem?: string;
  queryMode?: string;
  sqlInfo: SqlInfoType;
  sqlTimeCost?: number;
};

const SqlItemModal = forwardRef<SqlItemModalHandle, Props>((props, ref) => {
  const [open, setOpen] = useState(false);
  console.log('🚀 ~ SqlItemModal ~ open:', open);
  const { ...otherProps } = props;

  useImperativeHandle(ref, () => ({
    show: () => {
      setOpen(true);
    },
  }));

  return (
    <Modal
      open={open}
      destroyOnClose
      width={800}
      title="查看SQL"
      footer={null}
      onCancel={() => setOpen(false)}
      onClose={() => setOpen(false)}
      styles={{
        body: {
          paddingTop: 10,
        },
      }}
    >
      <SqlItem showIcon={false} showExpandIcon={false} defaultSqlType="querySQL" {...otherProps} />
    </Modal>
  );
});

export default SqlItemModal;

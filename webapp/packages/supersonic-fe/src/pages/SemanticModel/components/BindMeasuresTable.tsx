import React, { useEffect, useRef, useState } from 'react';
import { Button, Modal } from 'antd';
import type { IDataSource } from '../data';
import ProTable from '@ant-design/pro-table';
import type { ActionType, ProColumns } from '@ant-design/pro-table';
import { connect } from 'umi';
import type { Dispatch } from 'umi';
import type { StateType } from '../model';

export type CreateFormProps = {
  measuresList: any[];
  selectedMeasuresList: any[];
  onCancel: () => void;
  onSubmit: (selectMeasuresList: any[]) => void;
  createModalVisible: boolean;
  projectManger: StateType;
  dispatch: Dispatch;
};

const BindMeasuresTable: React.FC<CreateFormProps> = ({
  measuresList = [],
  selectedMeasuresList = [],
  onSubmit,
  onCancel,
  createModalVisible,
  projectManger,
}) => {
  const { searchParams = {} } = projectManger || {};
  const actionRef = useRef<ActionType>();

  const [selectedMeasuresKeys, setSelectedMeasuresKeys] = useState<string[]>(() => {
    return selectedMeasuresList.map((item: any) => {
      return item.bizName;
    });
  });
  const [selectMeasuresList, setSelectMeasuresList] = useState<IDataSource.IMeasuresItem[]>([]);

  const handleSubmit = async () => {
    onSubmit?.(selectMeasuresList);
  };

  const findMeasureItemByName = (bizName: string) => {
    return measuresList.find((item) => {
      return item.bizName === bizName;
    });
  };

  useEffect(() => {
    const selectedMeasures: IDataSource.IMeasuresItem[] = selectedMeasuresKeys.map((bizName) => {
      const item = findMeasureItemByName(bizName);
      return item;
    });
    setSelectMeasuresList([...selectedMeasures]);
  }, [selectedMeasuresKeys]);

  useEffect(() => {}, []);

  const columns: ProColumns[] = [
    {
      dataIndex: 'bizName',
      title: '度量名称',
    },
    {
      dataIndex: 'alias',
      title: '别名',
    },
    {
      dataIndex: 'agg',
      title: '算子类型',
    },
  ];
  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" onClick={handleSubmit}>
          将选中度量添加到指标
        </Button>
      </>
    );
  };

  const rowSelection = {
    selectedRowKeys: selectedMeasuresKeys,
    onChange: (_selectedRowKeys: any[]) => {
      setSelectedMeasuresKeys([..._selectedRowKeys]);
    },
  };

  return (
    <Modal
      width={800}
      destroyOnClose
      title="度量添加"
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={() => {
        onCancel();
      }}
    >
      <ProTable
        actionRef={actionRef}
        rowKey="bizName"
        rowSelection={rowSelection}
        columns={columns}
        params={{ ...searchParams }}
        pagination={false}
        dataSource={measuresList || []}
        size="small"
        search={false}
        options={false}
        scroll={{ y: 800 }}
      />
    </Modal>
  );
};

export default connect(({ projectManger }: { projectManger: StateType }) => ({
  projectManger,
}))(BindMeasuresTable);

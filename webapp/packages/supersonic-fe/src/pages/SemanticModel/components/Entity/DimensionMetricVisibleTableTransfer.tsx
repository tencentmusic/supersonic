import { Table, Transfer } from 'antd';
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface';
import type { TransferItem } from 'antd/es/transfer';
import difference from 'lodash/difference';
import React from 'react';
import { connect } from 'umi';
import type { StateType } from '../../model';
import type { IChatConfig } from '../../data';
import TransTypeTag from '../TransTypeTag';
import { SemanticNodeType, TransType } from '../../enum';
interface RecordType {
  id: number;
  key: string;
  name: string;
  bizName: string;
  type: TransType.DIMENSION | TransType.METRIC;
}

type Props = {
  domainManger: StateType;
  knowledgeInfosMap?: IChatConfig.IKnowledgeInfosItemMap;
  onKnowledgeInfosMapChange?: (knowledgeInfosMap: IChatConfig.IKnowledgeInfosItemMap) => void;
  [key: string]: any;
};

// type TaskStateMap = Record<string, DictTaskState>;

const DimensionMetricVisibleTableTransfer: React.FC<Props> = ({
  // domainManger,
  knowledgeInfosMap,
  // onKnowledgeInfosMapChange,
  ...restProps
}) => {
  let rightColumns: ColumnsType<RecordType> = [
    {
      dataIndex: 'name',
      title: '名称',
    },
    {
      dataIndex: 'type',
      width: 80,
      title: '类型',
      render: (type: SemanticNodeType) => {
        return <TransTypeTag type={type} />;
      },
    },
  ];

  const leftColumns: ColumnsType<RecordType> = [
    {
      dataIndex: 'name',
      title: '名称',
    },
    {
      dataIndex: 'type',
      title: '类型',
      render: (type) => {
        return <TransTypeTag type={type} />;
      },
    },
  ];
  if (!knowledgeInfosMap) {
    rightColumns = leftColumns;
  }
  return (
    <>
      <Transfer {...restProps}>
        {({
          direction,
          filteredItems,
          onItemSelectAll,
          onItemSelect,
          selectedKeys: listSelectedKeys,
        }) => {
          const columns = direction === 'left' ? leftColumns : rightColumns;
          const rowSelection: TableRowSelection<TransferItem> = {
            onSelectAll(selected, selectedRows) {
              const treeSelectedKeys = selectedRows.map(({ key }) => key);
              const diffKeys = selected
                ? difference(treeSelectedKeys, listSelectedKeys)
                : difference(listSelectedKeys, treeSelectedKeys);
              onItemSelectAll(diffKeys as string[], selected);
            },
            onSelect({ key }, selected) {
              onItemSelect(key as string, selected);
            },
            selectedRowKeys: listSelectedKeys,
          };

          return (
            <Table
              rowSelection={rowSelection}
              columns={columns}
              dataSource={filteredItems as any}
              size="small"
              pagination={false}
              scroll={{ y: 450 }}
              onRow={({ key }) => ({
                onClick: () => {
                  onItemSelect(key as string, !listSelectedKeys.includes(key as string));
                },
              })}
            />
          );
        }}
      </Transfer>
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DimensionMetricVisibleTableTransfer);

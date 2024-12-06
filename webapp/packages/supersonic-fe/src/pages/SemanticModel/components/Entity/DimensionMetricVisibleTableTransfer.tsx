import { Table, Transfer } from 'antd';
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface';
import type { TransferItem } from 'antd/es/transfer';
import difference from 'lodash/difference';
import React from 'react';
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
  knowledgeInfosMap?: IChatConfig.IKnowledgeInfosItemMap;
  [key: string]: any;
};

const DimensionMetricVisibleTableTransfer: React.FC<Props> = ({
  knowledgeInfosMap,
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
    {
      dataIndex: 'isTag',
      title: '是否标签',
      hidden: true,
      render: (isTag) => {
        if (isTag) {
          return <span style={{ color: '#0958d9' }}>是</span>;
        }
        return '否';
      },
    },
    {
      dataIndex: 'modelName',
      title: '所属模型',
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
          const columns: any = direction === 'left' ? leftColumns : rightColumns;
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

export default DimensionMetricVisibleTableTransfer;

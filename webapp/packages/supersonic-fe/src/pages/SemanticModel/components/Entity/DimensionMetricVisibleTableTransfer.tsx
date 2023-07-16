import { Space, Table, Transfer, Checkbox, Tooltip, Button } from 'antd';
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface';
import type { TransferItem } from 'antd/es/transfer';
import type { CheckboxChangeEvent } from 'antd/es/checkbox';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import difference from 'lodash/difference';
import React, { useState } from 'react';
import type { IChatConfig } from '../../data';
import DimensionValueSettingModal from './DimensionValueSettingModal';
import TransTypeTag from '../TransTypeTag';
import { TransType } from '../../enum';

interface RecordType {
  id: number;
  key: string;
  name: string;
  bizName: string;
  type: TransType.DIMENSION | TransType.METRIC;
}

type Props = {
  knowledgeInfosMap: IChatConfig.IKnowledgeInfosItemMap;
  onKnowledgeInfosMapChange: (knowledgeInfosMap: IChatConfig.IKnowledgeInfosItemMap) => void;
  [key: string]: any;
};

const DimensionMetricVisibleTableTransfer: React.FC<Props> = ({
  knowledgeInfosMap,
  onKnowledgeInfosMapChange,
  ...restProps
}) => {
  const [dimensionValueSettingModalVisible, setDimensionValueSettingModalVisible] =
    useState<boolean>(false);
  const [currentRecord, setCurrentRecord] = useState<any>({});
  const [currentDimensionSettingFormData, setCurrentDimensionSettingFormData] =
    useState<IChatConfig.IKnowledgeConfig>();

  const updateKnowledgeInfosMap = (record: RecordType, updateData: Record<string, any>) => {
    const { bizName, id } = record;
    const knowledgeMap = {
      ...knowledgeInfosMap,
    };
    const target = knowledgeMap[bizName];
    if (target) {
      knowledgeMap[bizName] = {
        ...target,
        ...updateData,
      };
    } else {
      knowledgeMap[bizName] = {
        itemId: id,
        bizName,
        ...updateData,
      };
    }
    onKnowledgeInfosMapChange?.(knowledgeMap);
  };

  const rightColumns: ColumnsType<RecordType> = [
    {
      dataIndex: 'name',
      title: '名称',
    },
    {
      dataIndex: 'type',
      width: 80,
      title: '类型',
      render: (type) => {
        return <TransTypeTag type={type} />;
      },
    },
    {
      dataIndex: 'y',
      title: (
        <Space>
          <span>维度值可见</span>
          <Tooltip title="勾选可见后，维度值将在搜索时可以被联想出来">
            <ExclamationCircleOutlined />
          </Tooltip>
        </Space>
      ),
      width: 120,
      render: (_, record) => {
        const { type, bizName } = record;
        return type === TransType.DIMENSION ? (
          <Checkbox
            checked={knowledgeInfosMap[bizName]?.searchEnable}
            onChange={(e: CheckboxChangeEvent) => {
              updateKnowledgeInfosMap(record, { searchEnable: e.target.checked });
            }}
            onClick={(event) => {
              event.stopPropagation();
            }}
          />
        ) : (
          <></>
        );
      },
    },
    {
      title: '操作',
      dataIndex: 'x',
      render: (_, record) => {
        const { type, bizName } = record;
        return type === TransType.DIMENSION ? (
          <Button
            style={{ padding: 0 }}
            key="editable"
            type="link"
            disabled={!knowledgeInfosMap[bizName]?.searchEnable}
            onClick={(event) => {
              setCurrentRecord(record);
              setCurrentDimensionSettingFormData(
                knowledgeInfosMap[bizName]?.knowledgeAdvancedConfig,
              );
              setDimensionValueSettingModalVisible(true);
              event.stopPropagation();
            }}
          >
            可见维度值设置
          </Button>
        ) : (
          <></>
        );
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
      <DimensionValueSettingModal
        visible={dimensionValueSettingModalVisible}
        initialValues={currentDimensionSettingFormData}
        onSubmit={(formValues) => {
          updateKnowledgeInfosMap(currentRecord, { knowledgeAdvancedConfig: formValues });
          setDimensionValueSettingModalVisible(false);
        }}
        onCancel={() => {
          setDimensionValueSettingModalVisible(false);
        }}
      />
    </>
  );
};

export default DimensionMetricVisibleTableTransfer;

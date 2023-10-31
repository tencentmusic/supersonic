import { Table, Transfer, Checkbox, message } from 'antd';
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface';
import type { TransferItem } from 'antd/es/transfer';
import type { CheckboxChangeEvent } from 'antd/es/checkbox';
import difference from 'lodash/difference';
import React, { useState, useEffect } from 'react';
import { connect } from 'umi';
import type { StateType } from '../model';
import TransTypeTag from './TransTypeTag';
import TableTitleTooltips from '../components/TableTitleTooltips';
import { ISemantic } from '../data';
import { getDimensionList } from '../service';
import { SemanticNodeType, TransType } from '../enum';

interface RecordType {
  id: number;
  key: string;
  name: string;
  transType: TransType.DIMENSION | TransType.METRIC;
}

type Props = {
  metricItem: ISemantic.IMetricItem;
  domainManger: StateType;
  relationsInitialValue?: ISemantic.IDrillDownDimensionItem[];
  onChange: (relations: ISemantic.IDrillDownDimensionItem[]) => void;
};

const DimensionMetricRelationTableTransfer: React.FC<Props> = ({
  metricItem,
  relationsInitialValue,
  onChange,
}) => {
  const [targetKeys, setTargetKeys] = useState<string[]>([]);

  const [checkedMap, setCheckedMap] = useState<Record<string, ISemantic.IDrillDownDimensionItem>>(
    {},
  );

  const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>([]);

  useEffect(() => {
    queryDimensionList();
  }, []);

  const queryDimensionList = async () => {
    const { code, data, msg } = await getDimensionList(metricItem.modelId);
    if (code === 200 && Array.isArray(data?.list)) {
      setDimensionList(data.list);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    if (!Array.isArray(relationsInitialValue)) {
      return;
    }
    const ids = relationsInitialValue.map((item) => `${item.dimensionId}`);
    const relationMap = relationsInitialValue.reduce((relationCheckedMap, item: any) => {
      const { dimensionId, necessary } = item;
      relationCheckedMap[dimensionId] = {
        dimensionId: Number(dimensionId),
        necessary: necessary,
      };
      return relationCheckedMap;
    }, {});
    setCheckedMap(relationMap);
    setTargetKeys(ids);
  }, [relationsInitialValue]);

  const updateRelationCheckedMap = (
    record: RecordType,
    updateData: ISemantic.IDrillDownDimensionItem,
  ) => {
    const { id } = record;
    const relationCheckedMap = {
      ...checkedMap,
    };
    const target = relationCheckedMap[id];
    if (target) {
      relationCheckedMap[id] = {
        ...target,
        ...updateData,
      };
    } else {
      relationCheckedMap[id] = {
        ...updateData,
      };
    }
    setCheckedMap(relationCheckedMap);
    handleRealtionChange(targetKeys, relationCheckedMap);
  };

  const handleRealtionChange = (
    targetKeys: string[],
    relationCheckedMap: Record<string, ISemantic.IDrillDownDimensionItem>,
  ) => {
    const relations = targetKeys.reduce(
      (relationList: ISemantic.IDrillDownDimensionItem[], dimensionId: string) => {
        const target = relationCheckedMap[dimensionId];
        if (target) {
          relationList.push(target);
        } else {
          relationList.push({
            dimensionId: Number(dimensionId),
            necessary: false,
          });
        }
        return relationList;
      },
      [],
    );
    onChange?.(relations);
  };

  const rightColumns: ColumnsType<RecordType> = [
    {
      dataIndex: 'name',
      title: '名称',
    },
    {
      dataIndex: 'transType',
      width: 80,
      title: '类型',
      render: (transType: SemanticNodeType) => {
        return <TransTypeTag type={transType} />;
      },
    },
    {
      dataIndex: 'y',
      title: (
        <TableTitleTooltips
          title="是否绑定"
          tooltips="若勾选绑定，则在查询该指标数据时必须结合该维度进行查询"
        />
      ),
      width: 120,
      render: (_: any, record: RecordType) => {
        const { transType, id } = record;
        return transType === TransType.DIMENSION ? (
          <Checkbox
            checked={checkedMap[id]?.necessary}
            onChange={(e: CheckboxChangeEvent) => {
              updateRelationCheckedMap(record, { dimensionId: id, necessary: e.target.checked });
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
  ];

  const leftColumns: ColumnsType<RecordType> = [
    {
      dataIndex: 'name',
      title: '名称',
    },
    {
      dataIndex: 'transType',
      title: '类型',
      render: (transType) => {
        return <TransTypeTag type={transType} />;
      },
    },
  ];

  return (
    <>
      <Transfer
        showSearch
        titles={['未关联维度', '已关联维度']}
        dataSource={dimensionList.map((item) => {
          const transType = TransType.DIMENSION;
          const { id } = item;
          return {
            ...item,
            transType,
            key: `${id}`,
          };
        })}
        listStyle={{
          width: 500,
          height: 600,
        }}
        filterOption={(inputValue: string, item: any) => {
          const { name } = item;
          if (name.includes(inputValue)) {
            return true;
          }
          return false;
        }}
        targetKeys={targetKeys}
        onChange={(newTargetKeys: string[]) => {
          setTargetKeys(newTargetKeys);
          handleRealtionChange(newTargetKeys, checkedMap);
        }}
      >
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
}))(DimensionMetricRelationTableTransfer);

import React, { useState, useRef, useEffect } from 'react';
import { Tag, Space } from 'antd';
import { ProTable } from '@ant-design/pro-components';
import { ProCard } from  '@ant-design/pro-components';
import SqlEditor from '@/components/SqlEditor';
import FormLabelRequire from './FormLabelRequire';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  typeParams: ISemantic.ITagDefineParams;
  dimensionList: ISemantic.IDimensionItem[];
  onFieldChange: (metrics: ISemantic.IMetricTypeParamsItem[]) => void;
  onSqlChange: (sql: string) => void;
};

const TagDimensionFormTable: React.FC<Props> = ({
  typeParams,
  dimensionList,
  onFieldChange,
  onSqlChange,
}) => {
  const actionRef = useRef<ActionType>();

  const [tableData, setTableData] = useState<any[]>([]);

  useEffect(() => {
    if (!Array.isArray(dimensionList)) {
      setTableData([]);
      return;
    }
    setTableData(dimensionList);
  }, [dimensionList]);

  const [defineTypeParams, setDefineTypeParams] = useState<ISemantic.ITagDefineParams>({
    expr: typeParams?.expr || '',
    dependencies: typeParams?.dependencies || [],
  });

  useEffect(() => {
    setDefineTypeParams({ ...typeParams });
  }, [typeParams]);

  const [exprString, setExprString] = useState(typeParams?.expr || '');

  const [selectedKeys, setSelectedKeys] = useState<any[]>(() => {
    return defineTypeParams.dependencies;
  });

  const [selectedKeysMap, setSelectedKeysMap] = useState<Record<string, boolean>>(() => {
    return defineTypeParams.dependencies.reduce(
      (keyMap: Record<string, boolean>, dimensionId: string | number) => {
        keyMap[`${dimensionId}`] = true;
        return keyMap;
      },
      {},
    );
  });

  const columns = [
    {
      dataIndex: 'name',
      title: '维度名称',
    },
    {
      dataIndex: 'bizName',
      title: '英文名称',
    },
  ];

  const handleUpdateKeys = (updateKeys: Record<string, boolean>) => {
    setSelectedKeysMap(updateKeys);
    const selectedKeys: number[] = [];
    const dimenisons = dimensionList.reduce((list: any[], item) => {
      const { bizName, id } = item;
      if (updateKeys[id] === true) {
        selectedKeys.push(id);
        list.push({
          bizName,
          id,
        });
      }
      return list;
    }, []);
    setSelectedKeys(selectedKeys);
    onFieldChange(dimenisons);
  };

  const rowSelection = {
    selectedRowKeys: selectedKeys,
    onSelect: (record: ISemantic.IDimensionItem, selected: boolean) => {
      const updateKeys = { ...selectedKeysMap, [record.id]: selected };
      handleUpdateKeys(updateKeys);
    },
    onSelectAll: (
      selected: boolean,
      selectedRows: ISemantic.IDimensionItem[],
      changeRows: ISemantic.IDimensionItem[],
    ) => {
      const updateKeys = changeRows.reduce(
        (keyMap: Record<string, boolean>, item: ISemantic.IDimensionItem) => {
          keyMap[item.id] = selected;
          return keyMap;
        },
        {},
      );
      handleUpdateKeys({ ...selectedKeysMap, ...updateKeys });
    },
  };

  return (
    <>
      <Space direction="vertical" style={{ width: '100%' }}>
        <ProTable
          actionRef={actionRef}
          headerTitle={<FormLabelRequire title="维度列表" />}
          rowKey="id"
          columns={columns}
          dataSource={tableData}
          // pagination={false}
          search={false}
          toolbar={{
            search: {
              placeholder: '请输入维度名称',
              onSearch: (value: string) => {
                setTableData(
                  dimensionList.reduce(
                    (data: ISemantic.IDimensionItem[], item: ISemantic.IDimensionItem) => {
                      if (item.name.includes(value)) {
                        data.push(item);
                      }
                      return data;
                    },
                    [],
                  ),
                );
              },
            },
          }}
          pagination={{ defaultPageSize: 10 }}
          size="small"
          options={false}
          tableAlertRender={false}
          scroll={{ y: 500 }}
          rowSelection={rowSelection}
        />
        <ProCard title={<FormLabelRequire title="表达式" />} tooltip="">
          <p
            className={styles.desc}
            style={{ border: 'unset', padding: 0, marginBottom: 20, marginLeft: 2 }}
          >
            已创建的维度已经过聚合，因此通过这些维度来创建新的维度无需指定
            <Tag color="#2499ef14" className={styles.markerTag}>
              聚合函数
            </Tag>
            ，如根据维度c和维度d来创建新的维度，因为维度本身带有聚合函数，因此表达式可以写成:
            c+d-100
          </p>
          <SqlEditor
            value={exprString}
            onChange={(sql: string) => {
              const expr = sql;
              setExprString(expr);
              onSqlChange?.(expr);
            }}
            height={'150px'}
          />
        </ProCard>
      </Space>
    </>
  );
};

export default TagDimensionFormTable;

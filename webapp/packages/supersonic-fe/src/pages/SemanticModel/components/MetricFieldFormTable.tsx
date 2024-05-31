import React, { useState, useEffect } from 'react';
import { Tag, Space } from 'antd';
import { ProTable } from '@ant-design/pro-components';
import { ProCard } from '@ant-design/pro-components';
import SqlEditor from '@/components/SqlEditor';
import styles from './style.less';
import FormLabelRequire from './FormLabelRequire';
import { ISemantic } from '../data';

type Props = {
  typeParams: ISemantic.IFieldTypeParams;
  fieldList: ISemantic.IFieldTypeParamsItem[];
  onFieldChange: (fields: ISemantic.IFieldTypeParamsItem[]) => void;
  onSqlChange: (sql: string) => void;
};

const MetricFieldFormTable: React.FC<Props> = ({
  typeParams,
  fieldList,
  onFieldChange,
  onSqlChange,
}) => {
  const [tableData, setTableData] = useState<ISemantic.IFieldTypeParamsItem[]>([]);

  const [defineTypeParams, setDefineTypeParams] = useState(
    typeParams || {
      expr: '',
      fields: [],
    },
  );

  useEffect(() => {
    if (!Array.isArray(fieldList)) {
      setTableData([]);
      return;
    }
    setTableData(fieldList);
  }, [fieldList]);

  useEffect(() => {
    setDefineTypeParams({ ...typeParams });
  }, [typeParams]);

  const [exprString, setExprString] = useState(typeParams?.expr || '');

  const [selectedKeys, setSelectedKeys] = useState<string[]>(() => {
    return defineTypeParams.fields.map((item: ISemantic.IFieldTypeParamsItem) => {
      return item.fieldName;
    });
  });

  const [selectedKeysMap, setSelectedKeysMap] = useState<Record<string, boolean>>(() => {
    return defineTypeParams.fields.reduce(
      (keyMap: Record<string, boolean>, item: ISemantic.IFieldTypeParamsItem) => {
        keyMap[item.fieldName] = true;
        return keyMap;
      },
      {},
    );
  });

  const columns = [
    {
      dataIndex: 'fieldName',
      title: '字段名称',
    },
    {
      dataIndex: 'dataType',
      title: '字段类型',
    },
  ];

  const handleUpdateKeys = (updateKeys: Record<string, boolean>) => {
    setSelectedKeysMap(updateKeys);
    const selectedKeys: string[] = [];
    const fieldList = Object.entries(updateKeys).reduce((list: any[], item) => {
      const [fieldName, selected] = item;
      if (selected) {
        selectedKeys.push(fieldName);
        list.push({ fieldName });
      }
      return list;
    }, []);
    setSelectedKeys(selectedKeys);
    onFieldChange(fieldList);
  };

  const rowSelection = {
    selectedRowKeys: selectedKeys,
    onSelect: (record: ISemantic.IFieldTypeParamsItem, selected: boolean) => {
      const updateKeys = { ...selectedKeysMap, [record.fieldName]: selected };
      handleUpdateKeys(updateKeys);
    },
    onSelectAll: (
      selected: boolean,
      selectedRows: ISemantic.IFieldTypeParamsItem[],
      changeRows: ISemantic.IFieldTypeParamsItem[],
    ) => {
      const updateKeys = changeRows.reduce(
        (keyMap: Record<string, boolean>, item: ISemantic.IFieldTypeParamsItem) => {
          keyMap[item.fieldName] = selected;
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
          headerTitle={<FormLabelRequire title="字段列表" />}
          rowKey="fieldName"
          columns={columns}
          dataSource={tableData}
          search={false}
          toolbar={{
            search: {
              placeholder: '请输入字段名称',
              onSearch: (value: string) => {
                if (!value) {
                  setTableData(fieldList);
                  return;
                }

                setTableData(
                  fieldList.reduce((data: ISemantic.IFieldTypeParamsItem[], item) => {
                    if (item.fieldName.includes(value)) {
                      data.push(item);
                    }
                    return data;
                  }, []),
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
        <ProCard
          title={<FormLabelRequire title="表达式" />}
          // tooltip="度量表达式由上面选择的度量组成，如选择了度量A和B，则可将表达式写成A+B"
        >
          <div>
            <p
              className={styles.desc}
              style={{ border: 'unset', padding: 0, marginBottom: 20, marginLeft: 2 }}
            >
              由于字段上是不带聚合函数的，因此通过字段x和y创建指标时，表达式需要写
              <Tag color="#2499ef14" className={styles.markerTag}>
                聚合函数
              </Tag>
              如: sum(x) + sum(y)
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
          </div>
        </ProCard>
      </Space>
    </>
  );
};

export default MetricFieldFormTable;

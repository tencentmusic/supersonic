import React, { useState, useRef, useEffect } from 'react';
import { Button, Tag, Space } from 'antd';
import ProTable from '@ant-design/pro-table';
import ProCard from '@ant-design/pro-card';
import SqlEditor from '@/components/SqlEditor';
import styles from './style.less';
import FormLabelRequire from './FormLabelRequire';
import { ISemantic } from '../data';

type Props = {
  typeParams: ISemantic.IFieldTypeParams;
  fieldList: string[];
  onFieldChange: (fields: ISemantic.IFieldTypeParamsItem[]) => void;
  onSqlChange: (sql: string) => void;
};

const MetricMeasuresFormTable: React.FC<Props> = ({
  typeParams,
  fieldList,
  onFieldChange,
  onSqlChange,
}) => {
  const [tableData, setTableData] = useState<any[]>([]);

  const [defineTypeParams, setDefineTypeParams] = useState(
    typeParams || {
      expr: '',
      metrics: [],
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
    return defineTypeParams.fields.map((item: any) => {
      return item.fieldName;
    });
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

  const rowSelection = {
    selectedRowKeys: selectedKeys,
    onChange: (_selectedRowKeys: any[]) => {
      setSelectedKeys([..._selectedRowKeys]);
      onFieldChange(
        _selectedRowKeys.map((fieldName) => {
          return { fieldName };
        }),
      );
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
          pagination={false}
          search={false}
          toolbar={{
            search: {
              placeholder: '请输入字段名称',
              onSearch: (value: string) => {
                setTableData(
                  fieldList.reduce((data: ISemantic.IFieldTypeParamsItem[], fieldName) => {
                    if (fieldName.includes(value)) {
                      data.push({ fieldName });
                    }
                    return data;
                  }, []),
                );
              },
            },
          }}
          size="small"
          options={false}
          tableAlertRender={false}
          scroll={{ y: 500 }}
          rowSelection={rowSelection}
        />
        <ProCard
          title={<FormLabelRequire title="字段表达式" />}
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

export default MetricMeasuresFormTable;

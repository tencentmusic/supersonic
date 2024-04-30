import React, { useState, useRef, useEffect } from 'react';
import { Tag, Space } from 'antd';
import { ProTable } from '@ant-design/pro-components';
import { ProCard } from  '@ant-design/pro-components';
import SqlEditor from '@/components/SqlEditor';
import FormLabelRequire from './FormLabelRequire';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  typeParams: ISemantic.IMetricTypeParams;
  metricList: ISemantic.IMetricItem[];
  onFieldChange: (metrics: ISemantic.IMetricTypeParamsItem[]) => void;
  onSqlChange: (sql: string) => void;
};

const MetricMetricFormTable: React.FC<Props> = ({
  typeParams,
  metricList,
  onFieldChange,
  onSqlChange,
}) => {
  const actionRef = useRef<ActionType>();

  const [tableData, setTableData] = useState<any[]>([]);

  useEffect(() => {
    if (!Array.isArray(metricList)) {
      setTableData([]);
      return;
    }
    setTableData(metricList);
  }, [metricList]);

  const [defineTypeParams, setDefineTypeParams] = useState(
    typeParams || {
      expr: '',
      metrics: [],
    },
  );

  useEffect(() => {
    setDefineTypeParams({ ...typeParams });
  }, [typeParams]);

  const [exprString, setExprString] = useState(typeParams?.expr || '');

  // const [selectMeasuresList, setSelectMeasuresList] = useState<IDataSource.IMeasuresItem[]>([]);

  const [selectedKeys, setSelectedKeys] = useState<string[]>(() => {
    // return [];
    return defineTypeParams.metrics.map((item: any) => {
      return item.bizName;
    });
  });

  const [selectedKeysMap, setSelectedKeysMap] = useState<Record<string, boolean>>(() => {
    return defineTypeParams.metrics.reduce((keyMap, item: any) => {
      keyMap[item.bizName] = true;
      return keyMap;
    }, {});
  });

  const columns = [
    {
      dataIndex: 'name',
      title: '指标名称',
    },
    {
      dataIndex: 'bizName',
      title: '英文名称',
    },
  ];

  const handleUpdateKeys = (updateKeys: Record<string, boolean>) => {
    setSelectedKeysMap(updateKeys);
    const selectedKeys: string[] = [];
    const metrics = metricList.reduce((list: any[], item) => {
      const { bizName, id } = item;
      if (updateKeys[bizName] === true) {
        selectedKeys.push(bizName);
        list.push({
          bizName,
          id,
        });
      }
      return list;
    }, []);
    setSelectedKeys(selectedKeys);
    onFieldChange(metrics);
  };

  const rowSelection = {
    selectedRowKeys: selectedKeys,
    onSelect: (record: ISemantic.IMeasure, selected: boolean) => {
      const updateKeys = { ...selectedKeysMap, [record.bizName]: selected };
      handleUpdateKeys(updateKeys);
    },
    onSelectAll: (
      selected: boolean,
      selectedRows: ISemantic.IMetricItem[],
      changeRows: ISemantic.IMetricItem[],
    ) => {
      const updateKeys = changeRows.reduce(
        (keyMap: Record<string, boolean>, item: ISemantic.IMetricItem) => {
          keyMap[item.bizName] = selected;
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
          headerTitle={<FormLabelRequire title="指标列表" />}
          rowKey="bizName"
          columns={columns}
          dataSource={tableData}
          // pagination={false}
          search={false}
          toolbar={{
            search: {
              placeholder: '请输入指标名称',
              onSearch: (value: string) => {
                setTableData(
                  metricList.reduce(
                    (data: ISemantic.IMetricItem[], item: ISemantic.IMetricItem) => {
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
            已创建的指标已经过聚合，因此通过这些指标来创建新的指标无需指定
            <Tag color="#2499ef14" className={styles.markerTag}>
              聚合函数
            </Tag>
            ，如根据指标c和指标d来创建新的指标，因为指标本身带有聚合函数，因此表达式可以写成:
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

export default MetricMetricFormTable;

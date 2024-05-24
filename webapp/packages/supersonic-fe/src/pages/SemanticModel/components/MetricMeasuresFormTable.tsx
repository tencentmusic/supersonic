import React, { useState, useRef, useEffect } from 'react';
import { Input, Space, Tag, Divider } from 'antd';
import { ProTable } from '@ant-design/pro-components';
import { ProCard } from '@ant-design/pro-components';
import SqlEditor from '@/components/SqlEditor';

import FormLabelRequire from './FormLabelRequire';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  datasourceId?: number;
  typeParams: ISemantic.IMeasureTypeParams;
  measuresList: ISemantic.IMeasure[];
  onFieldChange: (measures: ISemantic.IMeasure[]) => void;
  onSqlChange: (sql: string) => void;
};

const { TextArea } = Input;

const MetricMeasuresFormTable: React.FC<Props> = ({
  datasourceId,
  typeParams,
  measuresList,
  onFieldChange,
  onSqlChange,
}) => {
  const actionRef = useRef<ActionType>();

  const [tableData, setTableData] = useState<any[]>([]);

  const [measuresParams, setMeasuresParams] = useState(
    typeParams || {
      expr: '',
      measures: [],
    },
  );

  const [selectedKeys, setSelectedKeys] = useState<string[]>(() => {
    return measuresParams?.measures.map((item: ISemantic.IMeasure) => {
      return item.bizName;
    });
  });

  const [selectedKeysMap, setSelectedKeysMap] = useState<Record<string, boolean>>(() => {
    if (!Array.isArray(measuresParams?.measures)) {
      return {};
    }
    return measuresParams.measures.reduce((keyMap: any, item: ISemantic.IMeasure) => {
      keyMap[item.bizName] = true;
      return keyMap;
    }, {});
  });

  const getTableData = () => {
    const datasource =
      datasourceId && Array.isArray(measuresList)
        ? measuresList.filter((item) => item.datasourceId === datasourceId)
        : measuresList;
    const { measures } = measuresParams;
    if (!Array.isArray(measures)) {
      return datasource;
    }
    const tableData = datasource.map((item) => {
      const { bizName } = item;
      const target = measures.find((measureItem) => measureItem.bizName === bizName);
      if (target) {
        return {
          ...item,
          constraint: target.constraint,
        };
      }
      return item;
    });
    return tableData;
  };

  useEffect(() => {
    setTableData(getTableData());
  }, [measuresList, measuresParams]);

  useEffect(() => {
    setMeasuresParams({ ...typeParams });
  }, [typeParams]);

  const [exprString, setExprString] = useState(typeParams?.expr || '');

  const columns = [
    {
      dataIndex: 'bizName',
      title: '度量名称',
      tooltip: '由模型名称_字段名称拼接而来',
    },
    {
      dataIndex: 'constraint',
      title: '限定条件',
      width: 350,
      tooltip:
        '该限定条件用于在计算指标时限定口径，作用于度量，所用于过滤的维度必须在创建模型的时候被标记为日期或者维度，不需要加where关键字。比如：维度A="值1" and 维度B="值2"',
      render: (_: any, record: any) => {
        const { constraint, bizName } = record;
        return (
          <TextArea
            placeholder="请输入限定条件"
            value={constraint}
            style={{ height: 42 }}
            disabled={!selectedKeysMap[bizName]}
            onChange={(event) => {
              const { value } = event.target;

              const measures = tableData.reduce((list: any[], item) => {
                if (selectedKeysMap[item.bizName] === true) {
                  if (item.bizName === bizName) {
                    list.push({
                      ...item,
                      constraint: value,
                    });
                  } else {
                    list.push(item);
                  }
                }
                return list;
              }, []);
              onFieldChange?.(measures);
            }}
          />
        );
      },
    },
    {
      dataIndex: 'agg',
      title: '聚合函数',
      width: 150,
    },
  ];

  const handleUpdateKeys = (updateKeys: Record<string, boolean>) => {
    const datasource = getTableData();
    setSelectedKeysMap(updateKeys);
    const selectedKeys: string[] = [];
    const measures = datasource.reduce((list: any[], item) => {
      if (updateKeys[item.bizName] === true) {
        selectedKeys.push(item.bizName);
        list.push(item);
      }
      return list;
    }, []);
    setSelectedKeys(selectedKeys);
    onFieldChange(measures);
  };

  const rowSelection = {
    selectedRowKeys: selectedKeys,
    onSelect: (record: ISemantic.IMeasure, selected: boolean) => {
      const updateKeys = { ...selectedKeysMap, [record.bizName]: selected };
      handleUpdateKeys(updateKeys);
    },
    onSelectAll: (
      selected: boolean,
      selectedRows: ISemantic.IMeasure[],
      changeRows: ISemantic.IMeasure[],
    ) => {
      const updateKeys = changeRows.reduce(
        (keyMap: Record<string, boolean>, item: ISemantic.IMeasure) => {
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
          headerTitle={<FormLabelRequire title="度量列表" />}
          rowKey="bizName"
          columns={columns}
          dataSource={tableData}
          search={false}
          toolbar={{
            search: {
              placeholder: '请输入度量名称',
              onSearch: (value: string) => {
                const tableData = getTableData();
                if (!value) {
                  setTableData(tableData);
                  return;
                }
                setTableData(
                  [...tableData].reduce((data: ISemantic.IMeasure[], item: ISemantic.IMeasure) => {
                    if (item.bizName.includes(value)) {
                      data.push(item);
                    }
                    return data;
                  }, []),
                );
              },
            },
          }}
          pagination={{ defaultPageSize: 10 }}
          // size="small"
          options={false}
          tableAlertRender={false}
          scroll={{ y: 700 }}
          rowSelection={rowSelection}
        />
        <Divider style={{ marginBottom: 0 }} />
        <ProCard
          title={<FormLabelRequire title="表达式" />}
          // tooltip="由于度量已自带聚合函数，因此通过度量创建指标时，表达式中无需再写聚合函数，如
          // 通过度量a和度量b来创建指标，由于建模的时候度量a和度量b被指定了聚合函数SUM，因此创建指标时表达式只需要写成 a+b, 而不需要带聚合函数"
        >
          <p
            className={styles.desc}
            style={{ border: 'unset', padding: 0, marginBottom: 20, marginLeft: 2 }}
          >
            在
            <Tag color="#2499ef14" className={styles.markerTag}>
              建模时
            </Tag>
            度量已指定了
            <Tag color="#2499ef14" className={styles.markerTag}>
              聚合函数
            </Tag>
            ，在度量模式下，表达式无需再写聚合函数，如:
            通过指定了聚合函数SUM的度量a和度量b来创建指标，表达式只需要写成 a+b
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

export default MetricMeasuresFormTable;

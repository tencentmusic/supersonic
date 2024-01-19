import React, { useState, useRef, useEffect } from 'react';
import { Tag, Space } from 'antd';
import ProTable from '@ant-design/pro-table';
import ProCard from '@ant-design/pro-card';
import SqlEditor from '@/components/SqlEditor';
import FormLabelRequire from './FormLabelRequire';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  typeParams: ISemantic.IMetricTypeParams;
  metricList: ISemantic.IMetricItem[];
  // selectedMeasuresList: any;
  onFieldChange: (metrics: ISemantic.IMetricTypeParamsItem[]) => void;
  onSqlChange: (sql: string) => void;
};

const MetricMetricFormTable: React.FC<Props> = ({
  typeParams,
  // selectedMeasuresList = [],
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

  const [selectedMeasuresKeys, setSelectedMeasuresKeys] = useState<string[]>(() => {
    // return [];
    return defineTypeParams.metrics.map((item: any) => {
      return item.bizName;
    });
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

  const rowSelection = {
    selectedRowKeys: selectedMeasuresKeys,
    onChange: (_selectedRowKeys: any[]) => {
      setSelectedMeasuresKeys([..._selectedRowKeys]);
      onFieldChange(
        metricList.reduce(
          (metrics: ISemantic.IMetricTypeParamsItem[], item: ISemantic.IMetricItem) => {
            if (_selectedRowKeys.includes(item.bizName)) {
              metrics.push({
                bizName: item.bizName,
                id: item.id,
              });
            }
            return metrics;
          },
          [],
        ),
      );
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
          pagination={false}
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
          size="small"
          options={false}
          tableAlertRender={false}
          scroll={{ y: 500 }}
          rowSelection={rowSelection}
        />
        <ProCard title={<FormLabelRequire title="指标表达式" />} tooltip="">
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

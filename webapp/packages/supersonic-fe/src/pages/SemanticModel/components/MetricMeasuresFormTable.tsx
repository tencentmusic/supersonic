import React, { useState, useRef, useEffect } from 'react';
import { Button, Input, Space } from 'antd';
import ProTable from '@ant-design/pro-table';
import ProCard from '@ant-design/pro-card';
import SqlEditor from '@/components/SqlEditor';
import BindMeasuresTable from './BindMeasuresTable';
import FormLabelRequire from './FormLabelRequire';
import { ISemantic } from '../data';

type Props = {
  datasourceId?: number;
  typeParams: ISemantic.ITypeParams;
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

  const [measuresModalVisible, setMeasuresModalVisible] = useState<boolean>(false);
  const [measuresParams, setMeasuresParams] = useState(
    typeParams || {
      expr: '',
      measures: [],
    },
  );

  useEffect(() => {
    setMeasuresParams({ ...typeParams });
  }, [typeParams]);

  const [exprString, setExprString] = useState(typeParams?.expr || '');

  const columns = [
    {
      dataIndex: 'bizName',
      title: '度量名称',
    },
    {
      dataIndex: 'constraint',
      title: '限定条件',
      tooltip:
        '该限定条件用于在计算指标时限定口径，作用于度量，所用于过滤的维度必须在创建数据源的时候被标记为日期或者维度，不需要加where关键字。比如：维度A="值1" and 维度B="值2"',
      render: (_: any, record: any) => {
        const { constraint, name } = record;
        const { measures } = measuresParams;
        return (
          <TextArea
            placeholder="请输入限定条件"
            value={constraint}
            onChange={(event) => {
              const { value } = event.target;
              const list = measures.map((item: any) => {
                if (item.name === name) {
                  return {
                    ...item,
                    constraint: value,
                  };
                }
                return item;
              });
              onFieldChange?.(list);
            }}
          />
        );
      },
    },
    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      render: (_: any, record: any) => {
        const { name } = record;
        return (
          <Space>
            <a
              key="deleteBtn"
              onClick={() => {
                const { measures } = measuresParams;
                const list = measures.filter((item: any) => {
                  return item.name !== name;
                });
                onFieldChange?.(list);
              }}
            >
              删除
            </a>
          </Space>
        );
      },
    },
  ];
  return (
    <>
      <Space direction="vertical" style={{ width: '100%' }}>
        <ProTable
          actionRef={actionRef}
          headerTitle={<FormLabelRequire title="度量列表" />}
          tooltip="基于本主题域下所有数据源的度量来创建指标，且该列表的度量为了加以区分，均已加上数据源名称作为前缀，选中度量后，可基于这几个度量来写表达式，若是选中的度量来自不同的数据源，系统将会自动join来计算该指标"
          rowKey="name"
          columns={columns}
          dataSource={measuresParams?.measures || []}
          pagination={false}
          search={false}
          size="small"
          options={false}
          toolBarRender={() => [
            <Button
              key="create"
              type="primary"
              onClick={() => {
                setMeasuresModalVisible(true);
              }}
            >
              增加度量
            </Button>,
          ]}
        />
        <ProCard
          title={<FormLabelRequire title="度量表达式" />}
          tooltip="度量表达式由上面选择的度量组成，如选择了度量A和B，则可将表达式写成A+B"
        >
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
      {measuresModalVisible && (
        <BindMeasuresTable
          measuresList={
            datasourceId && Array.isArray(measuresList)
              ? measuresList.filter((item) => item.datasourceId === datasourceId)
              : measuresList
          }
          selectedMeasuresList={measuresParams?.measures || []}
          onSubmit={async (values: any[]) => {
            const measures = values.map(({ bizName, name, expr, datasourceId }) => {
              return {
                bizName,
                name,
                expr,
                datasourceId,
              };
            });
            onFieldChange?.(measures);
            setMeasuresModalVisible(false);
          }}
          onCancel={() => {
            setMeasuresModalVisible(false);
          }}
          createModalVisible={measuresModalVisible}
        />
      )}
    </>
  );
};

export default MetricMeasuresFormTable;

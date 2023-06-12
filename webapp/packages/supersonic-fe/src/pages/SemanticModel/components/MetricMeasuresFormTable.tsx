import React, { useState, useRef, useEffect } from 'react';
import { Button, Input, Space } from 'antd';
import ProTable from '@ant-design/pro-table';
import ProCard from '@ant-design/pro-card';
import SqlEditor from '@/components/SqlEditor';
import BindMeasuresTable from './BindMeasuresTable';

type Props = {
  typeParams: any;
  measuresList: any[];
  onFieldChange: (measures: any[]) => void;
  onSqlChange: (sql: string) => void;
};

const { TextArea } = Input;

const MetricMeasuresFormTable: React.FC<Props> = ({
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
    // {
    //   dataIndex: 'alias',
    //   title: '别名',
    //   render: (_: any, record: any) => {
    //     const { alias, name } = record;
    //     const { measures } = measuresParams;
    //     return (
    //       <Input
    //         placeholder="请输入别名"
    //         value={alias}
    //         onChange={(event) => {
    //           const { value } = event.target;
    //           const list = measures.map((item: any) => {
    //             if (item.name === name) {
    //               return {
    //                 ...item,
    //                 alias: value,
    //               };
    //             }
    //             return item;
    //           });
    //           onFieldChange?.(list);
    //         }}
    //       />
    //     );
    //   },
    // },
    {
      dataIndex: 'constraint',
      title: '限定条件',
      tooltip:
        '所用于过滤的维度需要存在于"维度"列表，不需要加where关键字。比如：维度A="值1" and 维度B="值2"',
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
          headerTitle="度量列表"
          tooltip="一般用于在“指标”列表已有指标的基础上加工新指标，比如：指标NEW1=指标A/100，指标NEW2=指标B/指标C。（若需用到多个已有指标，可以点击右上角“增加度量”）"
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
          title={'度量表达式'}
          tooltip="若为指标NEW1，则填写：指标A/100。若为指标NEW2，则填写：指标B/指标C"
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
          measuresList={measuresList}
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

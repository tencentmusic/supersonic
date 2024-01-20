import React, { useState, useRef, useEffect } from 'react';
import { Button, Input, Space, Tag } from 'antd';
import ProTable from '@ant-design/pro-table';
import ProCard from '@ant-design/pro-card';
import SqlEditor from '@/components/SqlEditor';
import BindMeasuresTable from './BindMeasuresTable';
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
      tooltip: '由模型名称_字段名称拼接而来',
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
      dataIndex: 'agg',
      title: '聚合函数',
    },

    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      render: (_: any, record: any) => {
        const { bizName } = record;
        return (
          <Space>
            <a
              key="deleteBtn"
              onClick={() => {
                const { measures } = measuresParams;
                const list = measures.filter((item: any) => {
                  return item.bizName !== bizName;
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
      {measuresModalVisible && (
        <BindMeasuresTable
          measuresList={
            datasourceId && Array.isArray(measuresList)
              ? measuresList.filter((item) => item.datasourceId === datasourceId)
              : measuresList
          }
          selectedMeasuresList={measuresParams?.measures || []}
          onSubmit={async (values: any[]) => {
            const measures = values.map(({ bizName, name, expr, datasourceId, agg }) => {
              return {
                bizName,
                name,
                expr,
                agg,
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

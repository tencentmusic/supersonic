import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm } from 'antd';
import React, { useRef, useState } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../model';
import { SENSITIVE_LEVEL_ENUM } from '../constant';
import { queryMetric, deleteMetric } from '../service';

import MetricInfoCreateForm from './MetricInfoCreateForm';

import moment from 'moment';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const ClassMetricTable: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { selectModelId: modelId, selectDomainId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [metricItem, setMetricItem] = useState<ISemantic.IMetricItem>();
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const actionRef = useRef<ActionType>();

  const queryMetricList = async (params: any) => {
    const { code, data, msg } = await queryMetric({
      ...params,
      ...pagination,
      modelId,
    });
    const { list, pageSize, current, total } = data || {};
    let resData: any = {};
    if (code === 200) {
      setPagination({
        pageSize: Math.min(pageSize, 100),
        current,
        total,
      });

      resData = {
        data: list || [],
        success: true,
      };
    } else {
      message.error(msg);
      resData = {
        data: [],
        total: 0,
        success: false,
      };
    }
    return resData;
  };

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      width: 80,
    },
    {
      dataIndex: 'name',
      title: '指标名称',
    },
    {
      dataIndex: 'alias',
      title: '别名',
      width: 300,
      ellipsis: true,
      search: false,
    },
    {
      dataIndex: 'bizName',
      title: '字段名称',
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      width: 80,
      valueEnum: SENSITIVE_LEVEL_ENUM,
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
      search: false,
    },
    {
      dataIndex: 'description',
      title: '描述',
      search: false,
    },
    // {
    //   dataIndex: 'type',
    //   title: '指标类型',
    //   valueEnum: {
    //     ATOMIC: '原子指标',
    //     DERIVED: '衍生指标',
    //   },
    // },

    {
      dataIndex: 'updatedAt',
      title: '更新时间',
      search: false,
      render: (value: any) => {
        return value && value !== '-' ? moment(value).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },
    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      render: (_, record) => {
        return (
          <Space>
            <a
              key="metricEditBtn"
              onClick={() => {
                setMetricItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </a>

            <Popconfirm
              title="确认删除？"
              okText="是"
              cancelText="否"
              onConfirm={async () => {
                const { code, msg } = await deleteMetric(record.id);
                if (code === 200) {
                  setMetricItem(undefined);
                  actionRef.current?.reload();
                } else {
                  message.error(msg);
                }
              }}
            >
              <a
                key="metricDeleteBtn"
                onClick={() => {
                  setMetricItem(record);
                }}
              >
                删除
              </a>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft}`}
        actionRef={actionRef}
        rowKey="id"
        search={{
          span: 4,
          defaultCollapsed: false,
          collapseRender: () => {
            return <></>;
          },
        }}
        columns={columns}
        params={{ modelId }}
        request={queryMetricList}
        pagination={pagination}
        tableAlertRender={() => {
          return false;
        }}
        onChange={(data: any) => {
          const { current, pageSize, total } = data;
          setPagination({
            current,
            pageSize,
            total,
          });
        }}
        size="small"
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setMetricItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建指标
          </Button>,
        ]}
      />
      {createModalVisible && (
        <MetricInfoCreateForm
          domainId={selectDomainId}
          modelId={Number(modelId)}
          createModalVisible={createModalVisible}
          metricItem={metricItem}
          onSubmit={() => {
            setCreateModalVisible(false);
            actionRef?.current?.reload();
            dispatch({
              type: 'domainManger/queryMetricList',
              payload: {
                modelId,
              },
            });
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassMetricTable);

import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm } from 'antd';
import React, { useRef, useState } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../model';
import { SENSITIVE_LEVEL_ENUM } from '../constant';
import { creatExprMetric, updateExprMetric, queryMetric, deleteMetric } from '../service';

import MetricInfoCreateForm from './MetricInfoCreateForm';

import moment from 'moment';
import styles from './style.less';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const ClassMetricTable: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { selectDomainId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [metricItem, setMetricItem] = useState<any>();
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
      domainId: selectDomainId,
    });
    const { list, pageSize, current, total } = data;
    let resData: any = {};
    if (code === 200) {
      setPagination({
        pageSize,
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
    },
    {
      dataIndex: 'name',
      title: '指标名称',
    },
    {
      dataIndex: 'bizName',
      title: '字段名称',
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
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
              key="classEditBtn"
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
                const { code } = await deleteMetric(record.id);
                if (code === 200) {
                  setMetricItem(undefined);
                  actionRef.current?.reload();
                } else {
                  message.error('删除失败');
                }
              }}
            >
              <a
                key="classEditBtn"
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

  const saveMetric = async (fieldsValue: any, reloadState: boolean = true) => {
    const queryParams = {
      domainId: selectDomainId,
      ...fieldsValue,
    };
    if (queryParams.typeParams && !queryParams.typeParams.expr) {
      message.error('度量表达式不能为空');
      return;
    }
    let saveMetricQuery = creatExprMetric;
    if (queryParams.id) {
      saveMetricQuery = updateExprMetric;
    }
    const { code, msg } = await saveMetricQuery(queryParams);
    if (code === 200) {
      message.success('编辑指标成功');
      setCreateModalVisible(false);
      if (reloadState) {
        actionRef?.current?.reload();
      }
      dispatch({
        type: 'domainManger/queryMetricList',
        payload: {
          domainId: selectDomainId,
        },
      });
      return;
    }
    message.error(msg);
  };

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft}`}
        actionRef={actionRef}
        headerTitle="指标列表"
        rowKey="id"
        search={{
          span: 4,
          defaultCollapsed: false,
          collapseRender: () => {
            return <></>;
          },
        }}
        columns={columns}
        params={{ domainId: selectDomainId }}
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
          domainId={Number(selectDomainId)}
          createModalVisible={createModalVisible}
          metricItem={metricItem}
          onSubmit={(values) => {
            saveMetric(values);
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

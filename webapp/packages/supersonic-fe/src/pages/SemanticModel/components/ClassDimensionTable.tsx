import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../model';
import { SENSITIVE_LEVEL_ENUM } from '../constant';
import {
  getDatasourceList,
  getDimensionList,
  createDimension,
  updateDimension,
  deleteDimension,
} from '../service';
import DimensionInfoModal from './DimensionInfoModal';
import moment from 'moment';
import styles from './style.less';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const ClassDimensionTable: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { selectDomainId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [dimensionItem, setDimensionItem] = useState<any>();
  const [dataSourceList, setDataSourceList] = useState<any[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });

  const actionRef = useRef<ActionType>();

  const queryDimensionList = async (params: any) => {
    const { code, data, msg } = await getDimensionList({
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

  const queryDataSourceList = async () => {
    const { code, data, msg } = await getDatasourceList({ domainId: selectDomainId });
    if (code === 200) {
      setDataSourceList(data);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDataSourceList();
  }, [selectDomainId]);

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      order: 100,
    },
    {
      dataIndex: 'name',
      title: '维度名称',
    },
    {
      dataIndex: 'bizName',
      title: '字段名称',
      order: 9,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      valueEnum: SENSITIVE_LEVEL_ENUM,
    },

    {
      dataIndex: 'datasourceName',
      title: '数据源名称',
      search: false,
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
                setDimensionItem(record);
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
                const { code } = await deleteDimension(record.id);
                if (code === 200) {
                  setDimensionItem(undefined);
                  actionRef.current?.reload();
                } else {
                  message.error('删除失败');
                }
              }}
            >
              <a
                key="classEditBtn"
                onClick={() => {
                  setDimensionItem(record);
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

  const saveDimension = async (fieldsValue: any, reloadState: boolean = true) => {
    const queryParams = {
      domainId: selectDomainId,
      type: 'categorical',
      ...fieldsValue,
    };
    let saveDimensionQuery = createDimension;
    if (queryParams.id) {
      saveDimensionQuery = updateDimension;
    }

    const { code, msg } = await saveDimensionQuery(queryParams);

    if (code === 200) {
      setCreateModalVisible(false);
      if (reloadState) {
        message.success('编辑维度成功');
        actionRef?.current?.reload();
      }
      dispatch({
        type: 'domainManger/queryDimensionList',
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
        headerTitle="维度列表"
        rowKey="id"
        columns={columns}
        request={queryDimensionList}
        pagination={pagination}
        search={{
          span: 4,
          defaultCollapsed: false,
          collapseRender: () => {
            return <></>;
          },
        }}
        onChange={(data: any) => {
          const { current, pageSize, total } = data;
          setPagination({
            current,
            pageSize,
            total,
          });
        }}
        tableAlertRender={() => {
          return false;
        }}
        size="small"
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setDimensionItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建维度
          </Button>,
        ]}
      />

      {createModalVisible && (
        <DimensionInfoModal
          bindModalVisible={createModalVisible}
          dimensionItem={dimensionItem}
          dataSourceList={dataSourceList}
          onSubmit={saveDimension}
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
}))(ClassDimensionTable);

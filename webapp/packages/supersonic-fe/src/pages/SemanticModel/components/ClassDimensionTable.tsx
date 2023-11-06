import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm, Input, Tag, Dropdown } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../model';
import { StatusEnum } from '../enum';
import { SENSITIVE_LEVEL_ENUM } from '../constant';
import {
  getDatasourceList,
  getDimensionList,
  deleteDimension,
  batchUpdateDimensionStatus,
} from '../service';
import DimensionInfoModal from './DimensionInfoModal';
import DimensionValueSettingModal from './DimensionValueSettingModal';
import { updateDimension } from '../service';
import { ISemantic, IDataSource } from '../data';
import moment from 'moment';
import styles from './style.less';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const ClassDimensionTable: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { selectModelId: modelId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [dimensionItem, setDimensionItem] = useState<ISemantic.IDimensionItem>();
  const [dataSourceList, setDataSourceList] = useState<IDataSource.IDataSourceItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [dimensionValueSettingList, setDimensionValueSettingList] = useState<
    ISemantic.IDimensionValueSettingItem[]
  >([]);
  const [dimensionValueSettingModalVisible, setDimensionValueSettingModalVisible] =
    useState<boolean>(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });

  const actionRef = useRef<ActionType>();

  const queryDimensionList = async (params: any) => {
    setLoading(true);
    const { code, data, msg } = await getDimensionList({
      ...params,
      ...pagination,
      modelId,
    });
    setLoading(false);
    const { list, pageSize, pageNum, total } = data || {};
    let resData: any = {};
    if (code === 200) {
      setPagination({
        ...pagination,
        pageSize: Math.min(pageSize, 100),
        current: pageNum,
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
    const { code, data, msg } = await getDatasourceList({ modelId });
    if (code === 200) {
      setDataSourceList(data);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDataSourceList();
  }, [modelId]);

  const updateDimensionStatus = async (dimensionData: ISemantic.IDimensionItem) => {
    const { code, msg } = await updateDimension(dimensionData);
    if (code === 200) {
      actionRef?.current?.reload();
      dispatch({
        type: 'domainManger/queryDimensionList',
        payload: {
          modelId,
        },
      });
      return;
    }
    message.error(msg);
  };

  const queryBatchUpdateStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    setLoading(true);
    const { code, msg } = await batchUpdateDimensionStatus({
      ids,
      status,
    });
    setLoading(false);
    if (code === 200) {
      actionRef?.current?.reload();
      dispatch({
        type: 'domainManger/queryDimensionList',
        payload: {
          modelId,
        },
      });
      return;
    }
    message.error(msg);
  };

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      width: 80,
      order: 100,
      search: false,
    },
    {
      dataIndex: 'key',
      title: '维度搜索',
      hideInTable: true,
      renderFormItem: () => <Input placeholder="请输入ID/维度名称/字段名称" />,
    },
    {
      dataIndex: 'name',
      title: '维度名称',
      search: false,
    },
    {
      dataIndex: 'alias',
      title: '别名',
      width: 150,
      ellipsis: true,
      search: false,
    },
    {
      dataIndex: 'bizName',
      title: '字段名称',
      search: false,
      // order: 9,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      width: 80,
      valueEnum: SENSITIVE_LEVEL_ENUM,
    },
    {
      dataIndex: 'status',
      title: '状态',
      width: 80,
      search: false,
      render: (status) => {
        switch (status) {
          case StatusEnum.ONLINE:
            return <Tag color="success">已启用</Tag>;
          case StatusEnum.OFFLINE:
            return <Tag color="warning">未启用</Tag>;
          case StatusEnum.INITIALIZED:
            return <Tag color="processing">初始化</Tag>;
          case StatusEnum.DELETED:
            return <Tag color="default">已删除</Tag>;
          default:
            return <Tag color="default">未知</Tag>;
        }
      },
    },
    {
      dataIndex: 'datasourceName',
      title: '数据源名称',
      search: false,
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
      width: 100,
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
      width: 180,
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
          <Space className={styles.ctrlBtnContainer}>
            <Button
              key="dimensionEditBtn"
              type="link"
              onClick={() => {
                setDimensionItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </Button>
            <Button
              key="dimensionValueEditBtn"
              type="link"
              onClick={() => {
                setDimensionItem(record);
                setDimensionValueSettingModalVisible(true);
                if (Array.isArray(record.dimValueMaps)) {
                  setDimensionValueSettingList(record.dimValueMaps);
                } else {
                  setDimensionValueSettingList([]);
                }
              }}
            >
              维度值设置
            </Button>
            {record.status === StatusEnum.ONLINE ? (
              <Button
                type="link"
                key="editStatusOfflineBtn"
                onClick={() => {
                  queryBatchUpdateStatus([record.id], StatusEnum.OFFLINE);
                }}
              >
                停用
              </Button>
            ) : (
              <Button
                type="link"
                key="editStatusOnlineBtn"
                onClick={() => {
                  queryBatchUpdateStatus([record.id], StatusEnum.ONLINE);
                }}
              >
                启用
              </Button>
            )}
            <Popconfirm
              title="确认删除？"
              okText="是"
              cancelText="否"
              onConfirm={async () => {
                const { code, msg } = await deleteDimension(record.id);
                if (code === 200) {
                  setDimensionItem(undefined);
                  actionRef.current?.reload();
                } else {
                  message.error(msg);
                }
              }}
            >
              <Button
                type="link"
                key="dimensionDeleteEditBtn"
                onClick={() => {
                  setDimensionItem(record);
                }}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  const rowSelection = {
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(selectedRowKeys);
    },
  };

  const dropdownButtonItems = [
    {
      key: 'batchStart',
      label: '批量启用',
    },
    {
      key: 'batchStop',
      label: '批量停用',
    },
    {
      key: 'batchDelete',
      label: (
        <Popconfirm
          title="确定批量删除吗？"
          onConfirm={() => {
            queryBatchUpdateStatus(selectedRowKeys, StatusEnum.DELETED);
          }}
        >
          <a>批量删除</a>
        </Popconfirm>
      ),
    },
  ];

  const onMenuClick = ({ key }: { key: string }) => {
    switch (key) {
      case 'batchStart':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.ONLINE);
        break;
      case 'batchStop':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.OFFLINE);
        break;
      default:
        break;
    }
  };

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft}`}
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={queryDimensionList}
        pagination={pagination}
        loading={loading}
        search={{
          span: 4,
          defaultCollapsed: false,
          collapseRender: () => {
            return <></>;
          },
        }}
        rowSelection={{
          type: 'checkbox',
          ...rowSelection,
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
          <Dropdown.Button
            key="ctrlBtnList"
            menu={{ items: dropdownButtonItems, onClick: onMenuClick }}
          >
            批量操作
          </Dropdown.Button>,
        ]}
      />

      {createModalVisible && (
        <DimensionInfoModal
          modelId={modelId}
          bindModalVisible={createModalVisible}
          dimensionItem={dimensionItem}
          dataSourceList={dataSourceList}
          onSubmit={() => {
            setCreateModalVisible(false);
            actionRef?.current?.reload();
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                modelId,
              },
            });
            return;
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
      {dimensionValueSettingModalVisible && (
        <DimensionValueSettingModal
          dimensionValueSettingList={dimensionValueSettingList}
          open={dimensionValueSettingModalVisible}
          dimensionItem={dimensionItem}
          onCancel={() => {
            setDimensionValueSettingModalVisible(false);
          }}
          onSubmit={() => {
            actionRef?.current?.reload();
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                modelId,
              },
            });
            setDimensionValueSettingModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassDimensionTable);

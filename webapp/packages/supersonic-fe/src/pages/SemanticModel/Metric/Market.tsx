import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Space, Popconfirm, Tag, Spin, Tooltip } from 'antd';
import MetricAddClass from './components/MetricAddClass';
import React, { useRef, useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect, history, useModel } from 'umi';
import type { StateType } from '../model';
import { SENSITIVE_LEVEL_ENUM } from '../constant';
import {
  queryMetric,
  deleteMetric,
  batchUpdateMetricStatus,
  batchDownloadMetric,
  batchMetricPublish,
  batchMetricUnPublish,
} from '../service';
import MetricFilter from './components/MetricFilter';
import MetricInfoCreateForm from '../components/MetricInfoCreateForm';
import MetricCardList from './components/MetricCardList';
import NodeInfoDrawer from '../SemanticGraph/components/NodeInfoDrawer';
import { SemanticNodeType, StatusEnum } from '../enum';
import moment from 'moment';
import styles from './style.less';
import { ISemantic } from '../data';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import { ColumnsConfig } from '../components/TableColumnRender';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

type QueryMetricListParams = {
  id?: string;
  name?: string;
  bizName?: string;
  sensitiveLevel?: string;
  type?: string;
  [key: string]: any;
};

const ClassMetricTable: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { initialState = {} } = useModel('@@initialState');

  const { currentUser = {} } = initialState as any;
  const { selectDomainId, selectModelId: modelId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const defaultPagination = {
    current: 1,
    pageSize: 20,
    total: 0,
  };
  const [pagination, setPagination] = useState(defaultPagination);
  const [loading, setLoading] = useState<boolean>(false);
  const [dataSource, setDataSource] = useState<ISemantic.IMetricItem[]>([]);
  const [metricItem, setMetricItem] = useState<ISemantic.IMetricItem>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [filterParams, setFilterParams] = useState<Record<string, any>>({
    showType: localStorage.getItem('metricMarketShowType') === '1' ? true : false,
  });
  const [infoDrawerVisible, setInfoDrawerVisible] = useState<boolean>(false);

  const [downloadLoading, setDownloadLoading] = useState<boolean>(false);

  const [hasAllPermission, setHasAllPermission] = useState<boolean>(true);

  const actionRef = useRef<ActionType>();

  const [addClassVisible, setAddClassVisible] = useState<boolean>(false);

  useEffect(() => {
    queryMetricList(filterParams);
  }, []);

  const queryBatchUpdateStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    const { code, msg } = await batchUpdateMetricStatus({
      ids,
      status,
    });
    if (code === 200) {
      queryMetricList(filterParams);
      return;
    }
    message.error(msg);
  };

  const queryMetricList = async (params: QueryMetricListParams = {}, disabledLoading = false) => {
    if (!disabledLoading) {
      setLoading(true);
    }
    const { code, data, msg } = await queryMetric({
      ...pagination,
      ...params,
      createdBy: params.onlyShowMe ? currentUser.name : null,
      pageSize: params.showType ? 100 : params.pageSize || pagination.pageSize,
      isPublish: 1,
    });
    setLoading(false);
    const { list, pageSize, pageNum, total } = data || {};
    let resData: any = {};
    if (code === 200) {
      if (!params.showType) {
        setPagination({
          ...pagination,
          pageSize: Math.min(pageSize, 100),
          current: pageNum,
          total,
        });
      }

      setDataSource(list);
      resData = {
        data: list || [],
        success: true,
      };
    } else {
      message.error(msg);
      setDataSource([]);
      resData = {
        data: [],
        total: 0,
        success: false,
      };
    }
    return resData;
  };

  const deleteMetricQuery = async (id: number) => {
    const { code, msg } = await deleteMetric(id);
    if (code === 200) {
      setMetricItem(undefined);
      queryMetricList(filterParams);
    } else {
      message.error(msg);
    }
  };

  const downloadMetricQuery = async (
    ids: React.Key[],
    dateStringList: string[],
    pickerType: string,
  ) => {
    if (Array.isArray(ids) && ids.length > 0) {
      setDownloadLoading(true);
      const [startDate, endDate] = dateStringList;
      await batchDownloadMetric({
        metricIds: ids,
        dateInfo: {
          dateMode: 'BETWEEN',
          startDate,
          endDate,
          period: pickerType.toUpperCase(),
        },
      });
      setDownloadLoading(false);
    }
  };

  const handleMetricEdit = (metricItem: ISemantic.IMetricItem) => {
    setMetricItem(metricItem);
    setCreateModalVisible(true);
  };

  const queryBatchUpdatePublish = async (ids: React.Key[], status: boolean) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    const queryPublish = status ? batchMetricPublish : batchMetricUnPublish;
    const { code, msg } = await queryPublish({
      ids,
    });
    if (code === 200) {
      queryMetricList(filterParams);
      return;
    }
    message.error(msg);
  };

  const columnsConfig = ColumnsConfig();

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      width: 80,
      fixed: 'left',
      search: false,
    },
    {
      dataIndex: 'name',
      title: '指标',
      // width: '20%',
      width: 280,
      fixed: 'left',
      render: columnsConfig.indicatorInfo.render,
    },
    // {
    //   dataIndex: 'modelName',
    //   title: '所属模型',
    //   render: (_, record: any) => {
    //     if (record.hasAdminRes) {
    //       return (
    //         <a
    //           target="blank"
    //           href={`/webapp/model/${record.domainId}/${record.modelId}/metric`}
    //           // onClick={() => {
    //           //   history.push(`/model/${record.domainId}/${record.modelId}/metric`);
    //           // }}
    //         >
    //           {record.modelName}
    //         </a>
    //       );
    //     }
    //     return <> {record.modelName}</>;
    //   },
    // },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      // width: 150,
      valueEnum: SENSITIVE_LEVEL_ENUM,
      render: columnsConfig.sensitiveLevel.render,
    },
    // {
    //   dataIndex: 'isPublish',
    //   title: '是否发布',
    //   width: 100,
    //   search: false,
    //   render: (isPublish) => {
    //     switch (isPublish) {
    //       case 0:
    //         return '否';
    //       case 1:
    //         return <span style={{ color: '#1677ff' }}>是</span>;
    //       default:
    //         return <Tag color="default">未知</Tag>;
    //     }
    //   },
    // },

    {
      dataIndex: 'description',
      title: '描述',
      search: false,
      width: 300,
      render: columnsConfig.description.render,
    },
    {
      dataIndex: 'status',
      title: '状态',
      width: 180,
      search: false,
      render: columnsConfig.state.render,
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
      // width: 150,
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
      width: 180,
      render: (_, record) => {
        if (record.hasAdminRes) {
          return (
            <Space>
              <a
                key="metricEditBtn"
                onClick={() => {
                  handleMetricEdit(record);
                }}
              >
                编辑
              </a>
              {record.isPublish ? (
                <a
                  key="metricUnPublishBtn"
                  onClick={() => {
                    queryBatchUpdatePublish([record.id], false);
                  }}
                >
                  下架
                </a>
              ) : (
                <a
                  key="metricPublishBtn"
                  onClick={() => {
                    queryBatchUpdatePublish([record.id], true);
                  }}
                >
                  发布
                </a>
              )}

              <Popconfirm
                title="确认删除？"
                okText="是"
                cancelText="否"
                onConfirm={async () => {
                  deleteMetricQuery(record.id);
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
        } else {
          return <></>;
        }
      },
    },
  ];

  const handleFilterChange = async (filterParams: {
    key: string;
    sensitiveLevel: string[];
    showFilter: string[];
    type: string;
  }) => {
    const { sensitiveLevel, type, showFilter } = filterParams;
    const params: QueryMetricListParams = { ...filterParams };
    const sensitiveLevelValue = sensitiveLevel?.[0];
    const showFilterValue = showFilter?.[0];
    const typeValue = type?.[0];
    showFilterValue ? (params[showFilterValue] = true) : null;
    params.sensitiveLevel = sensitiveLevelValue;
    params.type = typeValue;
    setFilterParams(params);
    await queryMetricList(
      {
        ...params,
        ...defaultPagination,
      },
      filterParams.key ? false : true,
    );
  };

  const rowSelection = {
    onChange: (selectedRowKeys: React.Key[]) => {
      const permissionList: boolean[] = [];
      selectedRowKeys.forEach((id: React.Key) => {
        const target = dataSource.find((item) => {
          return item.id === id;
        });
        if (target) {
          permissionList.push(target.hasAdminRes);
        }
      });
      if (permissionList.includes(false)) {
        setHasAllPermission(false);
      } else {
        setHasAllPermission(true);
      }
      setSelectedRowKeys(selectedRowKeys);
    },
  };

  const onMenuClick = (key: string) => {
    switch (key) {
      case 'batchStart':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.ONLINE);
        break;
      case 'batchStop':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.OFFLINE);
        break;
      case 'batchAddClass':
        setAddClassVisible(true);
        break;
      default:
        break;
    }
  };

  return (
    <>
      <div className={styles.metricFilterWrapper}>
        <MetricFilter
          initFilterValues={filterParams}
          extraNode={
            <BatchCtrlDropDownButton
              key="ctrlBtnList"
              downloadLoading={downloadLoading}
              onDeleteConfirm={() => {
                queryBatchUpdateStatus(selectedRowKeys, StatusEnum.DELETED);
              }}
              disabledList={hasAllPermission ? [] : ['batchStart', 'batchStop', 'batchDelete']}
              extenderList={['batchAddClass']}
              onMenuClick={onMenuClick}
              onDownloadDateRangeChange={(searchDateRange, pickerType) => {
                downloadMetricQuery(selectedRowKeys, searchDateRange, pickerType);
              }}
            />
          }
          onFiltersChange={(_, values) => {
            if (_.showType !== undefined) {
              setLoading(true);
              setDataSource([]);
            }
            handleFilterChange(values);
          }}
        />
      </div>
      <>
        {filterParams.showType ? (
          <Spin spinning={loading} style={{ minHeight: 500 }}>
            <MetricCardList
              metricList={dataSource}
              disabledEdit={true}
              onMetricChange={(metricItem: ISemantic.IMetricItem) => {
                history.push(`/metric/detail/${metricItem.id}`);
              }}
              onDeleteBtnClick={(metricItem: ISemantic.IMetricItem) => {
                deleteMetricQuery(metricItem.id);
              }}
              onEditBtnClick={(metricItem: ISemantic.IMetricItem) => {
                setMetricItem(metricItem);
                setCreateModalVisible(true);
              }}
            />
          </Spin>
        ) : (
          <ProTable
            className={`${styles.metricTable}`}
            actionRef={actionRef}
            rowKey="id"
            search={false}
            dataSource={dataSource}
            columns={columns}
            pagination={pagination}
            size="large"
            scroll={{ x: 1500 }}
            tableAlertRender={() => {
              return false;
            }}
            sticky={{ offsetHeader: 0 }}
            rowSelection={{
              type: 'checkbox',
              ...rowSelection,
            }}
            loading={loading}
            onChange={(data: any) => {
              const { current, pageSize, total } = data;
              const pagin = {
                current,
                pageSize,
                total,
              };
              setPagination(pagin);
              queryMetricList({ ...pagin, ...filterParams });
            }}
            options={false}
          />
        )}
      </>

      {createModalVisible && (
        <MetricInfoCreateForm
          domainId={Number(selectDomainId)}
          createModalVisible={createModalVisible}
          modelId={modelId}
          metricItem={metricItem}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryMetricList(filterParams);
            dispatch({
              type: 'domainManger/queryMetricList',
              payload: {
                domainId: selectDomainId,
              },
            });
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
      {infoDrawerVisible && (
        <NodeInfoDrawer
          nodeData={{ ...metricItem, nodeType: SemanticNodeType.METRIC }}
          placement="right"
          onClose={() => {
            setInfoDrawerVisible(false);
          }}
          width="100%"
          open={infoDrawerVisible}
          mask={true}
          getContainer={false}
          onEditBtnClick={(nodeData: any) => {
            handleMetricEdit(nodeData);
          }}
          maskClosable={true}
          onNodeChange={({ eventName }: { eventName: string }) => {
            setInfoDrawerVisible(false);
          }}
        />
      )}
      {addClassVisible && (
        <MetricAddClass
          ids={selectedRowKeys as number[]}
          createModalVisible={addClassVisible}
          onCancel={() => {
            setAddClassVisible(false);
          }}
          onSuccess={() => {
            setAddClassVisible(false);
            queryMetricList(filterParams);
            dispatch({
              type: 'domainManger/queryMetricList',
              payload: {
                domainId: selectDomainId,
              },
            });
          }}
        />
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassMetricTable);

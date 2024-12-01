import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Space, Popconfirm, Spin } from 'antd';
import MetricAddClass from './components/MetricAddClass';
import React, { useRef, useState, useEffect } from 'react';
import { history, useModel } from '@umijs/max';
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
import { StatusEnum } from '../enum';
import moment from 'moment';
import styles from './style.less';
import { ISemantic } from '../data';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import { ColumnsConfig } from '../components/TableColumnRender';

type Props = {};

type QueryMetricListParams = {
  id?: string;
  name?: string;
  bizName?: string;
  sensitiveLevel?: string;
  type?: string;
  [key: string]: any;
};

const ClassMetricTable: React.FC<Props> = ({}) => {
  const { initialState = {} } = useModel('@@initialState');
  const { currentUser = {} } = initialState as any;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const defaultPagination = {
    current: 1,
    pageSize: 20,
    total: 0,
  };
  const [pagination, setPagination] = useState(defaultPagination);
  const [loading, setLoading] = useState<boolean>(true);
  const [dataSource, setDataSource] = useState<ISemantic.IMetricItem[]>([]);
  const [metricItem, setMetricItem] = useState<ISemantic.IMetricItem>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [filterParams, setFilterParams] = useState<Record<string, any>>({
    showType: localStorage.getItem('metricMarketShowType') === '1' ? true : false,
  });

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
    history.push(`/model/metric/edit/${metricItem.id}`);
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
      width: 280,
      fixed: 'left',
      render: columnsConfig.indicatorInfo.render,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      valueEnum: SENSITIVE_LEVEL_ENUM,
      render: columnsConfig.sensitiveLevel.render,
    },
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
          createModalVisible={createModalVisible}
          metricItem={metricItem}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryMetricList(filterParams);
          }}
          onCancel={() => {
            setCreateModalVisible(false);
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
          }}
        />
      )}
    </>
  );
};
export default ClassMetricTable;

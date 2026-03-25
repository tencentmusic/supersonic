import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Space, Popconfirm, Spin, Tag, Typography } from 'antd';
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
import dayjs from 'dayjs';
import styles from './style.less';
import { ISemantic } from '../data';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import { ColumnsConfig } from '../components/TableColumnRender';

const { Paragraph } = Typography;

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

  const marketStats = [
    {
      label: '当前结果',
      value: pagination.total,
      hint: '符合当前筛选条件的指标数',
    },
    {
      label: '核心指标',
      value: dataSource.filter((item) => `${item.sensitiveLevel}` === '2').length,
      hint: '需要重点维护与优先巡检',
    },
    {
      label: '我创建的',
      value: dataSource.filter((item) => item.createdBy === currentUser.name).length,
      hint: '当前页由我维护的指标',
    },
    {
      label: '高频引用',
      value: dataSource.filter((item) => (item.useCnt || 0) >= 10).length,
      hint: '引用频次较高，适合优先关注',
    },
  ];

  const topUsedMetric = [...dataSource].sort((a, b) => (b.useCnt || 0) - (a.useCnt || 0))[0];
  const latestUpdatedMetric = [...dataSource].sort(
    (a, b) => dayjs(b.updatedAt).valueOf() - dayjs(a.updatedAt).valueOf(),
  )[0];

  const renderMetricCatalogInfo = (_: any, record: ISemantic.IMetricItem) => (
    <div className={styles.metricCatalogCell}>
      {columnsConfig.indicatorInfo.render(_, record)}
      <Space size={[8, 8]} wrap className={styles.metricCatalogSignals}>
        {`${record.sensitiveLevel}` === '2' ? (
          <Tag color="error" className={styles.metricSignalTag}>
            核心指标
          </Tag>
        ) : null}
      </Space>
      {record.description ? (
        <Paragraph
          className={styles.metricCatalogDescription}
          ellipsis={{ tooltip: record.description, rows: 2 }}
        >
          {record.description}
        </Paragraph>
      ) : null}
      <Space size={[8, 8]} wrap className={styles.metricCatalogMeta}>
        {record.domainName ? <Tag className={styles.metricMetaTag}>{record.domainName}</Tag> : null}
        {record.modelName ? <Tag className={styles.metricMetaTag}>{record.modelName}</Tag> : null}
      </Space>
    </div>
  );

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
      width: 420,
      fixed: 'left',
      render: renderMetricCatalogInfo,
    },
    {
      dataIndex: 'status',
      title: '状态',
      width: 180,
      search: false,
      render: columnsConfig.state.render,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      valueEnum: SENSITIVE_LEVEL_ENUM,
      render: columnsConfig.sensitiveLevel.render,
    },
    {
      dataIndex: 'useCnt',
      title: '引用热度',
      width: 110,
      search: false,
      render: (_, record) => (
        <Tag className={styles.metricHeatTag}>{record.useCnt ? `${record.useCnt} 次` : '未引用'}</Tag>
      ),
    },
    {
      dataIndex: 'createdBy',
      title: '维护人',
      search: false,
      width: 120,
    },
    {
      dataIndex: 'updatedAt',
      title: '更新时间',
      width: 180,
      search: false,
      render: (value: any) => {
        return value && value !== '-' ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
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
      <div className={styles.metricOverview}>
        {marketStats.map((item) => (
          <div className={styles.metricOverviewCard} key={item.label}>
            <div className={styles.metricOverviewLabel}>{item.label}</div>
            <div className={styles.metricOverviewValue}>{item.value}</div>
            <div className={styles.metricOverviewHint}>{item.hint}</div>
          </div>
        ))}
      </div>
      <div className={styles.metricFilterWrapper}>
        <MetricFilter
          initFilterValues={filterParams}
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
          <div className={styles.metricTableSection}>
            <div className={styles.metricTableHeader}>
              <div>
                <div className={styles.metricTableTitle}>指标目录</div>
              </div>
              <div className={styles.metricTableActions}>
                <div className={styles.metricTableSummary}>
                  {topUsedMetric ? `高频指标：${topUsedMetric.name}` : '高频指标：-'}
                  {latestUpdatedMetric
                    ? ` · 最近更新：${dayjs(latestUpdatedMetric.updatedAt).format('MM-DD HH:mm')}`
                    : ''}
                  {selectedRowKeys.length ? ` · 已选择 ${selectedRowKeys.length} 项` : ''}
                </div>
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
              </div>
            </div>
            <ProTable
              className={`${styles.metricTable}`}
              actionRef={actionRef}
              rowKey="id"
              search={false}
              dataSource={dataSource}
              columns={columns}
              pagination={pagination}
              size="large"
              scroll={{ x: 1400 }}
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
          </div>
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

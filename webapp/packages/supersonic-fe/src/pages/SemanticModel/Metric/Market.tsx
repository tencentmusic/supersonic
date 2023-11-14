import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import {
  message,
  Space,
  Popconfirm,
  Tag,
  Spin,
  Dropdown,
  DatePicker,
  Popover,
  Button,
  Radio,
} from 'antd';
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
} from '../service';
import MetricFilter from './components/MetricFilter';
import MetricInfoCreateForm from '../components/MetricInfoCreateForm';
import MetricCardList from './components/MetricCardList';
import NodeInfoDrawer from '../SemanticGraph/components/NodeInfoDrawer';
import { SemanticNodeType, StatusEnum } from '../enum';
import moment, { Moment } from 'moment';
import styles from './style.less';
import { ISemantic } from '../data';
import {
  PlaySquareOutlined,
  StopOutlined,
  CloudDownloadOutlined,
  DeleteOutlined,
} from '@ant-design/icons';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const { RangePicker } = DatePicker;

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
  const [popoverOpenState, setPopoverOpenState] = useState<boolean>(false);
  const [pickerType, setPickerType] = useState<string>('day');

  const dateRangeRef = useRef<any>([]);
  const [downloadLoading, setDownloadLoading] = useState<boolean>(false);

  const actionRef = useRef<ActionType>();

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

  const downloadMetricQuery = async (ids: React.Key[], dateStringList: string[]) => {
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
      setPopoverOpenState(false);
    }
  };

  const handleMetricEdit = (metricItem: ISemantic.IMetricItem) => {
    setMetricItem(metricItem);
    setCreateModalVisible(true);
  };

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
    },
    {
      dataIndex: 'name',
      title: '指标名称',
      render: (_, record: any) => {
        return (
          <a
            onClick={() => {
              history.push(`/metric/detail/${record.id}`);
            }}
          >
            {record.name}
          </a>
        );
      },
    },
    {
      dataIndex: 'modelName',
      title: '所属模型',
      render: (_, record: any) => {
        if (record.hasAdminRes) {
          return (
            <a
              onClick={() => {
                history.replace(`/model/${record.domainId}/${record.modelId}/metric`);
              }}
            >
              {record.modelName}
            </a>
          );
        }
        return <> {record.modelName}</>;
      },
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
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
      dataIndex: 'createdBy',
      title: '创建人',
      search: false,
    },
    {
      dataIndex: 'tags',
      title: '标签',
      search: false,
      render: (tags) => {
        if (Array.isArray(tags)) {
          return (
            <Space size={2}>
              {tags.map((tag) => (
                <Tag color="blue" key={tag}>
                  {tag}
                </Tag>
              ))}
            </Space>
          );
        }
        return <>--</>;
      },
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
    sensitiveLevel: string;
    type: string;
  }) => {
    const { sensitiveLevel, type } = filterParams;
    const params: QueryMetricListParams = { ...filterParams };
    const sensitiveLevelValue = sensitiveLevel?.[0];
    const typeValue = type?.[0];

    params.sensitiveLevel = sensitiveLevelValue;
    params.type = typeValue;
    setFilterParams(params);
    await queryMetricList(params, filterParams.key ? false : true);
  };

  const rowSelection = {
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(selectedRowKeys);
    },
    getCheckboxProps: (record: ISemantic.IMetricItem) => ({
      disabled: !record.hasAdminRes,
    }),
  };

  const dropdownButtonItems: any[] = [
    {
      key: 'batchStart',
      label: '批量启用',
      icon: <PlaySquareOutlined />,
    },
    {
      key: 'batchStop',
      label: '批量停用',
      icon: <StopOutlined />,
    },
    {
      key: 'batchDownload',
      // label: '批量下载',
      label: <a>批量下载</a>,
      icon: <CloudDownloadOutlined />,
    },
    {
      type: 'divider',
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
      icon: <DeleteOutlined />,
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
      case 'batchDownload':
        setPopoverOpenState(true);
      default:
        break;
    }
  };
  const popoverConfig = {
    title: '选择下载区间',
    content: (
      <Space direction="vertical">
        <Radio.Group
          size="small"
          value={pickerType}
          onChange={(e) => {
            setPickerType(e.target.value);
          }}
        >
          <Radio.Button value="day">按日</Radio.Button>
          <Radio.Button value="week">按周</Radio.Button>
          <Radio.Button value="month">按月</Radio.Button>
        </Radio.Group>
        <RangePicker
          style={{ paddingBottom: 5 }}
          onChange={(date) => {
            dateRangeRef.current = date;
            return;
          }}
          picker={pickerType as any}
          allowClear={true}
        />
        <div style={{ marginTop: 20 }}>
          <Space>
            <Button
              type="primary"
              loading={downloadLoading}
              onClick={() => {
                const [startMoment, endMoment] = dateRangeRef.current;
                let searchDateRange = [
                  startMoment?.format('YYYY-MM-DD'),
                  endMoment?.format('YYYY-MM-DD'),
                ];
                if (pickerType === 'week') {
                  searchDateRange = [
                    startMoment?.startOf('isoWeek').format('YYYY-MM-DD'),
                    endMoment?.startOf('isoWeek').format('YYYY-MM-DD'),
                  ];
                }
                if (pickerType === 'month') {
                  searchDateRange = [
                    startMoment?.startOf('month').format('YYYY-MM-DD'),
                    endMoment?.startOf('month').format('YYYY-MM-DD'),
                  ];
                }
                downloadMetricQuery(selectedRowKeys, searchDateRange);
              }}
            >
              下载
            </Button>
          </Space>
        </div>
      </Space>
    ),
  };
  return (
    <>
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
                history.push(`/metric/detail/${metricItem.modelId}/${metricItem.bizName}`);
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
            tableAlertRender={() => {
              return false;
            }}
            rowSelection={{
              type: 'checkbox',
              ...rowSelection,
            }}
            toolBarRender={() => [
              <Popover
                content={popoverConfig?.content}
                title={popoverConfig?.title}
                trigger="click"
                key="ctrlBtnList"
                open={popoverOpenState}
                placement="bottomLeft"
                onOpenChange={(open: boolean) => {
                  setPopoverOpenState(open);
                }}
              >
                <Dropdown.Button menu={{ items: dropdownButtonItems, onClick: onMenuClick }}>
                  批量操作
                </Dropdown.Button>
              </Popover>,
            ]}
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
            size="small"
            options={{ reload: false, density: false, fullScreen: false }}
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
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassMetricTable);

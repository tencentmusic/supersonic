import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm, Input, Select, Tag } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { StatusEnum, SemanticNodeType } from '../enum';
import { useModel } from '@umijs/max';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_OPTIONS, TAG_DEFINE_TYPE } from '../constant';
import {
  queryMetric,
  deleteMetric,
  batchUpdateMetricStatus,
  batchDownloadMetric,
  batchCreateTag,
  batchMetricPublish,
  batchMetricUnPublish,
} from '../service';
import MetricInfoCreateForm from './MetricInfoCreateForm';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import BatchSensitiveLevelModal from '@/components/BatchCtrlDropDownButton/BatchSensitiveLevelModal';
import TableHeaderFilter from '@/components/TableHeaderFilter';
import styles from './style.less';
import { ISemantic } from '../data';
import { ColumnsConfig } from './TableColumnRender';
import { toMetricEditPage } from '@/pages/SemanticModel/utils';

type Props = {
  onEmptyMetricData?: () => void;
};

const ClassMetricTable: React.FC<Props> = ({ onEmptyMetricData }) => {
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const metricModel = useModel('SemanticModel.metricData');
  const { selectDomainId } = domainModel;
  const { selectModelId: modelId } = modelModel;
  const { MrefreshMetricList, setSelectMetric } = metricModel;
  const [batchSensitiveLevelOpenState, setBatchSensitiveLevelOpenState] = useState<boolean>(false);
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [metricItem, setMetricItem] = useState<ISemantic.IMetricItem>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [tableData, setTableData] = useState<ISemantic.IMetricItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const defaultPagination = {
    current: 1,
    pageSize: 20,
    total: 0,
  };
  const initState = useRef<boolean>(false);
  const [pagination, setPagination] = useState(defaultPagination);

  const [filterParams, setFilterParams] = useState<Record<string, any>>({});

  const actionRef = useRef<ActionType>();

  const [downloadLoading, setDownloadLoading] = useState<boolean>(false);

  const queryBatchUpdateStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    const { code, msg } = await batchUpdateMetricStatus({
      ids,
      status,
    });
    if (code === 200) {
      queryMetricList({ ...filterParams, ...defaultPagination });
      MrefreshMetricList({ modelId });
      return;
    }
    message.error(msg);
  };

  const queryBatchExportTag = async (ids: React.Key[]) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    setLoading(true);
    const { code, msg } = await batchCreateTag(
      ids.map((id) => {
        return {
          itemId: id,
          tagDefineType: TAG_DEFINE_TYPE.METRIC,
        };
      }),
    );
    setLoading(false);
    if (code === 200) {
      queryMetricList({ ...filterParams, ...defaultPagination });
      MrefreshMetricList({ modelId });
      return;
    }
    message.error(msg);
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
      queryMetricList({ ...filterParams, ...defaultPagination });
      MrefreshMetricList({ modelId });
      return;
    }
    message.error(msg);
  };

  useEffect(() => {
    queryMetricList({ ...filterParams, ...defaultPagination });
  }, [filterParams, modelId]);

  const queryMetricList = async (params: any) => {
    if (!modelId) {
      return;
    }
    setLoading(true);
    const { code, data, msg } = await queryMetric({
      ...pagination,
      ...params,
      modelId,
    });
    setLoading(false);
    const { list, pageSize, pageNum, total } = data || {};
    if (code === 200) {
      setPagination({
        ...pagination,
        pageSize: Math.min(pageSize, 100),
        current: pageNum,
        total,
      });

      if (list.length === 0 && !initState.current) {
        onEmptyMetricData?.();
      }
      initState.current = true;
      setTableData(list);
    } else {
      message.error(msg);
      setTableData([]);
    }
  };

  const columnsConfig = ColumnsConfig({
    indicatorInfo: {
      url: '/model/metric/:domainId/:modelId/:indicatorId',
      onNameClick: (record) => {
        setSelectMetric(record as ISemantic.IMetricItem);
      },
    },
  });

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
      // width: 280,
      fixed: 'left',
      width: '20%',
      search: false,
      render: columnsConfig.indicatorInfo.render,
    },
    {
      dataIndex: 'key',
      title: '指标搜索',
      hideInTable: true,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      // width: 100,
      valueEnum: SENSITIVE_LEVEL_ENUM,
      render: columnsConfig.sensitiveLevel.render,
    },

    {
      dataIndex: 'isTag',
      title: '是否标签',
      hideInTable: !!!process.env.SHOW_TAG,
      render: (isTag) => {
        switch (isTag) {
          case 0:
            return '否';
          case 1:
            return <span style={{ color: '#1677ff' }}>是</span>;
          default:
            return <Tag color="default">未知</Tag>;
        }
      },
    },
    {
      dataIndex: 'isPublish',
      title: '是否发布',
      // width: 90,
      search: false,
      render: (isPublish) => {
        switch (isPublish) {
          case 0:
            return '否';
          case 1:
            return <span style={{ color: '#1677ff' }}>是</span>;
          default:
            return <Tag color="default">未知</Tag>;
        }
      },
    },
    {
      dataIndex: 'status',
      title: '状态',
      // width: 100,
      search: false,
      render: columnsConfig.state.render,
    },
    {
      dataIndex: 'description',
      title: '描述',
      width: 300,
      search: false,
      render: columnsConfig.description.render,
    },
    {
      ...columnsConfig.createInfo,
    },

    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      fixed: 'right',
      width: 150,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <Button
              type="link"
              key="metricEditBtn"
              onClick={() => {
                const { domainId, modelId, id } = record;
                toMetricEditPage(domainId, modelId, id);
              }}
            >
              编辑
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
                const { code, msg } = await deleteMetric(record.id);
                if (code === 200) {
                  setMetricItem(undefined);
                  queryMetricList({ ...filterParams, ...defaultPagination });
                } else {
                  message.error(msg);
                }
              }}
            >
              <Button
                type="link"
                key="metricDeleteBtn"
                onClick={() => {
                  setMetricItem(record);
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

  const onMenuClick = (key: string) => {
    switch (key) {
      case 'batchStart':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.ONLINE);
        break;
      case 'batchStop':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.OFFLINE);
        break;
      case 'exportTagButton':
        queryBatchExportTag(selectedRowKeys);
        break;
      case 'batchPublish':
        queryBatchUpdatePublish(selectedRowKeys, true);
        break;
      case 'batchUnPublish':
        queryBatchUpdatePublish(selectedRowKeys, false);
        break;
      case 'batchSensitiveLevel':
        setBatchSensitiveLevelOpenState(true);
        break;
      default:
        break;
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

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft} ${styles.disabledSearchTable} `}
        actionRef={actionRef}
        headerTitle={
          <div style={{ marginLeft: 15 }}>
            <TableHeaderFilter
              components={
                [
                  {
                    label: '指标搜索',
                    component: (
                      <Input.Search
                        style={{ width: 280 }}
                        placeholder="请输入ID/指标名称/英文名称"
                        onSearch={(value) => {
                          setFilterParams((preState) => {
                            return {
                              ...preState,
                              key: value,
                            };
                          });
                        }}
                      />
                    ),
                  },
                  {
                    label: '敏感度',
                    component: (
                      <Select
                        style={{ width: 140 }}
                        options={SENSITIVE_LEVEL_OPTIONS}
                        placeholder="请选择敏感度"
                        allowClear
                        onChange={(value) => {
                          setFilterParams((preState) => {
                            return {
                              ...preState,
                              sensitiveLevel: value,
                            };
                          });
                        }}
                      />
                    ),
                  },
                  {
                    label: '是否为标签',
                    hidden: !!!process.env.SHOW_TAG,
                    component: (
                      <Select
                        style={{ width: 145 }}
                        placeholder="请选择标签状态"
                        allowClear
                        onChange={(value) => {
                          setFilterParams((preState) => {
                            return {
                              ...preState,
                              isTag: value,
                            };
                          });
                        }}
                        options={[
                          { value: 1, label: '是' },
                          { value: 0, label: '否' },
                        ]}
                      />
                    ),
                  },
                ].filter((item) => !!!item.hidden) as any
              }
            />
          </div>
        }
        rowKey="id"
        loading={loading}
        search={false}
        rowSelection={{
          type: 'checkbox',
          ...rowSelection,
        }}
        columns={columns}
        params={{ modelId }}
        dataSource={tableData}
        pagination={pagination}
        tableAlertRender={() => {
          return false;
        }}
        onChange={(data: any) => {
          const { current, pageSize, total } = data;
          const currentPagin = {
            current,
            pageSize,
            total,
          };
          setPagination(currentPagin);
          queryMetricList({ ...filterParams, ...currentPagin });
        }}
        scroll={{ x: 1500 }}
        sticky={{ offsetHeader: 0 }}
        size="large"
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              toMetricEditPage(selectDomainId, modelId!, 0);
              // setMetricItem(undefined);
              // setCreateModalVisible(true);
            }}
          >
            创建指标
          </Button>,
          <BatchCtrlDropDownButton
            key="ctrlBtnList"
            downloadLoading={downloadLoading}
            extenderList={[
              'batchSensitiveLevel',
              'batchPublish',
              'batchUnPublish',
              'exportTagButton',
            ]}
            onDeleteConfirm={() => {
              queryBatchUpdateStatus(selectedRowKeys, StatusEnum.DELETED);
            }}
            onMenuClick={onMenuClick}
            onDownloadDateRangeChange={(searchDateRange, pickerType) => {
              downloadMetricQuery(selectedRowKeys, searchDateRange, pickerType);
            }}
          />,
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
            queryMetricList({ ...filterParams, ...defaultPagination });
            MrefreshMetricList({ modelId });
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
      {batchSensitiveLevelOpenState && (
        <BatchSensitiveLevelModal
          ids={selectedRowKeys as number[]}
          open={batchSensitiveLevelOpenState}
          type={SemanticNodeType.METRIC}
          onCancel={() => {
            setBatchSensitiveLevelOpenState(false);
          }}
          onSubmit={() => {
            queryMetricList({ ...filterParams, ...pagination });
            setBatchSensitiveLevelOpenState(false);
          }}
        />
      )}
    </>
  );
};
export default ClassMetricTable;

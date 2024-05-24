import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm, Input, Select } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { StatusEnum } from '../../enum';
import { useModel } from '@umijs/max';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { getTagList, deleteTag, batchUpdateTagStatus } from '../../service';
import TagInfoCreateForm from './TagInfoCreateForm';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import TableHeaderFilter from '../../components/TableHeaderFilter';
import moment from 'moment';
import styles from '../style.less';
import { ISemantic } from '../../data';
import { ColumnsConfig } from '../../components/TableColumnRender';
import TagValueSettingModal from './TagValueSettingModal';

type Props = {};

const ClassTagTable: React.FC<Props> = ({}) => {
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const { selectDomainId } = domainModel;
  const { selectModelId: modelId } = modelModel;

  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [tagItem, setTagItem] = useState<ISemantic.ITagItem>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [tableData, setTableData] = useState<ISemantic.ITagItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const defaultPagination = {
    current: 1,
    pageSize: 20,
    total: 0,
  };
  const [pagination, setPagination] = useState(defaultPagination);

  const [filterParams, setFilterParams] = useState<Record<string, any>>({});

  const [tagValueSettingModalVisible, setTagValueSettingModalVisible] = useState<boolean>(false);

  const actionRef = useRef<ActionType>();

  const queryBatchUpdateStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    const { code, msg } = await batchUpdateTagStatus({
      ids,
      status,
    });
    if (code === 200) {
      queryTagList({ ...filterParams, ...defaultPagination });
      return;
    }
    message.error(msg);
  };

  useEffect(() => {
    queryTagList({ ...filterParams, ...defaultPagination });
  }, [filterParams]);

  const queryTagList = async (params: any) => {
    setLoading(true);
    const { code, data, msg } = await getTagList({
      ...pagination,
      ...params,
      modelIds: [modelId],
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
      setTableData(list);
    } else {
      message.error(msg);
      setTableData([]);
    }
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
      title: '标签',
      width: 280,
      fixed: 'left',
      // width: '30%',
      search: false,
      render: columnsConfig.indicatorInfo.render,
    },
    {
      dataIndex: 'key',
      title: '标签搜索',
      hideInTable: true,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      width: 160,
      valueEnum: SENSITIVE_LEVEL_ENUM,
      render: columnsConfig.sensitiveLevel.render,
    },

    {
      dataIndex: 'description',
      title: '描述',
      width: 300,
      search: false,
      render: columnsConfig.description.render,
    },
    {
      dataIndex: 'status',
      title: '状态',
      width: 160,
      search: false,
      render: columnsConfig.state.render,
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
      width: 150,
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
      width: 150,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <Button
              type="link"
              key="metricEditBtn"
              onClick={() => {
                setTagItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </Button>
            <Button
              key="tagValueEditBtn"
              type="link"
              onClick={() => {
                setTagItem(record);
                setTagValueSettingModalVisible(true);
              }}
            >
              标签值设置
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
                const { code, msg } = await deleteTag(record.id);
                if (code === 200) {
                  setTagItem(undefined);
                  queryTagList({ ...filterParams, ...defaultPagination });
                } else {
                  message.error(msg);
                }
              }}
            >
              <Button
                type="link"
                key="metricDeleteBtn"
                onClick={() => {
                  setTagItem(record);
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
      default:
        break;
    }
  };

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft} ${styles.disabledSearchTable} `}
        actionRef={actionRef}
        headerTitle={
          <TableHeaderFilter
            components={[
              {
                label: '标签搜索',
                component: (
                  <Input.Search
                    style={{ width: 280 }}
                    placeholder="请输入ID/标签名称/英文名称"
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
            ]}
          />
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
          queryTagList({ ...filterParams, ...currentPagin });
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
              setTagItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建标签
          </Button>,
          <BatchCtrlDropDownButton
            key="ctrlBtnList"
            onDeleteConfirm={() => {
              queryBatchUpdateStatus(selectedRowKeys, StatusEnum.DELETED);
            }}
            onMenuClick={onMenuClick}
            hiddenList={['batchDownload']}
          />,
        ]}
      />
      {createModalVisible && (
        <TagInfoCreateForm
          domainId={selectDomainId}
          modelId={Number(modelId)}
          createModalVisible={createModalVisible}
          tagItem={tagItem}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryTagList({ ...filterParams, ...defaultPagination });
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
      {tagValueSettingModalVisible && (
        <TagValueSettingModal
          open={tagValueSettingModalVisible}
          tagItem={tagItem}
          onCancel={() => {
            setTagValueSettingModalVisible(false);
          }}
          onSubmit={() => {
            setTagValueSettingModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default ClassTagTable;

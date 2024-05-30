import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Space, Popconfirm } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { useModel } from 'umi';
import { SENSITIVE_LEVEL_ENUM } from '../constant';
import { getTagList, deleteTag, batchDeleteTag, getTagObjectList } from '../service';
import TagFilter from './components/TagFilter';
import TagInfoCreateForm from './components/TagInfoCreateForm';
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
  const [dataSource, setDataSource] = useState<ISemantic.ITagItem[]>([]);
  const [tagItem, setTagItem] = useState<ISemantic.ITagItem>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [filterParams, setFilterParams] = useState<Record<string, any>>({});

  const [downloadLoading, setDownloadLoading] = useState<boolean>(false);

  const [hasAllPermission, setHasAllPermission] = useState<boolean>(true);

  const [tagObjectList, setTagObjectList] = useState<ISemantic.ITagObjectItem[]>([]);

  const actionRef = useRef<ActionType>();

  useEffect(() => {
    queryTagObjectList();
  }, []);

  const queryTagObjectList = async () => {
    const { code, msg, data } = await getTagObjectList({});
    if (code === 200) {
      setTagObjectList(data);
      // const target = data[0];
      // if (target) {
      //   queryTagList({ ...filterParams, tagObjectId: target.id });
      // }
      return;
    }
    message.error(msg);
  };

  // const getTagList = (ids: React.Key[])=>{
  //   const filterItem = dataSource.filter((item)=>{
  //     return ids.includes(item.id);
  //   });
  //   const dimension = {

  //   }
  //   filterItem.forEach((item)=>{

  //   })
  // }

  const queryBatchDeleteTag = async (ids: React.Key[]) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    const { code, msg } = await batchDeleteTag([
      {
        ids,
      },
    ]);
    if (code === 200) {
      queryTagList(filterParams);
      return;
    }
    message.error(msg);
  };

  const queryTagList = async (params: QueryMetricListParams = {}, disabledLoading = false) => {
    if (!disabledLoading) {
      setLoading(true);
    }
    if (!params.tagObjectId) {
      setLoading(false);
      setDataSource([]);
      return;
    }
    const { code, data, msg } = await getTagList({
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
    const { code, msg } = await deleteTag(id);
    if (code === 200) {
      setTagItem(undefined);
      queryTagList(filterParams);
    } else {
      message.error(msg);
    }
  };

  const handleMetricEdit = (tagItem: ISemantic.ITagItem) => {
    setTagItem(tagItem);
    setCreateModalVisible(true);
  };

  const columnsConfig = ColumnsConfig({
    indicatorInfo: {
      url: '/tag/detail/',
      starType: 'tag',
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
      title: '标签',
      width: 280,
      fixed: 'left',
      render: columnsConfig.indicatorInfo.render,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      width: 150,
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
    // {
    //   dataIndex: 'status',
    //   title: '状态',
    //   width: 180,
    //   search: false,
    //   render: columnsConfig.state.render,
    // },
    {
      dataIndex: 'domainName',
      title: '主题域',
      search: false,
    },
    {
      dataIndex: 'tagObjectName',
      title: '标签对象',
      search: false,
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
              {/* <a
                key="metricEditBtn"
                onClick={() => {
                  handleMetricEdit(record);
                }}
              >
                编辑
              </a> */}

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
                    setTagItem(record);
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
    await queryTagList(
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
    // getCheckboxProps: (record: ISemantic.ITagItem) => ({
    //   disabled: !record.hasAdminRes,
    // }),
  };

  // const onMenuClick = (key: string) => {
  //   switch (key) {
  //     case 'batchStart':
  //       queryBatchUpdateStatus(selectedRowKeys, StatusEnum.ONLINE);
  //       break;
  //     case 'batchStop':
  //       queryBatchUpdateStatus(selectedRowKeys, StatusEnum.OFFLINE);
  //       break;
  //     default:
  //       break;
  //   }
  // };

  return (
    <>
      <div className={styles.TagFilterWrapper}>
        <TagFilter
          tagObjectList={tagObjectList}
          initFilterValues={filterParams}
          extraNode={
            <BatchCtrlDropDownButton
              key="ctrlBtnList"
              downloadLoading={downloadLoading}
              onDeleteConfirm={() => {
                queryBatchDeleteTag(selectedRowKeys);
              }}
              hiddenList={['batchDownload', 'batchStart', 'batchStop']}
              disabledList={hasAllPermission ? [] : ['batchStart', 'batchDelete']}
            />
          }
          onFilterInit={(values) => {
            setFilterParams({
              ...filterParams,
              ...values,
            });
            queryTagList(values);
          }}
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
        <ProTable
          className={`${styles.tagTable}`}
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
            queryTagList({ ...pagin, ...filterParams });
          }}
          options={false}
        />
      </>

      {createModalVisible && (
        <TagInfoCreateForm
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
    </>
  );
};
export default ClassMetricTable;

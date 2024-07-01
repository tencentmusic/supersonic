import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm, Input } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { StatusEnum } from '../enum';
import { useModel } from '@umijs/max';
import { deleteModel, batchUpdateModelStatus } from '../service';
import ClassModelTypeModal from './ClassModelTypeModal';
import { ColumnsConfig } from './TableColumnRender';
import TableHeaderFilter from './TableHeaderFilter';
import moment from 'moment';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  onModelChange?: (model?: ISemantic.IModelItem) => void;
};

const ModelTable: React.FC<Props> = ({ modelList, disabledEdit = false, onModelChange }) => {
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const { selectDomainId } = domainModel;
  const { modelTableHistoryParams, setModelTableHistoryParams } = modelModel;

  const [modelItem, setModelItem] = useState<ISemantic.IModelItem>();
  const [filterParams, setFilterParams] = useState<Record<string, any>>({});
  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState(false);
  const [currentPageNumber, setCurrentPageNumber] = useState<number>(1);
  const actionRef = useRef<ActionType>();

  const [tableData, setTableData] = useState<ISemantic.IModelItem[]>([]);
  const params = modelTableHistoryParams?.[selectDomainId];

  useEffect(() => {
    if (!Array.isArray(modelList)) {
      return;
    }
    const { key } = filterParams;
    getTableData(key);
  }, [modelList, filterParams]);

  useEffect(() => {
    if (!params) {
      return;
    }
    const { pageNumber, key } = params;
    setFilterParams((preState) => {
      return {
        ...preState,
        key,
      };
    });
    setCurrentPageNumber(pageNumber);
  }, []);

  const dipatchParams = (params: Record<string, any>) => {
    setModelTableHistoryParams({
      [selectDomainId]: {
        ...params,
      },
    });
  };

  const getTableData = (key: string) => {
    if (key) {
      setTableData(modelList.filter((item) => item.name.includes(key)));
    } else {
      setTableData(modelList);
    }
  };

  const queryBatchUpdateStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    const { code, msg } = await batchUpdateModelStatus({
      ids,
      status,
    });
    if (code === 200) {
      onModelChange?.();
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
      search: false,
    },
    {
      dataIndex: 'name',
      title: '模型名称',
      search: false,
      render: (_, record) => {
        return (
          <a
            onClick={() => {
              onModelChange?.(record);
            }}
          >
            {_}
          </a>
        );
      },
    },
    {
      dataIndex: 'key',
      title: '模型搜索',
      hideInTable: true,
    },
    {
      dataIndex: 'bizName',
      title: '英文名称',
      search: false,
    },
    {
      dataIndex: 'status',
      title: '状态',
      search: false,
      render: columnsConfig.state.render,
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
  ];

  if (!disabledEdit) {
    columns.push({
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      width: 150,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <a
              key="metricEditBtn"
              onClick={() => {
                setModelItem(record);
                setCreateDataSourceModalOpen(true);
              }}
            >
              编辑
            </a>
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
                const { code, msg } = await deleteModel(record.id);
                if (code === 200) {
                  onModelChange?.();
                } else {
                  message.error(msg);
                }
              }}
            >
              <a key="modelDeleteBtn">删除</a>
            </Popconfirm>
          </Space>
        );
      },
    });
  }

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft}`}
        actionRef={actionRef}
        rowKey="id"
        search={false}
        columns={columns}
        dataSource={tableData}
        tableAlertRender={() => {
          return false;
        }}
        pagination={{
          current: currentPageNumber,
          onChange: (pageNumber) => {
            setCurrentPageNumber(pageNumber);
            dipatchParams({
              ...filterParams,
              pageNumber: `${pageNumber}`,
            });
          },
        }}
        headerTitle={
          <TableHeaderFilter
            components={[
              {
                label: '模型搜索',
                component: (
                  <Input.Search
                    style={{ width: 280 }}
                    placeholder="请输入模型名称"
                    defaultValue={params?.key}
                    onSearch={(value) => {
                      setCurrentPageNumber(1);
                      dipatchParams({
                        ...filterParams,
                        key: value,
                        pageNumber: `1`,
                      });
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
            ]}
          />
        }
        size="small"
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() =>
          disabledEdit
            ? [<></>]
            : [
                <Button
                  key="create"
                  type="primary"
                  onClick={() => {
                    setModelItem(undefined);
                    setCreateDataSourceModalOpen(true);
                  }}
                >
                  创建模型
                </Button>,
              ]
        }
      />
      {createDataSourceModalOpen && (
        <ClassModelTypeModal
          open={createDataSourceModalOpen}
          modelItem={modelItem}
          onSubmit={() => {
            onModelChange?.();
            setCreateDataSourceModalOpen(false);
          }}
          onCancel={() => {
            setCreateDataSourceModalOpen(false);
          }}
        />
      )}
    </>
  );
};
export default ModelTable;

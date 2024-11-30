import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { StatusEnum } from '../../enum';
import { useModel } from '@umijs/max';
import { deleteView, updateView, getDataSetList, getAllModelByDomainId } from '../../service';
import ViewCreateFormModal from './ViewCreateFormModal';
import moment from 'moment';
import styles from '../../components/style.less';
import { ISemantic } from '../../data';
import { ColumnsConfig } from '../../components/TableColumnRender';
import ViewSearchFormModal from './ViewSearchFormModal';
import { toDatasetEditPage } from '@/pages/SemanticModel/utils';

type Props = {
  // dataSetList: ISemantic.IDatasetItem[];
  disabledEdit?: boolean;
};

const DataSetTable: React.FC<Props> = ({ disabledEdit = false }) => {
  const domainModel = useModel('SemanticModel.domainData');
  const { selectDomainId } = domainModel;

  const [viewItem, setViewItem] = useState<ISemantic.IDatasetItem>();
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState(false);
  const [searchModalOpen, setSearchModalOpen] = useState(false);
  const [modelList, setModelList] = useState<ISemantic.IModelItem[]>([]);
  const actionRef = useRef<ActionType>();
  const [editFormStep, setEditFormStep] = useState<number>(0);

  const updateViewStatus = async (modelData: ISemantic.IDatasetItem) => {
    setSaveLoading(true);
    const { code, msg } = await updateView({
      ...modelData,
    });
    setSaveLoading(false);
    if (code === 200) {
      queryDataSetList();
    } else {
      message.error(msg);
    }
  };

  const [viewList, setViewList] = useState<ISemantic.IDatasetItem[]>();

  useEffect(() => {
    if (!selectDomainId) {
      return;
    }
    queryDataSetList();
    queryDomainAllModel();
  }, [selectDomainId]);

  const queryDataSetList = async () => {
    setLoading(true);
    const { code, data, msg } = await getDataSetList(selectDomainId);
    setLoading(false);
    if (code === 200) {
      setViewList(data);
    } else {
      message.error(msg);
    }
  };

  const queryDomainAllModel = async () => {
    const { code, data, msg } = await getAllModelByDomainId(selectDomainId);
    if (code === 200) {
      setModelList(data);
    } else {
      message.error(msg);
    }
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
      title: '数据集名称',
      search: false,
      render: (name, record) => {
        return (
          <a
            onClick={() => {
              toDatasetEditPage(record.domainId, record.id, 'relation');
              // setEditFormStep(1);
              // setViewItem(record);
              // setCreateDataSourceModalOpen(true);
            }}
          >
            {name}
          </a>
        );
      },
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
      width: 250,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <a
              key="metricEditBtn"
              onClick={() => {
                toDatasetEditPage(record.domainId, record.id);
                // setEditFormStep(0);
                // setViewItem(record);
                // setCreateDataSourceModalOpen(true);
              }}
            >
              编辑
            </a>
            <a
              key="searchEditBtn"
              onClick={() => {
                setViewItem(record);
                setSearchModalOpen(true);
              }}
            >
              查询设置
            </a>
            {record.status === StatusEnum.ONLINE ? (
              <Button
                type="link"
                key="editStatusOfflineBtn"
                onClick={() => {
                  updateViewStatus({
                    ...record,
                    status: StatusEnum.OFFLINE,
                  });
                }}
              >
                停用
              </Button>
            ) : (
              <Button
                type="link"
                key="editStatusOnlineBtn"
                onClick={() => {
                  updateViewStatus({
                    ...record,
                    status: StatusEnum.ONLINE,
                  });
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
                const { code, msg } = await deleteView(record.id);
                if (code === 200) {
                  queryDataSetList();
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
        loading={loading}
        dataSource={viewList}
        tableAlertRender={() => {
          return false;
        }}
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
                    setViewItem(undefined);
                    setCreateDataSourceModalOpen(true);
                  }}
                >
                  创建数据集
                </Button>,
              ]
        }
      />
      {createDataSourceModalOpen && (
        <ViewCreateFormModal
          step={editFormStep}
          domainId={selectDomainId}
          viewItem={viewItem}
          modelList={modelList}
          onSubmit={() => {
            queryDataSetList();
            setCreateDataSourceModalOpen(false);
          }}
          onCancel={() => {
            setCreateDataSourceModalOpen(false);
          }}
        />
      )}

      {searchModalOpen && (
        <ViewSearchFormModal
          domainId={selectDomainId}
          viewItem={viewItem}
          onSubmit={() => {
            queryDataSetList();
            setSearchModalOpen(false);
          }}
          onCancel={() => {
            setSearchModalOpen(false);
          }}
        />
      )}
    </>
  );
};
export default DataSetTable;

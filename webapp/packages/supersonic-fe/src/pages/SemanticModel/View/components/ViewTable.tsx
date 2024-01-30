import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm, Input, Tag } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { StatusEnum } from '../../enum';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { deleteView, updateView, getViewList } from '../../service';
import ViewCreateFormModal from './ViewCreateFormModal';
import moment from 'moment';
import styles from '../../components/style.less';
import { ISemantic } from '../../data';
import { ColumnsConfig } from '../../components/MetricTableColumnRender';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  dispatch: Dispatch;
  domainManger: StateType;
};

const ViewTable: React.FC<Props> = ({ disabledEdit = false, modelList, domainManger }) => {
  const { selectDomainId } = domainManger;
  const [viewItem, setViewItem] = useState<ISemantic.IViewItem>();
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState(false);
  const actionRef = useRef<ActionType>();

  const updateViewStatus = async (modelData: ISemantic.IViewItem) => {
    setSaveLoading(true);
    const { code, msg } = await updateView({
      ...modelData,
    });
    setSaveLoading(false);
    if (code === 200) {
      queryViewList();
    } else {
      message.error(msg);
    }
  };

  const [viewList, setViewList] = useState([]);

  useEffect(() => {
    queryViewList();
  }, []);

  const queryViewList = async () => {
    setLoading(true);
    const { code, data, msg } = await getViewList(selectDomainId);
    setLoading(false);
    if (code === 200) {
      setViewList(data);
    } else {
      message.error(msg);
    }
  };

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      width: 80,
      search: false,
    },
    {
      dataIndex: 'name',
      title: '视图名称',
      search: false,
      // render: (_, record) => {
      //   return <a>{_}</a>;
      // },
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
      title: '英文名称',
      search: false,
    },
    {
      dataIndex: 'status',
      title: '状态',
      search: false,
      render: ColumnsConfig.state.render,
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
                setViewItem(record);
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
                  queryViewList();
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
                  创建视图
                </Button>,
              ]
        }
      />
      {createDataSourceModalOpen && (
        <ViewCreateFormModal
          domainId={selectDomainId}
          viewItem={viewItem}
          modelList={modelList}
          onSubmit={() => {
            queryViewList();
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
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ViewTable);

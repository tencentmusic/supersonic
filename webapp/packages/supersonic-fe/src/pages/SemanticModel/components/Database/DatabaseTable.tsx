import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import DatabaseSettingModal from './DatabaseSettingModal';
import { ISemantic } from '../../data';
import { getDatabaseList, deleteDatabase } from '../../service';

import moment from 'moment';
import styles from '../style.less';

type Props = {};

const DatabaseTable: React.FC<Props> = ({}) => {
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [databaseItem, setDatabaseItem] = useState<ISemantic.IDatabaseItem>();
  const [dataBaseList, setDataBaseList] = useState<any[]>([]);

  const actionRef = useRef<ActionType>();

  const queryDatabaseList = async () => {
    const { code, data, msg } = await getDatabaseList();
    if (code === 200) {
      setDataBaseList(data);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDatabaseList();
  }, []);

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      width: 80,
    },
    {
      dataIndex: 'name',
      title: '连接名称',
    },

    {
      dataIndex: 'type',
      title: '类型',
      search: false,
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

    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      render: (_, record) => {
        if (!record.hasEditPermission) {
          return <></>;
        }
        return (
          <Space>
            <a
              key="dimensionEditBtn"
              onClick={() => {
                setDatabaseItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </a>
            <Popconfirm
              title="确认删除？"
              okText="是"
              cancelText="否"
              onConfirm={async () => {
                const { code, msg } = await deleteDatabase(record.id);
                if (code === 200) {
                  setDatabaseItem(undefined);
                  queryDatabaseList();
                } else {
                  message.error(msg);
                }
              }}
            >
              <a
                key="dimensionDeleteEditBtn"
                onClick={() => {
                  setDatabaseItem(record);
                }}
              >
                删除
              </a>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ margin: 20 }}>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft}`}
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        dataSource={dataBaseList}
        search={false}
        tableAlertRender={() => {
          return false;
        }}
        size="small"
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setDatabaseItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建数据库连接
          </Button>,
        ]}
      />
      {createModalVisible && (
        <DatabaseSettingModal
          open={createModalVisible}
          databaseItem={databaseItem}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryDatabaseList();
          }}
        />
      )}
    </div>
  );
};
export default DatabaseTable;

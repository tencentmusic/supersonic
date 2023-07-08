import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Drawer, Space, Popconfirm, Modal, Card, Row, Col } from 'antd';
import { ConsoleSqlOutlined, CoffeeOutlined } from '@ant-design/icons';
import React, { useRef, useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import DataSourceCreateForm from '../Datasource/components/DataSourceCreateForm';
import ClassDataSourceTypeModal from './ClassDataSourceTypeModal';
import type { StateType } from '../model';
import { getDatasourceList, deleteDatasource } from '../service';
import DataSource from '../Datasource';
import moment from 'moment';

const { Meta } = Card;
type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const ClassDataSourceTable: React.FC<Props> = ({ dispatch, domainManger }) => {
  const { selectDomainId, dataBaseResultColsMap, dataBaseConfig } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [dataSourceItem, setDataSourceItem] = useState<any>();
  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState(false);
  const [dataSourceModalVisible, setDataSourceModalVisible] = useState(false);
  const [fastModeSql, setFastModeSql] = useState<string>('');
  const [fastModeTableName, setFastModeTableName] = useState<string>('');

  const actionRef = useRef<ActionType>();

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
    },
    {
      dataIndex: 'name',
      title: '数据源名称',
    },
    {
      dataIndex: 'bizName',
      title: '英文名称',
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
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
        return (
          <Space>
            <a
              key="classEditBtn"
              onClick={() => {
                setDataSourceItem(record);
                if (record.datasourceDetail.queryType === 'table_query') {
                  setDataSourceModalVisible(true);
                  return;
                }
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
                const { code, msg } = await deleteDatasource(record.id);
                if (code === 200) {
                  setDataSourceItem(undefined);
                  actionRef.current?.reload();
                } else {
                  message.error(msg);
                }
              }}
            >
              <a
                key="classEditBtn"
                onClick={() => {
                  setDataSourceItem(record);
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

  const queryDataSourceList = async (params: any) => {
    dispatch({
      type: 'domainManger/setPagination',
      payload: {
        ...params,
      },
    });
    const { code, data, msg } = await getDatasourceList({ ...params });
    let resData: any = {};
    if (code === 200) {
      resData = {
        data: data || [],
        success: true,
      };
    } else {
      message.error(msg);
      resData = {
        data: [],
        total: 0,
        success: false,
      };
    }
    return resData;
  };

  const queryDataBaseExcuteSql = (tableName: string) => {
    const sql = `select * from ${tableName}`;
    setFastModeSql(sql);
    setFastModeTableName(tableName);
    dispatch({
      type: 'domainManger/queryDataBaseExcuteSql',
      payload: {
        sql,
        domainId: selectDomainId,
        tableName,
      },
    });
  };

  return (
    <>
      <ProTable
        actionRef={actionRef}
        headerTitle="数据源列表"
        rowKey="id"
        columns={columns}
        params={{ domainId: selectDomainId }}
        request={queryDataSourceList}
        pagination={false}
        search={false}
        size="small"
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setDataSourceItem(undefined);
              setCreateDataSourceModalOpen(true);
            }}
          >
            创建数据源
          </Button>,
        ]}
      />
      {
        <ClassDataSourceTypeModal
          open={createDataSourceModalOpen}
          onTypeChange={(type) => {
            if (type === 'fast') {
              setDataSourceModalVisible(true);
            } else {
              setCreateModalVisible(true);
            }
            setCreateDataSourceModalOpen(false);
          }}
        />
      }
      {dataSourceModalVisible && (
        <DataSourceCreateForm
          sql={fastModeSql}
          basicInfoFormMode="fast"
          domainId={Number(selectDomainId)}
          dataSourceItem={dataSourceItem}
          onCancel={() => {
            setDataSourceModalVisible(false);
          }}
          onDataBaseTableChange={(tableName: string) => {
            queryDataBaseExcuteSql(tableName);
          }}
          onSubmit={() => {
            setDataSourceModalVisible(false);
            setDataSourceItem(undefined);
            actionRef.current?.reload();
          }}
          createModalVisible={dataSourceModalVisible}
        />
      )}
      {createModalVisible && (
        <Drawer
          width={'100%'}
          destroyOnClose
          title="数据源编辑"
          open={true}
          onClose={() => {
            setCreateModalVisible(false);
            setDataSourceItem(undefined);
          }}
          footer={null}
        >
          <DataSource
            initialValues={dataSourceItem}
            domainId={Number(selectDomainId)}
            onSubmitSuccess={() => {
              setCreateModalVisible(false);
              setDataSourceItem(undefined);
              actionRef.current?.reload();
            }}
          />
        </Drawer>
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassDataSourceTable);

import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import LlmSettingModal from './LlmSettingModal';
import { ISemantic } from '../../data';
import { deleteLlmConfig } from '../../service';
import { getLlmList } from '@/services/system';
import dayjs from 'dayjs';

type Props = {};

const LlmTable: React.FC<Props> = ({}) => {
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [llmItem, setLlmItem] = useState<ISemantic.ILlmItem>();
  const [dataBaseList, setDataBaseList] = useState<any[]>([]);

  const actionRef = useRef<ActionType>();

  const queryLlmList = async () => {
    const { code, data, msg } = await getLlmList();
    if (code === 200) {
      setDataBaseList(data);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryLlmList();
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
      dataIndex: ['config', 'modelName'],
      title: '模型名称',
      search: false,
    },
    {
      dataIndex: ['config', 'provider'],
      title: '接口协议',
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
        return value && value !== '-' ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },

    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      width: 100,
      render: (_, record) => {
        // if (!record.hasEditPermission) {
        //   return <></>;
        // }
        return (
          <Space>
            <a
              key="dimensionEditBtn"
              onClick={() => {
                setLlmItem(record);
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
                const { code, msg } = await deleteLlmConfig(record.id);
                if (code === 200) {
                  setLlmItem(undefined);
                  queryLlmList();
                } else {
                  message.error(msg);
                }
              }}
            >
              <a
                key="dimensionDeleteEditBtn"
                onClick={() => {
                  setLlmItem(record);
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
              setLlmItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建大模型连接
          </Button>,
        ]}
      />
      {createModalVisible && (
        <LlmSettingModal
          open={createModalVisible}
          llmItem={llmItem}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryLlmList();
          }}
        />
      )}
    </div>
  );
};
export default LlmTable;

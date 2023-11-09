import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm, Input, Tag } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { StatusEnum } from '../enum';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../model';
import { deleteModel, updateModel } from '../service';

import ModelCreateFormModal from './ModelCreateFormModal';

import moment from 'moment';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  onModelChange?: (model?: ISemantic.IModelItem) => void;
  dispatch: Dispatch;
  domainManger: StateType;
};

const ModelTable: React.FC<Props> = ({
  modelList,
  domainManger,
  disabledEdit = false,
  onModelChange,
  dispatch,
}) => {
  const { selectModelId: modelId, selectDomainId } = domainManger;
  const [modelCreateFormModalVisible, setModelCreateFormModalVisible] = useState<boolean>(false);
  const [modelItem, setModelItem] = useState<ISemantic.IModelItem>();
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const actionRef = useRef<ActionType>();

  const updateModelStatus = async (modelData: ISemantic.IModelItem) => {
    setSaveLoading(true);
    const { code, msg } = await updateModel({
      ...modelData,
    });
    setSaveLoading(false);
    if (code === 200) {
      onModelChange?.();
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
      title: '指标名称',
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
      title: '指标搜索',
      hideInTable: true,
      renderFormItem: () => <Input placeholder="请输入ID/指标名称/字段名称/标签" />,
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
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <a
              key="metricEditBtn"
              onClick={() => {
                setModelItem(record);
                setModelCreateFormModalVisible(true);
              }}
            >
              编辑
            </a>
            {record.status === StatusEnum.ONLINE ? (
              <Button
                type="link"
                key="editStatusOfflineBtn"
                onClick={() => {
                  updateModelStatus({
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
                  updateModelStatus({
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
        dataSource={modelList}
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
                    setModelItem(undefined);
                    setModelCreateFormModalVisible(true);
                  }}
                >
                  创建模型
                </Button>,
              ]
        }
      />
      {modelCreateFormModalVisible && (
        <ModelCreateFormModal
          domainId={selectDomainId}
          basicInfo={modelItem}
          onSubmit={() => {
            setModelCreateFormModalVisible(false);
            onModelChange?.();
          }}
          onCancel={() => {
            setModelCreateFormModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ModelTable);

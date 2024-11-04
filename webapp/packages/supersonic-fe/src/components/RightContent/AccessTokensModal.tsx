import React, { forwardRef, useEffect, useImperativeHandle, useState } from 'react';
import { Button, Form, Input, message, Modal, Table } from 'antd';
import { useBoolean, useDynamicList, useRequest } from 'ahooks';
import {
  changePassword,
  generateAccessToken,
  getUserAccessTokens,
  removeAccessToken,
} from '@/services/user';
import { encryptPassword, encryptKey } from '@/utils/utils';
import { API } from '@/services/API';
import { EditableProTable, ProColumns } from '@ant-design/pro-components';
import { CopyOutlined } from '@ant-design/icons';

type DataSourceType = {
  id: React.Key;
  name?: string;
  token?: string;
  expireDate?: string;
  createDate?: string;
  toBeSaved?: boolean;
};

export interface IRef {
  open: () => void;
  close: () => void;
}

const ChangePasswordModal = forwardRef<IRef>((_, ref) => {
  const [open, { setTrue: openModal, setFalse: closeModal }] = useBoolean(false);
  const [dataSource, setDataSource] = useState<readonly DataSourceType[]>([]);

  const getAccessTokens = async () => {
    try {
      const res = await getUserAccessTokens();
      if (res && res.code === 200) {
        return res.data;
      } else {
        message.error(res.msg);
        return [];
      }
    } catch (error) {
      message.error('获取数据失败，原因：' + error);
      return [];
    }
  };

  useImperativeHandle(ref, () => ({
    open: () => {
      openModal();
    },
    close: () => {
      closeModal();
    },
  }));

  const columns: ProColumns<DataSourceType>[] = [
    {
      title: '名称',
      dataIndex: 'name',
      width: '15%',
      formItemProps: {
        rules: [{ required: true, message: '此项为必填项' }],
      },
      editable: (text, record, index) => !!record.toBeSaved,
    },
    {
      title: '访问令牌',
      dataIndex: 'token',
      width: '25%',
      formItemProps: (form, { rowIndex }) => {
        return {
          rules: [{ required: true, message: '此项为必填项' }],
        };
      },
      render: (text, record, index) => {
        // 脱敏处理, 点击图标可完整token
        return record.toBeSaved ? (
          text
        ) : (
          <>
            {record.token ? record.token.slice(0, 5) + '********' + record.token.slice(-5) : ''}
            <Button
              type="link"
              size="small"
              onClick={() => {
                navigator.clipboard.writeText(record.token || '');
                message.info('已复制到剪贴板');
              }}
            >
              <CopyOutlined />
            </Button>
          </>
        );
      },
      editable: false,
    },
    {
      title: '过期时间',
      dataIndex: 'expireDate',
      valueType: 'date',
      width: '20%',
      formItemProps: {
        rules: [{ required: true, message: '此项为必填项' }],
      },
      editable: (text, record, index) => !!record.toBeSaved,
    },
    {
      title: '创建时间',
      dataIndex: 'createDate',
      valueType: 'date',
      editable: false,
      width: '20%',
    },
    {
      title: '操作',
      valueType: 'option',
      width: '15%',
      render: (text, record, _, action) => [
        <a
          key="delete"
          onClick={() => {
            Modal.confirm({
              title: '删除访问令牌',
              content: '确定删除此访问令牌吗？',
              onOk: async () => {
                const res = await removeAccessToken(record.id as number);

                if (res && res.code !== 200) {
                  message.error('删除失败，原因：' + res.msg);
                  return;
                }

                setDataSource(dataSource.filter((item) => item.id !== record.id));
                message.success('删除成功');
              },
            });
          }}
        >
          删除
        </a>,
      ],
    },
  ];

  return (
    <Modal
      title="访问令牌"
      open={open}
      onClose={closeModal}
      onCancel={closeModal}
      width={1200}
      footer={false}
      destroyOnClose
    >
      <EditableProTable<DataSourceType>
        rowKey="id"
        recordCreatorProps={{
          position: 'bottom',
          creatorButtonText: '新增访问令牌',
          record: () => ({ id: (Math.random() * 1000000).toFixed(0), toBeSaved: true }),
        }}
        loading={false}
        columns={columns}
        request={async () => {
          const data = await getAccessTokens();
          return {
            data,
            total: data.length,
            success: true,
          };
        }}
        value={dataSource}
        onChange={setDataSource}
        editable={{
          type: 'single',
          onSave: async (rowKey, data, row) => {
            await generateAccessToken({
              name: data.name!,
              expireTime: new Date(data.expireDate!).getTime() - new Date().getTime(),
            });

            const newTokens = await getAccessTokens();
            setTimeout(() => {
              setDataSource(newTokens);
            }, 100);
          },
        }}
      />
    </Modal>
  );
});

export default ChangePasswordModal;

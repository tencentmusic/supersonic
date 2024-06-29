import type { ProColumns } from '@ant-design/pro-components';
import { EditableProTable } from '@ant-design/pro-components';
import React, { useState } from 'react';
import { MemoryType, ReviewEnum, StatusEnum } from './type';
import { getMemeoryList, saveMemory } from './service';
import { Popover, Input, Badge, Radio } from 'antd';
import styles from './style.less';

const { TextArea } = Input;
const RadioGroup = Radio.Group;

const MemorySection = () => {
  const [editableKeys, setEditableRowKeys] = useState<React.Key[]>([]);
  const [dataSource, setDataSource] = useState<readonly MemoryType[]>([]);
  const [loading, setLoading] = useState(false);

  const columns: ProColumns<MemoryType>[] = [
    {
      title: '用户问题',
      dataIndex: 'question',
      readonly: true,
    },
    {
      title: 'Schema映射',
      dataIndex: 'dbSchema',
      width: 300,
      valueType: 'textarea',
      renderFormItem: (_, { record }) => (
        <TextArea rows={3} disabled={record?.status === StatusEnum.ENABLED} />
      ),
    },
    {
      title: '大模型解析SQL',
      dataIndex: 's2sql',
      width: 300,
      valueType: 'textarea',
      renderFormItem: (_, { record }) => (
        <TextArea rows={3} disabled={record?.status === StatusEnum.ENABLED} />
      ),
    },
    {
      title: '大模型评估意见',
      dataIndex: 'llmReviewCmt',
      readonly: true,
      render: (value) => {
        return (
          <Popover trigger="hover" content={<div className={styles.commentPopover}>{value}</div>}>
            <div className={styles.reviewComment}>{value}</div>
          </Popover>
        );
      },
    },
    {
      title: '大模型评估结果',
      key: 'llmReviewRet',
      dataIndex: 'llmReviewRet',
      readonly: true,
      width: 150,
      valueEnum: {
        [ReviewEnum.POSITIVE]: {
          text: '正确',
          status: 'Success',
        },
        [ReviewEnum.NEGATIVE]: {
          text: '错误',
          status: 'Error',
        },
      },
    },
    {
      title: '管理员评估意见',
      dataIndex: 'humanReviewCmt',
      valueType: 'textarea',
      renderFormItem: (_, { record }) => (
        <TextArea rows={12} disabled={record?.status === StatusEnum.ENABLED} />
      ),
      render: (value) => {
        return value === '-' ? (
          '-'
        ) : (
          <Popover trigger="hover" content={<div className={styles.commentPopover}>{value}</div>}>
            <div className={styles.reviewComment}>{value}</div>
          </Popover>
        );
      },
    },
    {
      title: '管理员评估结果',
      key: 'humanReviewRet',
      dataIndex: 'humanReviewRet',
      width: 150,
      valueType: 'radio',
      renderFormItem: (_, { record }) => (
        <RadioGroup
          disabled={record?.status === StatusEnum.ENABLED}
          options={[
            { label: '正确', value: ReviewEnum.POSITIVE },
            { label: '错误', value: ReviewEnum.NEGATIVE },
          ]}
        />
      ),
      valueEnum: {
        [ReviewEnum.POSITIVE]: {
          text: '正确',
          status: 'Success',
        },
        [ReviewEnum.NEGATIVE]: {
          text: '错误',
          status: 'Error',
        },
      },
    },
    {
      title: '状态',
      key: 'status',
      dataIndex: 'status',
      valueType: 'radio',
      width: 120,
      tooltip:
        '若启用，将会把这条记录加入到向量库中作为样例召回供大模型参考以及作为相似问题推荐给用户',
      valueEnum: {
        [StatusEnum.PENDING]: { text: '待定' },
        [StatusEnum.ENABLED]: {
          text: '启用',
        },
        [StatusEnum.DISABLED]: {
          text: '禁用',
        },
      },
      render: (_, record) => {
        const { status } = record;
        if (status === StatusEnum.PENDING) {
          return <Badge status="default" text="待定" />;
        } else if (status === StatusEnum.ENABLED) {
          return <Badge status="success" text="已启用" />;
        } else {
          return <Badge status="error" text="已禁用" />;
        }
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 150,
      render: (text, record, _, action) => [
        <a
          key="editable"
          onClick={() => {
            action?.startEditable?.(record.id);
          }}
        >
          编辑
        </a>,
      ],
    },
  ];

  const loadMemoryList = async () => {
    setLoading(true);
    const res = await getMemeoryList();
    setLoading(false);
    const { list, total } = res.data;
    return {
      data: list,
      total: total,
      success: true,
    };
  };

  const onSave = async (_: any, data: any) => {
    await saveMemory(data);
  };

  return (
    <EditableProTable<MemoryType>
      rowKey="id"
      className={styles.memorySection}
      recordCreatorProps={false}
      loading={loading}
      columns={columns}
      request={loadMemoryList}
      value={dataSource}
      onChange={setDataSource}
      pagination={{}}
      editable={{
        type: 'multiple',
        editableKeys,
        actionRender: (row, config, defaultDom) => [defaultDom.save, defaultDom.cancel],
        onSave,
        onChange: setEditableRowKeys,
      }}
    />
  );
};

export default MemorySection;

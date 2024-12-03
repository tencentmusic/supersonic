import type { ProColumns } from '@ant-design/pro-components';
import { EditableProTable } from '@ant-design/pro-components';
import React, { useState } from 'react';
import { MemoryType, ReviewEnum, StatusEnum } from './type';
import { getMemeoryList, saveMemory, batchDeleteMemory } from './service';
import { Popover, Input, Badge, Radio, Select, Button, message } from 'antd';
import MemorySettingModal from './MemorySettingModal';
import styles from './style.less';
import { isArrayOfValues } from '@/utils/utils';
import dayjs from 'dayjs';
const { TextArea } = Input;
const RadioGroup = Radio.Group;

type Props = {
  agentId?: number;
};

const MemorySection = ({ agentId }: Props) => {
  const [editableKeys, setEditableRowKeys] = useState<React.Key[]>([]);
  const [dataSource, setDataSource] = useState<readonly MemoryType[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<any>({});
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const { question, status, llmReviewRet, humanReviewRet } = filters;
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);

  const defaultPagination = {
    current: 1,
    pageSize: 10,
    total: 0,
  };
  const [pagination, setPagination] = useState(defaultPagination);

  const deleteTermConfig = async (ids: number[]) => {
    const { code, msg } = await batchDeleteMemory(ids);
    if (code === 200) {
      loadMemoryList();
      setSelectedRowKeys([]);
    } else {
      message.error(msg);
    }
  };

  const columns: ProColumns<MemoryType>[] = [
    {
      title: '用户问题',
      dataIndex: 'question',
      readonly: true,
    },
    {
      title: 'Schema映射',
      dataIndex: 'dbSchema',
      width: 220,
      valueType: 'textarea',
      renderFormItem: (_, { record }) => (
        <TextArea rows={3} disabled={record?.status === StatusEnum.ENABLED} />
      ),
    },
    {
      title: '语义S2SQL',
      dataIndex: 's2sql',
      width: 220,
      valueType: 'textarea',
      renderFormItem: (_, { record }) => (
        <TextArea rows={3} disabled={record?.status === StatusEnum.ENABLED} />
      ),
    },
    {
      title: '大模型评估意见',
      dataIndex: 'llmReviewCmt',
      readonly: true,
      width: 200,
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
      sorter: true,
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
      sorter: true,
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
      sorter: true,
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
      dataIndex: 'updatedAt',
      title: '更新时间',
      editable: false,
      search: false,
      sorter: true,
      render: (value: any) => {
        return value && value !== '-' ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },
    {
      dataIndex: 'createdAt',
      title: '创建时间',
      search: false,
      editable: false,
      sorter: true,
      render: (value: any) => {
        return value && value !== '-' ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
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

  const sortValueMap: any = {
    ascend: 'asc',
    descend: 'desc',
  };

  const sortKeyMap: any = {
    updatedAt: 'updated_at',
    createdAt: 'created_at',
  };

  const loadMemoryList = async (
    {
      filtersValue,
      current,
      pageSize,
    }: { filtersValue?: any; current?: number; pageSize?: number } = {},
    sort?: any,
  ) => {
    if (!agentId) {
      return {
        data: [],
        total: 0,
        success: true,
      };
    }
    let sortParams: { orderCondition: string; sort: 'desc' | 'asc' } = {
      orderCondition: 'updated_at',
      sort: 'desc',
    };

    if (sort) {
      const target = Object.entries(sort)[0];
      if (target) {
        const [sortKey, sortValue] = target;
        if (sortKey && sortValue) {
          sortParams = {
            orderCondition: sortKeyMap[sortKey] || sortKey,
            sort: sortValueMap[sortValue] || 'desc',
          };
        }
      }
    }
    setLoading(true);
    const res = await getMemeoryList({
      agentId,
      chatMemoryFilter: filtersValue || filters,
      current: current || 1,
      pageSize,
      ...sortParams,
    });
    setLoading(false);
    const { list, total, pageNum } = res.data;
    setDataSource(list);
    setPagination({
      pageSize,
      current: pageNum,
      total,
    });
    return {
      data: list,
      total: total,
      success: true,
    };
  };
  const rowSelection = {
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(selectedRowKeys as number[]);
    },
  };

  const onSave = async (_: any, data: any) => {
    await saveMemory(data);
  };

  return (
    <div className={styles.memorySection}>
      <div className={styles.filterSection}>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>用户问题</div>
          <Input
            className={styles.filterItemControl}
            placeholder="请输入用户问题"
            value={question}
            onChange={(e) => {
              setFilters({ ...filters, question: e.target.value });
            }}
          />
        </div>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>大模型评估结果</div>
          <Select
            className={styles.filterItemControl}
            placeholder="请选择大模型评估结果"
            options={[
              { label: '正确', value: ReviewEnum.POSITIVE },
              { label: '错误', value: ReviewEnum.NEGATIVE },
            ]}
            value={llmReviewRet}
            allowClear
            onChange={(value: ReviewEnum) => {
              setFilters({ ...filters, llmReviewRet: value });
            }}
          />
        </div>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>管理员评估结果</div>
          <Select
            className={styles.filterItemControl}
            placeholder="请选择管理员评估结果"
            options={[
              { label: '正确', value: ReviewEnum.POSITIVE },
              { label: '错误', value: ReviewEnum.NEGATIVE },
            ]}
            value={humanReviewRet}
            allowClear
            onChange={(value: ReviewEnum) => {
              setFilters({ ...filters, humanReviewRet: value });
            }}
          />
        </div>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>状态</div>
          <Select
            className={styles.filterItemControl}
            placeholder="请选择状态"
            options={[
              { label: '待定', value: StatusEnum.PENDING },
              { label: '已启用', value: StatusEnum.ENABLED },
              { label: '已禁用', value: StatusEnum.DISABLED },
            ]}
            value={status}
            allowClear
            onChange={(value: ReviewEnum) => {
              setFilters({ ...filters, status: value });
            }}
          />
        </div>
      </div>
      <div className={styles.search}>
        <Button onClick={() => setFilters({})}>重置</Button>
        <Button type="primary" onClick={() => loadMemoryList()}>
          查询
        </Button>
        {agentId && (
          <Button
            type="primary"
            onClick={() => {
              setCreateModalVisible(true);
            }}
          >
            新增
          </Button>
        )}

        <Button
          key="batchDelete"
          type="primary"
          disabled={!isArrayOfValues(selectedRowKeys)}
          onClick={() => {
            deleteTermConfig(selectedRowKeys);
          }}
        >
          批量删除
        </Button>
      </div>
      <EditableProTable<MemoryType>
        rowKey="id"
        recordCreatorProps={false}
        loading={loading}
        columns={columns}
        request={loadMemoryList}
        rowSelection={{
          type: 'checkbox',
          ...rowSelection,
        }}
        value={dataSource}
        onChange={setDataSource}
        // pagination={{ pageSize: 10 }}
        pagination={pagination}
        sticky={{ offsetHeader: 0 }}
        editable={{
          type: 'multiple',
          editableKeys,
          actionRender: (row, config, defaultDom) => [defaultDom.save, defaultDom.cancel],
          onSave,
          onChange: setEditableRowKeys,
        }}
      />
      {createModalVisible && agentId && (
        <MemorySettingModal
          open={true}
          agentId={agentId}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
          onSubmit={() => {
            setCreateModalVisible(false);
            loadMemoryList();
          }}
        />
      )}
    </div>
  );
};

export default MemorySection;

import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { StatusEnum } from '../../enum';
import { useModel } from '@umijs/max';
import { getTagObjectList, deleteTagObject } from '../../service';
import dayjs from 'dayjs';
import TagObjectCreateForm from './TagObjectCreateForm';
import styles from '../style.less';
import { ISemantic } from '../../data';
import { ColumnsConfig } from '../../components/TableColumnRender';

type Props = {};

const TagObjectTable: React.FC<Props> = ({}) => {
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const { selectDomainId } = domainModel;
  const { selectModelId: modelId } = modelModel;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [tagItem, setTagItem] = useState<ISemantic.ITagItem>();

  const [tableData, setTableData] = useState<ISemantic.ITagItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  const actionRef = useRef<ActionType>();

  useEffect(() => {
    queryTagList();
  }, [selectDomainId]);

  const queryTagList = async () => {
    setLoading(true);
    const { code, data, msg } = await getTagObjectList({
      domainId: selectDomainId,
      status: StatusEnum.ONLINE,
    });
    setLoading(false);
    if (code === 200) {
      setTableData(data);
    } else {
      message.error(msg);
      setTableData([]);
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
      title: '标签对象',
      search: false,
    },

    {
      dataIndex: 'description',
      title: '描述',
      search: false,
      render: columnsConfig.description.render,
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
      width: 150,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <Button
              type="link"
              key="metricEditBtn"
              onClick={() => {
                setTagItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </Button>
            <Popconfirm
              title="确认删除？"
              okText="是"
              cancelText="否"
              onConfirm={async () => {
                const { code, msg } = await deleteTagObject(record.id);
                if (code === 200) {
                  setTagItem(undefined);
                  queryTagList();
                } else {
                  message.error(msg);
                }
              }}
            >
              <Button
                type="link"
                key="metricDeleteBtn"
                onClick={() => {
                  setTagItem(record);
                }}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft} ${styles.disabledSearchTable} `}
        actionRef={actionRef}
        rowKey="id"
        size="small"
        loading={loading}
        search={false}
        columns={columns}
        params={{ modelId }}
        dataSource={tableData}
        tableAlertRender={() => {
          return false;
        }}
        sticky={{ offsetHeader: 0 }}
        options={{ reload: false, density: false, fullScreen: false }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setTagItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建标签对象
          </Button>,
        ]}
      />
      {createModalVisible && (
        <TagObjectCreateForm
          domainId={selectDomainId}
          createModalVisible={createModalVisible}
          tagItem={tagItem}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryTagList();
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default TagObjectTable;

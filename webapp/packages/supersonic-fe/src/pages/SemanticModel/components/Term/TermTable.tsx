import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { message, Button, Space, Popconfirm, Typography } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { useModel } from '@umijs/max';
import { getTermList, saveOrUpdate, deleteTerm } from '../../service';
import dayjs from 'dayjs';
import styles from '../style.less';
import { ISemantic } from '../../data';
import TermCreateForm from './TermCreateForm';

const { Paragraph } = Typography;

type Props = {};

const TermTable: React.FC<Props> = ({}) => {
  const domainModel = useModel('SemanticModel.domainData');
  const { selectDomainId } = domainModel;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [termItem, setTermItem] = useState<ISemantic.ITermItem>();

  const [tableData, setTableData] = useState<ISemantic.ITermItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  const actionRef = useRef<ActionType>();

  useEffect(() => {
    queryTermList();
  }, [selectDomainId]);

  const queryTermList = async () => {
    setLoading(true);
    const { code, data, msg } = await getTermList(selectDomainId);
    setLoading(false);
    if (code === 200) {
      setTableData(data || []);
    } else {
      message.error(msg);
      setTableData([]);
    }
  };

  const queryTermConfig = async (terms: ISemantic.ITermItem) => {
    const { code, msg } = await saveOrUpdate({
      domainId: selectDomainId,
      ...terms,
    });
    setLoading(false);
    if (code === 200) {
      queryTermList();
    } else {
      message.error(msg);
    }
  };

  const deleteTermConfig = async (termItem: ISemantic.ITermItem) => {
    const { code, msg } = await deleteTerm(termItem.id);
    if (code === 200) {
      queryTermList();
    } else {
      message.error(msg);
    }
  };

  const columns: ProColumns[] = [
    {
      dataIndex: 'name',
      title: '名称',
      search: false,
    },
    {
      dataIndex: 'alias',
      title: '近义词',
      search: false,
      render: (_) => {
        const alias = Array.isArray(_) ? _.join(',') : '-';
        return (
          <Paragraph ellipsis={{ tooltip: alias, rows: 3 }} style={{ width: 350, marginBottom: 0 }}>
            {alias}
          </Paragraph>
        );
      },
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
      dataIndex: 'description',
      title: '描述',
      search: false,
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
                setTermItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </Button>
            <Popconfirm
              title="确认删除？"
              okText="是"
              cancelText="否"
              onConfirm={() => {
                deleteTermConfig(record);
              }}
            >
              <Button type="link" key="metricDeleteBtn">
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
        className={`${styles.classTable}  ${styles.disabledSearchTable} `}
        actionRef={actionRef}
        rowKey="id"
        size="small"
        loading={loading}
        search={false}
        columns={columns}
        dataSource={tableData}
        tableAlertRender={() => {
          return false;
        }}
        sticky={{ offsetHeader: 0 }}
        options={false}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setTermItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建术语
          </Button>,
        ]}
      />
      {createModalVisible && (
        <TermCreateForm
          createModalVisible={createModalVisible}
          termItem={termItem}
          onSubmit={(termData) => {
            queryTermConfig(termData);
            setCreateModalVisible(false);
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default TermTable;

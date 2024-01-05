import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm, Input } from 'antd';
import React, { useRef, useState } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getCommonDimensionList, deleteCommonDimension } from '../../service';
import CommonDimensionInfoModal from './CommonDimensionInfoModal';
import { ISemantic } from '../../data';
import moment from 'moment';
import styles from '../style.less';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const CommonDimensionTable: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { selectDomainId: domainId, dimensionList } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [dimensionItem, setDimensionItem] = useState<ISemantic.IDimensionItem>();
  const [loading, setLoading] = useState<boolean>(false);

  const actionRef = useRef<ActionType>();

  const queryDimensionList = async () => {
    setLoading(true);
    const { code, data, msg } = await getCommonDimensionList(domainId);
    setLoading(false);
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

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      width: 80,
      order: 100,
      search: false,
    },
    {
      dataIndex: 'key',
      title: '维度搜索',
      hideInTable: true,
      renderFormItem: () => <Input placeholder="请输入ID/维度名称/英文名称" />,
    },
    {
      dataIndex: 'name',
      title: '维度名称',
      search: false,
    },
    {
      dataIndex: 'bizName',
      title: '英文名称',
      search: false,
      // order: 9,
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
      width: 100,
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
      width: 180,
      search: false,
      render: (value: any) => {
        return value && value !== '-' ? moment(value).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },

    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      width: 200,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <Button
              key="dimensionEditBtn"
              type="link"
              onClick={() => {
                setDimensionItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </Button>
            <Popconfirm
              title="删除会自动解除所关联的维度，是否确认？"
              okText="是"
              cancelText="否"
              placement="left"
              onConfirm={async () => {
                const { code, msg } = await deleteCommonDimension(record.id);
                if (code === 200) {
                  setDimensionItem(undefined);
                  actionRef.current?.reload();
                } else {
                  message.error(msg);
                }
              }}
            >
              <Button
                type="link"
                key="dimensionDeleteEditBtn"
                onClick={() => {
                  setDimensionItem(record);
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
        style={{ marginTop: 15 }}
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft}`}
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={queryDimensionList}
        loading={loading}
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
              setDimensionItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建公共维度
          </Button>,
        ]}
      />

      {createModalVisible && (
        <CommonDimensionInfoModal
          domainId={domainId}
          bindModalVisible={createModalVisible}
          dimensionItem={dimensionItem}
          dimensionList={dimensionList}
          onSubmit={() => {
            setCreateModalVisible(false);
            actionRef?.current?.reload();
            return;
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(CommonDimensionTable);

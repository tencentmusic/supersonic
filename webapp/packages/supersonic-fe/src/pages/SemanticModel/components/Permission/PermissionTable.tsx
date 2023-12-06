import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm, Tooltip } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getGroupAuthInfo, removeGroupAuth } from '../../service';
import { getOrganizationTree } from '@/components/SelectPartner/service';
import { getAllUser } from '@/components/SelectTMEPerson/service';
import PermissionCreateDrawer from './PermissionCreateDrawer';
import { findDepartmentTree } from '@/pages/SemanticModel/utils';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const PermissionTable: React.FC<Props> = ({ domainManger }) => {
  const { APP_TARGET } = process.env;
  const isInner = APP_TARGET === 'inner';
  const { dimensionList, metricList, selectModelId: modelId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);

  const [permissonData, setPermissonData] = useState<any>({});

  const [intentionList, setIntentionList] = useState<any[]>([]);

  const [departmentTreeData, setDepartmentTreeData] = useState<any[]>([]);
  const [tmePerson, setTmePerson] = useState<any[]>([]);

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const actionRef = useRef<ActionType>();

  const queryListData = async () => {
    const { code, data } = await getGroupAuthInfo(modelId);
    if (code === 200) {
      setIntentionList(data);
      return;
    }
    message.error('获取主题域解析词失败');
  };

  useEffect(() => {
    if (modelId) {
      queryListData();
    }
  }, [modelId]);

  const queryDepartmentData = async () => {
    const { code, data } = await getOrganizationTree();
    if (code === 200 || code === '0') {
      setDepartmentTreeData(data);
    }
  };

  const queryTmePersonData = async () => {
    const { code, data } = await getAllUser();
    if (code === 200 || Number(code) === 0) {
      setTmePerson(data);
    }
  };
  useEffect(() => {
    if (isInner) {
      queryDepartmentData();
    }
    queryTmePersonData();
  }, []);

  const columns: ProColumns[] = [
    {
      dataIndex: 'groupId',
      title: 'ID',
      width: 50,
    },
    {
      dataIndex: 'name',
      title: '名称',
      width: 150,
    },
    {
      dataIndex: 'departmentPermission',
      title: '授权组织',
      ellipsis: {
        showTitle: false,
      },
      hideInTable: !isInner,
      width: 200,
      render: (_, record: any) => {
        const { authorizedDepartmentIds = [] } = record;
        const departmentNameList = authorizedDepartmentIds.reduce(
          (departmentNames: string[], id: string) => {
            const department = findDepartmentTree(departmentTreeData, id);
            if (department) {
              departmentNames.push(department.name);
            }
            return departmentNames;
          },
          [],
        );
        const words = departmentNameList.join(',');
        return (
          <Tooltip placement="topLeft" title={words}>
            {words}
          </Tooltip>
        );
      },
    },
    {
      dataIndex: 'personPermission',
      title: '授权个人',
      ellipsis: {
        showTitle: false,
      },
      // width: 200,
      render: (_, record: any) => {
        const { authorizedUsers = [] } = record;
        const personNameList = tmePerson.reduce((enNames: string[], item: any) => {
          const hasPerson = authorizedUsers.includes(item.name);
          if (hasPerson) {
            enNames.push(item.displayName);
          }
          return enNames;
        }, []);
        const words = personNameList.join(',');
        return (
          <Tooltip placement="topLeft" title={words}>
            {words}
          </Tooltip>
        );
      },
    },
    {
      dataIndex: 'columnPermission',
      title: '列权限',
      // width: 400,
      ellipsis: {
        showTitle: false,
      },
      render: (_, record: any) => {
        const { authRules } = record;
        const target = authRules?.[0];
        if (target) {
          const { dimensions, metrics } = target;
          let dimensionNameList: string[] = [];
          let metricsNameList: string[] = [];
          if (Array.isArray(dimensions)) {
            dimensionNameList = dimensionList.reduce((enNameList: string[], item: any) => {
              const { bizName, name } = item;
              if (dimensions.includes(bizName)) {
                enNameList.push(name);
              }
              return enNameList;
            }, []);
          }
          if (Array.isArray(metrics)) {
            metricsNameList = metricList.reduce((enNameList: string[], item: any) => {
              const { bizName, name } = item;
              if (metrics.includes(bizName)) {
                enNameList.push(name);
              }
              return enNameList;
            }, []);
          }
          const words = [...dimensionNameList, ...metricsNameList].join(',');
          return (
            <Tooltip placement="topLeft" title={words}>
              {words}
            </Tooltip>
          );
        }
        return <> - </>;
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
              key="permissionEditBtn"
              onClick={() => {
                setPermissonData(record);
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
                const { code, msg } = await removeGroupAuth({
                  modelId: record.modelId,
                  groupId: record.groupId,
                });
                if (code === 200) {
                  setPermissonData({});
                  queryListData();
                } else {
                  message.error(msg);
                }
              }}
            >
              <a
                key="permissionDeleteBtn"
                onClick={() => {
                  setPermissonData(record);
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
    <>
      <ProTable
        actionRef={actionRef}
        headerTitle="资源列表"
        rowKey="groupId"
        columns={columns}
        search={false}
        dataSource={intentionList}
        pagination={pagination}
        onChange={(data: any) => {
          const { current, pageSize, total } = data;
          setPagination({
            current,
            pageSize,
            total,
          });
        }}
        size="small"
        options={false}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setPermissonData({});
              setCreateModalVisible(true);
            }}
          >
            新建授权
          </Button>,
        ]}
      />
      {createModalVisible && (
        <PermissionCreateDrawer
          visible={createModalVisible}
          permissonData={permissonData}
          onSubmit={() => {
            queryListData();
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
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(PermissionTable);

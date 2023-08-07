import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../model';
import { SENSITIVE_LEVEL_ENUM } from '../constant';
import { queryMetric } from '../service';
import MetricFilter from './components/MetricFilter';

import moment from 'moment';
import styles from './style.less';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

type QueryMetricListParams = {
  id?: string;
  name?: string;
  bizName?: string;
  sensitiveLevel?: string;
  type?: string;
  [key: string]: any;
};

const ClassMetricTable: React.FC<Props> = () => {
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const [loading, setLoading] = useState<boolean>(false);
  const [dataSource, setDataSource] = useState<any[]>([]);
  const [filterParams, setFilterParams] = useState<Record<string, any>>({});
  const actionRef = useRef<ActionType>();

  useEffect(() => {
    queryMetricList();
  }, []);

  const queryMetricList = async (params: QueryMetricListParams = {}) => {
    setLoading(true);
    const { code, data, msg } = await queryMetric({
      ...pagination,
      ...params,
    });
    setLoading(false);
    const { list, pageSize, current, total } = data || {};
    let resData: any = {};
    if (code === 200) {
      setPagination({
        pageSize: Math.min(pageSize, 100),
        current,
        total,
      });
      setDataSource(list);
      resData = {
        data: list || [],
        success: true,
      };
    } else {
      message.error(msg);
      setDataSource([]);
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
    },
    {
      dataIndex: 'name',
      title: '指标名称',
    },
    {
      dataIndex: 'alias',
      title: '别名',
      search: false,
    },
    {
      dataIndex: 'bizName',
      title: '字段名称',
    },
    {
      dataIndex: 'domainName',
      title: '主题域',
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      valueEnum: SENSITIVE_LEVEL_ENUM,
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
      dataIndex: 'type',
      title: '指标类型',
      valueEnum: {
        ATOMIC: '原子指标',
        DERIVED: '衍生指标',
      },
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

  const handleFilterChange = async (filterParams: {
    name: string;
    sensitiveLevel: string;
    type: string;
  }) => {
    const { sensitiveLevel, type } = filterParams;
    const params: QueryMetricListParams = { ...filterParams };
    const sensitiveLevelValue = sensitiveLevel?.[0];
    const typeValue = type?.[0];

    params.sensitiveLevel = sensitiveLevelValue;
    params.type = typeValue;
    setFilterParams(params);
    await queryMetricList(params);
  };

  return (
    <>
      <div className={styles.metricFilterWrapper}>
        <MetricFilter
          onFiltersChange={(_, values) => {
            handleFilterChange(values);
          }}
        />
      </div>
      <ProTable
        className={`${styles.metricTable}`}
        actionRef={actionRef}
        rowKey="id"
        search={false}
        dataSource={dataSource}
        columns={columns}
        pagination={pagination}
        tableAlertRender={() => {
          return false;
        }}
        loading={loading}
        onChange={(data: any) => {
          const { current, pageSize, total } = data;
          const pagin = {
            current,
            pageSize,
            total,
          };
          setPagination(pagin);
          queryMetricList({ ...pagin, ...filterParams });
        }}
        size="small"
        options={{ reload: false, density: false, fullScreen: false }}
      />
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassMetricTable);

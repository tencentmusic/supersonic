import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useState } from 'react';
import moment from 'moment';
import { ColumnConfig } from '../data';

type Props = {
  columnConfig?: ColumnConfig[];
  dataSource: any;
  metricFieldName: string;
  dateFieldName?: string;
  loading?: boolean;
};

const MetricTable: React.FC<Props> = ({
  columnConfig,
  dataSource,
  dateFieldName = 'sys_imp_date',
  metricFieldName,
  loading = false,
}) => {
  const [columns, setColumns] = useState<ColumnsType<any>>([]);
  useEffect(() => {
    if (Array.isArray(columnConfig)) {
      const config: ColumnsType<any> = columnConfig.map((item: ColumnConfig) => {
        const { name, nameEn } = item;
        if (nameEn === dateFieldName) {
          return {
            title: '日期',
            dataIndex: nameEn,
            key: nameEn,
            width: 120,
            fixed: 'left',
            defaultSortOrder: 'descend',
            sorter: (a, b) => moment(a[nameEn]).valueOf() - moment(b[nameEn]).valueOf(),
          };
        }
        if (nameEn === metricFieldName) {
          return {
            title: name,
            dataIndex: nameEn,
            key: nameEn,
            sortDirections: ['descend'],
            sorter: (a, b) => a[nameEn] - b[nameEn],
          };
        }
        return {
          title: name,
          key: nameEn,
          dataIndex: nameEn,
        };
      });
      setColumns(config);
    }
  }, [columnConfig]);

  return (
    <div style={{ height: '100%' }}>
      {/* {Array.isArray(columns) && columns.length > 0 && ( */}
      <Table
        columns={columns}
        dataSource={dataSource}
        scroll={{ x: 200, y: 700 }}
        loading={loading}
        onChange={() => {}}
      />
      {/* )} */}
    </div>
  );
};

export default MetricTable;

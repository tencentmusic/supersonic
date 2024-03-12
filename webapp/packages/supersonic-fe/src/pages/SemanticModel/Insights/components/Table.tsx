import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useState } from 'react';
import moment from 'moment';

type Props = {
  columnConfig?: ColumnConfig[];
  dataSource: any;
  loading?: boolean;
};

const TagTable: React.FC<Props> = ({ columnConfig, dataSource, loading = false }) => {
  return (
    <div style={{ height: '100%' }}>
      {/* {Array.isArray(columns) && columns.length > 0 && ( */}
      <Table
        columns={columnConfig}
        dataSource={dataSource}
        scroll={{ x: 200, y: 700 }}
        loading={loading}
        onChange={() => {}}
      />
      {/* )} */}
    </div>
  );
};

export default TagTable;

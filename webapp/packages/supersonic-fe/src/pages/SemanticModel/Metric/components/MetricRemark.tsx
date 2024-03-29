import { message, Select, Tag } from 'antd';
import React, { useState } from 'react';
import CommonEditTable from '../../components/CommonEditTable';
import { ISemantic } from '../../data';
import {
  queryRemarks,
  deleteMetricDataRemark,
  insertOrUpdateMetircDataRemark,
} from '../../service';
import styles from '../style.less';

type Props = {
  metricId: number;
};

const remarkColumns = [
  {
    title: '开始时间',
    dataIndex: 'startTime',
    valueType: 'dateTime',
    search: false,
    width: 180,
  },
  {
    title: '结束时间',
    dataIndex: 'endTime',
    valueType: 'dateTime',
    search: false,
    width: 180,
  },
  {
    title: '产品',
    dataIndex: 'product',
    width: 150,
    tooltip: '',
    renderFormItem: () => {
      return (
        <Select
          mode="tags"
          maxTagCount={1}
          style={{ width: '100%' }}
          placeholder="请选择产品类型"
          options={[
            {
              label: 'TME整体',
              value: 'TME整体',
            },
            {
              label: 'QQ音乐',
              value: 'QQ音乐',
            },
            {
              label: '酷狗音乐',
              value: '酷狗音乐',
            },
            {
              label: '酷我音乐',
              value: '酷我音乐',
            },
          ]}
        />
      );
    },
    render: (_: any, row: any) => {
      return Array.isArray(row?.product) ? (
        row?.product.map((item: any) => <Tag key={item}>{item}</Tag>)
      ) : (
        <Tag key={row.product}>{row.product}</Tag>
      );
    },
  },
  {
    title: '备注',
    dataIndex: 'remark',
    width: 200,
    tooltip: '',
    search: false,
    fieldProps: {
      placeholder: '请填写备注',
    },
    formItemProps: {
      rules: [
        {
          required: true,
          whitespace: true,
          message: '此项是必填项',
        },
      ],
    },
  },
];

const MetricRemark: React.FC<Props> = ({ metricId }) => {
  const [remarkData, setRemarkData] = useState<ISemantic.IMetricRemarkItem[]>([]);
  const [remarkPagination, setRemarkPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const queryRemarkData = async () => {
    const { code, data, msg } = await queryRemarks({
      current: remarkPagination.current,
      size: remarkPagination.pageSize,
      metricId,
    });
    const { records, size, current, total } = data || {};
    let resData: any = {};

    if (code == 200) {
      setRemarkPagination({
        pageSize: size,
        current,
        total,
      });

      const list = records.map((item: any) => {
        return {
          ...item,
          editRowId: (Math.random() * 1000000).toFixed(0),
        };
      });
      setRemarkData(list);
      resData = {
        data: list || [],
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

  const handleMetricDataDelete = async (id: string) => {
    const { code, msg } = await deleteMetricDataRemark(id);
    if (code !== 200) {
      message.error(msg);
    }
  };

  const handleMetricRemarksSubmit = async (data: any) => {
    const { code, msg } = await insertOrUpdateMetircDataRemark({
      ...data,
      product: Array.isArray(data.product) ? data.product.join() : data.product,
      metricId,
    });
    if (code !== 200) {
      message.error(msg);
    }
  };

  return (
    <div className={styles.sectionBox} style={{ padding: '20px' }}>
      <div className={styles.table}>
        <CommonEditTable
          position="top"
          search={false}
          request={queryRemarkData}
          tableDataSource={remarkData}
          columnList={remarkColumns}
          onDataSourceChange={(tableData) => { }}
          pagination={{
            ...remarkPagination,
            onChange: (page: number) => {
              setRemarkPagination({ ...remarkPagination, current: page });
            },
          }}
          onDelete={(data: any) => {
            handleMetricDataDelete(data.id);
          }}
          editable={{
            onSave: async (rowKey: string, data: any) => {
              handleMetricRemarksSubmit(data);
            },
          }}
        />
      </div>
    </div>
  );
};

export default MetricRemark;

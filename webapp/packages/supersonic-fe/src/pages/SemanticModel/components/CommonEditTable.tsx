import type { ActionType } from '@ant-design/pro-components';
import type { Ref, ReactNode } from 'react';
import { Space, message } from 'antd';
import React, { useRef, forwardRef, useImperativeHandle, useState, useEffect } from 'react';

import { EditableProTable } from '@ant-design/pro-components';

type Props = {
  title?: string;
  tableDataSource: any[];
  columnList: any[];
  rowKey?: string;
  hideCtrlBtn?: string[];
  editableProTableProps?: any;
  onDataSourceChange?: (dataSource: any) => void;
  extenderCtrlColumn?: (text, record, _, action) => ReactNode[];
  editableActionRender?: (row, config, defaultDom, actionRef) => ReactNode[];
  onRecordSave?: (record: any) => void;
  ref?: any;
};

export type CommonEditTableRef = {
  getCommonEditTableDataSource: () => void;
  editTableActionRef: ActionType;
};
const CommonEditTable: React.FC<Props> = forwardRef(
  (
    {
      title,
      columnList,
      rowKey,
      tableDataSource,
      editableProTableProps = {},
      hideCtrlBtn,
      onRecordSave,
      onDataSourceChange,
      extenderCtrlColumn,
      editableActionRender,
    }: Props,
    ref: Ref<any>,
  ) => {
    const defaultRowKey = rowKey || 'editRowId';
    const [dataSource, setDataSource] = useState<Record<string, any>[]>(tableDataSource);
    const actionRef = useRef<ActionType>();

    useImperativeHandle(ref, () => ({
      getCommonEditTableDataSource: () => {
        return [...dataSource];
      },
      editTableActionRef: actionRef,
    }));

    useEffect(() => {
      setDataSource(
        tableDataSource.map((item: any) => {
          return {
            ...item,
            editRowId: item[defaultRowKey] || (Math.random() * 1000000).toFixed(0),
          };
        }),
      );
    }, [tableDataSource]);

    const handleDataSourceChange = (data: any) => {
      setTimeout(() => {
        onDataSourceChange?.(data);
      }, 0);
    };

    const columns = [
      ...columnList,
      {
        title: '操作',
        dataIndex: 'x',
        valueType: 'option',
        render: (text, record, _, action) => {
          return (
            <Space>
              {!hideCtrlBtn?.includes('editable') && (
                <a
                  key="editable"
                  onClick={() => {
                    action?.startEditable?.(record.editRowId);
                  }}
                >
                  编辑
                </a>
              )}
              {!hideCtrlBtn?.includes('deleteBtn') && (
                <a
                  key="deleteBtn"
                  onClick={() => {
                    const data = [...dataSource].filter(
                      (item) => item[defaultRowKey] !== record[defaultRowKey],
                    );
                    setDataSource(data);
                    handleDataSourceChange(data);
                  }}
                >
                  删除
                </a>
              )}

              {extenderCtrlColumn?.(text, record, _, action)}
            </Space>
          );
        },
      },
      {
        dataIndex: 'editRowId',
        hideInTable: true,
      },
    ];

    const defaultActionRender = (row, config, defaultDom) => {
      return editableActionRender?.(row, config, defaultDom, actionRef);
    };
    const actionRender = editableActionRender ? defaultActionRender : undefined;

    return (
      <>
        <EditableProTable
          key={title}
          actionRef={actionRef}
          headerTitle={title}
          rowKey={defaultRowKey}
          columns={columns}
          value={dataSource}
          tableAlertRender={() => {
            return false;
          }}
          onChange={(data: any) => {
            let tableData = data;
            if (rowKey) {
              // 如果rowKey存在，将rowId复写为rowKey值
              tableData = data.map((item: any) => {
                return {
                  ...item,
                  editRowId: item[rowKey],
                };
              });
            }
            setDataSource(tableData);
            handleDataSourceChange(data);
          }}
          editable={{
            onSave: (_, row) => {
              const rowKeyValue = row[defaultRowKey];
              const isSame = dataSource.filter((item: any, index: number) => {
                return index !== row.index && item[defaultRowKey] === rowKeyValue;
              });
              if (isSame[0]) {
                message.error('存在重复值');
                return Promise.reject();
              }
              onRecordSave?.(row);
              return true;
            },
            actionRender: actionRender,
          }}
          pagination={false}
          size="small"
          recordCreatorProps={{
            record: () => ({ editRowId: (Math.random() * 1000000).toFixed(0) }),
          }}
          {...editableProTableProps}
        />
      </>
    );
  },
);
export default CommonEditTable;

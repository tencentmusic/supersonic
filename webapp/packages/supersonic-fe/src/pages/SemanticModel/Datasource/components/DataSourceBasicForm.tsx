import React, { useEffect, useState } from 'react';
import { Form, Input, Spin, Select, message } from 'antd';
import type { FormInstance } from 'antd/lib/form';
import { getDbNames, getTables } from '../../service';

const FormItem = Form.Item;
const { TextArea } = Input;

type Props = {
  isEdit?: boolean;
  dataBaseConfig: any;
  form: FormInstance<any>;
  tableLoading?: boolean;
  mode?: 'normal' | 'fast';
};

const DataSourceBasicForm: React.FC<Props> = ({
  isEdit,
  dataBaseConfig,
  tableLoading = false,
  mode = 'normal',
}) => {
  const [dbNameList, setDbNameList] = useState<any[]>([]);
  const [tableNameList, setTableNameList] = useState<any[]>([]);
  const [currentDbName, setCurrentDbName] = useState<string>('');
  const [currentTableName, setCurrentTableName] = useState<string>('');
  const queryDbNameList = async (databaseId: number) => {
    const { code, data, msg } = await getDbNames(databaseId);
    if (code === 200) {
      const list = data?.resultList || [];
      setDbNameList(list);
    } else {
      message.error(msg);
    }
  };
  const queryTableNameList = async (databaseName: string) => {
    const { code, data, msg } = await getTables(dataBaseConfig.id, databaseName);
    if (code === 200) {
      const list = data?.resultList || [];
      setTableNameList(list);
    } else {
      message.error(msg);
    }
  };
  useEffect(() => {
    if (dataBaseConfig?.id) {
      queryDbNameList(dataBaseConfig.id);
    }
  }, [dataBaseConfig]);

  return (
    <Spin spinning={tableLoading}>
      {mode === 'fast' && (
        <>
          <FormItem
            name="dbName"
            label="数据库名"
            rules={[{ required: true, message: '请选择数据库/表' }]}
          >
            <Select
              showSearch
              placeholder="请选择数据库/表"
              disabled={isEdit}
              onChange={(dbName: string) => {
                queryTableNameList(dbName);
                setCurrentDbName(dbName);
              }}
            >
              {dbNameList.map((item) => (
                <Select.Option key={item.name} value={item.name}>
                  {item.name}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
          <FormItem
            name="tableName"
            label="数据表名"
            rules={[{ required: true, message: '请选择数据库/表' }]}
          >
            <Select
              placeholder="请选择数据库/表"
              disabled={isEdit}
              showSearch
              onChange={(tableName: string) => {
                // queryTableNameList(tableName);
                setCurrentTableName(tableName);
              }}
            >
              {tableNameList.map((item) => (
                <Select.Option key={item.name} value={item.name}>
                  {item.name}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
        </>
      )}

      <FormItem
        name="name"
        label="数据源中文名"
        rules={[{ required: true, message: '请输入数据源中文名' }]}
      >
        <Input placeholder="名称不可重复" />
      </FormItem>
      <FormItem
        name="bizName"
        label="数据源英文名"
        rules={[{ required: true, message: '请输入数据源英文名' }]}
      >
        <Input placeholder="名称不可重复" disabled={isEdit} />
      </FormItem>
      <FormItem name="description" label="数据源描述">
        <TextArea placeholder="请输入数据源描述" />
      </FormItem>
    </Spin>
  );
};

export default DataSourceBasicForm;

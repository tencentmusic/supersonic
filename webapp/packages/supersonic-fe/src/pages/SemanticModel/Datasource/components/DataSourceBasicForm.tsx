import React, { useState } from 'react';
import { Form, Input, Spin, Select, message } from 'antd';
import type { FormInstance } from 'antd/lib/form';
import { getDbNames, getTables } from '../../service';
import { ISemantic } from '../../data';
import { isString } from 'lodash';

const FormItem = Form.Item;
const { TextArea } = Input;

type Props = {
  isEdit?: boolean;
  databaseConfigList: ISemantic.IDatabaseItemList;
  form: FormInstance<any>;
  mode?: 'normal' | 'fast';
};

const DataSourceBasicForm: React.FC<Props> = ({ isEdit, databaseConfigList, mode = 'normal' }) => {
  const [currentDbLinkConfigId, setCurrentDbLinkConfigId] = useState<number>();
  const [dbNameList, setDbNameList] = useState<any[]>([]);
  const [tableNameList, setTableNameList] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const queryDbNameList = async (databaseId: number) => {
    setLoading(true);
    const { code, data, msg } = await getDbNames(databaseId);
    setLoading(false);
    if (code === 200) {
      const list = data?.resultList || [];
      setDbNameList(list);
    } else {
      message.error(msg);
    }
  };
  const queryTableNameList = async (databaseName: string) => {
    if (!currentDbLinkConfigId) {
      return;
    }
    setLoading(true);
    const { code, data, msg } = await getTables(currentDbLinkConfigId, databaseName);
    setLoading(false);
    if (code === 200) {
      const list = data?.resultList || [];
      setTableNameList(list);
    } else {
      message.error(msg);
    }
  };

  return (
    <Spin spinning={loading}>
      {mode === 'fast' && (
        <>
          <FormItem
            name="databaseId"
            label="数据库连接"
            rules={[{ required: true, message: '请选择数据库连接' }]}
          >
            <Select
              showSearch
              placeholder="请选择数据库连接"
              disabled={isEdit}
              onChange={(dbLinkConfigId: number) => {
                queryDbNameList(dbLinkConfigId);
                setCurrentDbLinkConfigId(dbLinkConfigId);
              }}
            >
              {databaseConfigList.map((item) => (
                <Select.Option key={item.id} value={item.id} disabled={!item.hasUsePermission}>
                  {item.name}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
          <FormItem
            name="dbName"
            label="数据库名"
            rules={[{ required: true, message: '请选择数据库/表' }]}
          >
            <Select
              showSearch
              placeholder="请先选择一个数据库连接"
              disabled={isEdit}
              onChange={(dbName: string) => {
                queryTableNameList(dbName);
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
            <Select placeholder="请选择数据库/表" disabled={isEdit} showSearch>
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
        label="模型中文名"
        rules={[{ required: true, message: '请输入模型中文名' }]}
      >
        <Input placeholder="名称不可重复" />
      </FormItem>
      <FormItem
        name="bizName"
        label="模型英文名"
        rules={[{ required: true, message: '请输入模型英文名' }]}
      >
        <Input placeholder="名称不可重复" disabled={isEdit} />
      </FormItem>
      <FormItem
        name="alias"
        label="别名"
        getValueFromEvent={(value) => {
          return value.join(',');
        }}
        getValueProps={(value) => {
          return {
            value: isString(value) ? value.split(',') : [],
          };
        }}
      >
        <Select
          mode="tags"
          placeholder="输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
          tokenSeparators={[',']}
          maxTagCount={9}
        />
      </FormItem>
      <FormItem name="description" label="模型描述">
        <TextArea placeholder="请输入模型描述" />
      </FormItem>
    </Spin>
  );
};

export default DataSourceBasicForm;

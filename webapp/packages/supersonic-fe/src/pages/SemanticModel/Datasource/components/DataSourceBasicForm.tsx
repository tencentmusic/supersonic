import React from 'react';
import { Form, Input, Spin } from 'antd';
import type { FormInstance } from 'antd/lib/form';

const FormItem = Form.Item;
const { TextArea } = Input;

type Props = {
  isEdit?: boolean;
  form: FormInstance<any>;
  tableLoading?: boolean;
};

const DataSourceBasicForm: React.FC<Props> = ({ isEdit, tableLoading = false }) => {
  return (
    <Spin spinning={tableLoading}>
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

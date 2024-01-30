import { useEffect, useImperativeHandle, forwardRef } from 'react';
import { Form, Input } from 'antd';
import type { ForwardRefRenderFunction } from 'react';
import SelectPartner from '@/components/SelectPartner';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { formLayout } from '@/components/FormHelper/utils';
import styles from '../style.less';
type Props = {
  permissonData: any;
  onSubmit?: (data?: any) => void;
  onValuesChange?: (value: any, values: any) => void;
};

const FormItem = Form.Item;

const PermissionCreateForm: ForwardRefRenderFunction<any, Props> = (
  { permissonData, onValuesChange },
  ref,
) => {
  const [form] = Form.useForm();

  useImperativeHandle(ref, () => ({
    formRef: form,
  }));

  useEffect(() => {
    const fieldsValue = {
      ...permissonData,
    };
    fieldsValue.authorizedDepartmentIds = permissonData.authorizedDepartmentIds || [];
    fieldsValue.authorizedUsers = permissonData.authorizedUsers || [];
    form.setFieldsValue(fieldsValue);
  }, [permissonData]);

  return (
    <>
      <Form
        {...formLayout}
        key={permissonData.groupId}
        form={form}
        layout="vertical"
        onValuesChange={(value, values) => {
          onValuesChange?.(value, values);
        }}
        className={styles.form}
      >
        <FormItem hidden={true} name="groupId" label="ID">
          <Input placeholder="groupId" />
        </FormItem>
        <FormItem name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="请输入名称" />
        </FormItem>
        <FormItem name="authorizedDepartmentIds" label="按组织">
          <SelectPartner
            type="selectedDepartment"
            treeSelectProps={{
              placeholder: '请选择需要授权的部门',
            }}
          />
        </FormItem>

        <FormItem name="authorizedUsers" label="按个人">
          <SelectTMEPerson placeholder="请选择需要授权的个人" />
        </FormItem>
      </Form>
    </>
  );
};

export default forwardRef(PermissionCreateForm);

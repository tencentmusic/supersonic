import { useEffect, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { Form, Input } from 'antd';

import { formLayout } from '@/components/FormHelper/utils';
import { isString } from 'lodash';
import styles from '../style.less';
import CommonEditList from '../../components/CommonEditList/index';
import SqlEditor from '@/components/SqlEditor';
type Props = {
  initialValues: any;
  onSubmit?: () => void;
};

const FormItem = Form.Item;

const EntityCreateForm: ForwardRefRenderFunction<any, Props> = ({ initialValues }, ref) => {
  const [form] = Form.useForm();

  const exchangeFields = ['blackList', 'whiteList'];

  const getFormValidateFields = async () => {
    const fields = await form.validateFields();
    const fieldValue = Object.keys(fields).reduce((formField, key: string) => {
      const targetValue = fields[key];
      if (isString(targetValue) && exchangeFields.includes(key)) {
        formField[key] = targetValue.split(',');
      } else {
        formField[key] = targetValue;
      }
      return formField;
    }, {});
    return {
      ...fieldValue,
    };
  };

  useEffect(() => {
    form.resetFields();
    if (!initialValues) {
      return;
    }
    const fieldValue = Object.keys(initialValues).reduce((formField, key: string) => {
      const targetValue = initialValues[key];
      if (Array.isArray(targetValue) && exchangeFields.includes(key)) {
        formField[key] = targetValue.join(',');
      } else {
        formField[key] = targetValue;
      }
      return formField;
    }, {});
    form.setFieldsValue({
      ...fieldValue,
    });
  }, [initialValues]);

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
  }));

  return (
    <>
      <Form {...formLayout} form={form} layout="vertical" className={styles.form}>
        <FormItem name="blackList" label="黑名单">
          <Input placeholder="多个维度值用英文逗号隔开" />
        </FormItem>

        <FormItem name="whiteList" label="白名单">
          <Input placeholder="多个维度值用英文逗号隔开" />
        </FormItem>

        <FormItem name="ruleList">
          {/* <SqlEditor height={'150px'} /> */}
          <CommonEditList title="过滤规则" />
        </FormItem>
      </Form>
    </>
  );
};

export default forwardRef(EntityCreateForm);

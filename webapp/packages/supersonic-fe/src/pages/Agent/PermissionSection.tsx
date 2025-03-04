import React, { useEffect, useState } from 'react';
import { Form, Switch } from 'antd';
import SelectPartner from '@/components/SelectPartner';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import styles from './style.less';
import { AgentType } from './type';

type Props = {
  currentAgent?: AgentType;
  onSaveAgent: (agent: AgentType, noTip?: boolean) => Promise<void>;
};

const FormItem = Form.Item;

const PermissionSection: React.FC<Props> = ({ currentAgent, onSaveAgent }) => {
  const [isOpenState, setIsOpenState] = useState(true);
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({
      ...currentAgent,
    });
    setIsOpenState(currentAgent?.isOpen === 1);
  }, []);

  const saveAuth = async () => {
    const values = await form.validateFields();
    const { admins, adminOrgs, isOpen, viewOrgs = [], viewers = [] } = values;
    const agent = {
      ...(currentAgent || {}),
      admins,
      adminOrgs,
      viewOrgs,
      viewers,
      isOpen: isOpen ? 1 : 0,
    };
    onSaveAgent(agent as any);
  };

  return (
    <Form
      form={form}
      layout="vertical"
      onValuesChange={(value) => {
        const { isOpen } = value;
        if (isOpen !== undefined) {
          setIsOpenState(isOpen);
        }
        saveAuth();
      }}
      className={styles.permissionSection}
    >
      <FormItem
        name="admins"
        label={
          <FormItemTitle title={'管理员'} subTitle={'管理员将拥有主题域下所有编辑及访问权限'} />
        }
      >
        <SelectTMEPerson placeholder="请邀请团队成员" />
      </FormItem>
      <FormItem name="adminOrgs" label="按组织">
        <SelectPartner
          type="selectedDepartment"
          treeSelectProps={{
            placeholder: '请选择需要授权的部门',
          }}
        />
      </FormItem>
      <Form.Item label={<FormItemTitle title={'设为公开'} />} name="isOpen" valuePropName="checked">
        <Switch />
      </Form.Item>
      {!isOpenState && (
        <>
          <FormItem name="viewOrgs" label="按组织">
            <SelectPartner
              type="selectedDepartment"
              treeSelectProps={{
                placeholder: '请选择需要授权的部门',
              }}
            />
          </FormItem>
          <FormItem name="viewers" label="按个人">
            <SelectTMEPerson placeholder="请选择需要授权的个人" />
          </FormItem>
        </>
      )}
    </Form>
  );
};

export default PermissionSection;

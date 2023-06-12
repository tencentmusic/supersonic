import React, { useState, useEffect } from 'react';
import { Form, Input, Switch, message } from 'antd';
import SelectPartenr from '@/components/SelectPartner';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { connect } from 'umi';
import type { Dispatch } from 'umi';
import type { StateType } from '../../model';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { updateDomain, getDomainDetail } from '../../service';

import styles from '../style.less';
type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
  onSubmit?: (data?: any) => void;
  onValuesChange?: (value, values) => void;
};

const FormItem = Form.Item;

const PermissionAdminForm: React.FC<Props> = ({ domainManger, onValuesChange }) => {
  const [form] = Form.useForm();
  const [isOpenState, setIsOpenState] = useState<boolean>(true);
  const [classDetail, setClassDetail] = useState<any>({});
  const { selectDomainId } = domainManger;
  const { APP_TARGET } = process.env;

  const queryClassDetail = async (domainId: number) => {
    const { code, msg, data } = await getDomainDetail({ domainId });
    if (code === 200) {
      setClassDetail(data);
      const fieldsValue = {
        ...data,
      };
      fieldsValue.admins = fieldsValue.admins || [];
      fieldsValue.viewers = fieldsValue.viewers || [];
      fieldsValue.viewOrgs = fieldsValue.viewOrgs || [];
      fieldsValue.isOpen = !!fieldsValue.isOpen;
      setIsOpenState(fieldsValue.isOpen);
      form.setFieldsValue(fieldsValue);
      return;
    }
    message.error(msg);
  };

  useEffect(() => {
    queryClassDetail(selectDomainId);
  }, [selectDomainId]);

  const saveAuth = async () => {
    const values = await form.validateFields();
    const { admins, isOpen, viewOrgs = [], viewers = [] } = values;
    const queryClassData = {
      ...classDetail,
      admins,
      viewOrgs,
      viewers,
      isOpen: isOpen ? 1 : 0,
    };
    const { code, msg } = await updateDomain(queryClassData);
    if (code === 200) {
      // message.success('保存成功');
      return;
    }
    message.error(msg);
  };

  return (
    <>
      <Form
        form={form}
        layout="vertical"
        onValuesChange={(value, values) => {
          const { isOpen } = value;
          if (isOpen !== undefined) {
            setIsOpenState(isOpen);
          }
          saveAuth();
          onValuesChange?.(value, values);
        }}
        className={styles.form}
      >
        <FormItem hidden={true} name="groupId" label="ID">
          <Input placeholder="groupId" />
        </FormItem>
        <FormItem
          name="admins"
          label={
            <FormItemTitle title={'管理员'} subTitle={'管理员将拥有主题域下所有编辑及访问权限'} />
          }
        >
          <SelectTMEPerson placeholder="请邀请团队成员" />
        </FormItem>

        <Form.Item
          label={
            <FormItemTitle
              title={'设为公开'}
              subTitle={
                '公开后,所有用户将可使用主题域下低/中敏感度资源，高敏感度资源需通过资源列表进行授权'
              }
            />
          }
          name="isOpen"
          valuePropName="checked"
        >
          <Switch />
        </Form.Item>
        {!isOpenState && (
          <>
            {APP_TARGET === 'inner' && (
              <FormItem name="viewOrgs" label="按组织">
                <SelectPartenr
                  type="selectedDepartment"
                  treeSelectProps={{
                    placeholder: '请选择需要授权的部门',
                  }}
                />
              </FormItem>
            )}
            <FormItem name="viewers" label="按个人">
              <SelectTMEPerson placeholder="请选择需要授权的个人" />
            </FormItem>
          </>
        )}
        {/* <FormItem>
          <Button
            type="primary"
            onClick={() => {
              saveAuth();
            }}
          >
            保 存
          </Button>
        </FormItem> */}
      </Form>
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(PermissionAdminForm);

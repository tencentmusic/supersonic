import React, { forwardRef, useImperativeHandle } from 'react';
import { Form, Input, message, Modal } from 'antd';
import { useBoolean } from 'ahooks';
import { changePassword } from '@/services/user';
import { pick } from 'lodash';
import { encryptPassword } from '@/utils/utils';
import CryptoJS from 'crypto-js';

export interface IRef {
  open: () => void;
  close: () => void;
}

const ChangePasswordModal = forwardRef<IRef>((_, ref) => {
  const [form] = Form.useForm();
  const [open, { setTrue: openModal, setFalse: closeModal }] = useBoolean(false);
  const [confirmLoading, { set: setConfirmLoading }] = useBoolean(false);
  const encryptKey = CryptoJS.enc.Utf8.parse('supersonic@2024');

  useImperativeHandle(ref, () => ({
    open: () => {
      openModal();
      form.resetFields();
    },
    close: () => {
      closeModal();
      form.resetFields();
    },
  }));

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setConfirmLoading(true);
      // Call API to change password
      const res = await changePassword({
        oldPassword: encryptPassword(values.oldPassword, encryptKey),
        newPassword: encryptPassword(values.newPassword, encryptKey),
      });

      if (res && res.code !== 200) {
        return message.warning(res.msg);
      }
      closeModal();
    } catch (error) {
      console.log('Failed:', error);
    } finally {
      setConfirmLoading(false);
    }
  };

  return (
    <Modal
      title="修改密码"
      open={open}
      onOk={handleOk}
      onClose={closeModal}
      onCancel={closeModal}
      confirmLoading={confirmLoading}
    >
      <Form form={form}>
        <Form.Item
          name="oldPassword"
          label="原密码"
          rules={[
            {
              required: true,
              message: '请输入原密码!',
            },
          ]}
          hasFeedback
        >
          <Input.Password />
        </Form.Item>

        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[
            {
              required: true,
              message: '请输入新密码!',
            },
            {
              min: 6,
              max: 10,
              message: '密码须在6-10字符之间!',
            },
          ]}
          hasFeedback
        >
          <Input.Password />
        </Form.Item>

        <Form.Item
          name="confirm"
          label="确认密码"
          dependencies={['newPassword']}
          hasFeedback
          rules={[
            {
              required: true,
              message: '请确认密码!',
            },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('两次输入的密码不一致!'));
              },
            }),
          ]}
        >
          <Input.Password />
        </Form.Item>
      </Form>
    </Modal>
  );
});

export default ChangePasswordModal;

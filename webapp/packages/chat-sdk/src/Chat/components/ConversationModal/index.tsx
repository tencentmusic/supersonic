import { Form, Input, Modal } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { updateConversationName } from '../../service';
import type { ConversationDetailType } from '../../type';

const FormItem = Form.Item;

type Props = {
  visible: boolean;
  editConversation?: ConversationDetailType;
  onClose: () => void;
  onFinish: (conversationName: string) => void;
};

const layout = {
  labelCol: { span: 6 },
  wrapperCol: { span: 18 },
};

const ConversationModal: React.FC<Props> = ({ visible, editConversation, onClose, onFinish }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const conversationNameInputRef = useRef<any>();

  useEffect(() => {
    if (visible) {
      form.setFieldsValue({ conversationName: editConversation!.chatName });
      setTimeout(() => {
        conversationNameInputRef.current.focus({
          cursor: 'all',
        });
      }, 0);
    }
  }, [visible]);

  const onConfirm = async () => {
    const values = await form.validateFields();
    setLoading(true);
    await updateConversationName(values.conversationName, editConversation!.chatId);
    setLoading(false);
    onFinish(values.conversationName);
  };

  return (
    <Modal
      title="修改问答名称"
      open={visible}
      onCancel={onClose}
      onOk={onConfirm}
      confirmLoading={loading}
    >
      <Form {...layout} form={form}>
        <FormItem name="conversationName" label="名称" rules={[{ required: true }]}>
          <Input
            placeholder="请输入问答名称"
            ref={conversationNameInputRef}
            onPressEnter={onConfirm}
          />
        </FormItem>
      </Form>
    </Modal>
  );
};

export default ConversationModal;

import { Form, Button, Modal, Select, message } from 'antd';
import { getMetricClassifications, batchUpdateClassifications } from '../../service';
import React, { useRef, useState, useEffect } from 'react';

export type Props = {
  onCancel: () => void;
  onSuccess?: () => void;
  createModalVisible: boolean;
  ids: number[];
};

const MetricAddClass: React.FC<Props> = ({ ids = [], createModalVisible, onCancel, onSuccess }) => {
  const [formVals, setFormVals] = useState({
    type: 'ADD', // 类型
  });
  const [saveLoading, setSaveLoading] = useState(false);
  const [form] = Form.useForm();

  const [classList, setClassList] = useState<string[]>([]);

  const getClassList = async () => {
    const { code, data, msg } = await getMetricClassifications();
    if (code === 200 && Array.isArray(data)) {
      setClassList(data);
      return;
    }
    message.error(msg);
  };

  useEffect(() => {
    getClassList();
  }, []);

  const handleSubmit = async (params = {}) => {
    const fieldsValue = await form.validateFields();
    setFormVals({ ...formVals, ...fieldsValue });
    setSaveLoading(true);
    const formValus = {
      ...formVals,
      ...fieldsValue,
      ...params,
      ids,
    };
    const { code, msg } = await batchUpdateClassifications(formValus);
    setSaveLoading(false);
    if (code === 200) {
      message.success('添加成功!');
      onSuccess?.();
      return;
    }
    message.error(msg);
  };

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取 消</Button>
        <Button
          type="primary"
          danger
          loading={saveLoading}
          onClick={() => {
            handleSubmit({
              type: 'DELETE',
            });
          }}
        >
          删 除
        </Button>
        <Button
          type="primary"
          loading={saveLoading}
          onClick={() => {
            handleSubmit();
          }}
        >
          添 加
        </Button>
      </>
    );
  };

  return (
    <Modal
      width={600}
      destroyOnClose
      title="批量分类"
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Form
        // {...formLayout}
        form={form}
        initialValues={{
          ...formVals,
        }}
      >
        <Form.Item name="classifications" label="分类">
          <Select
            mode="tags"
            placeholder="输入分类名后回车确认，复制粘贴支持英文逗号自动分隔"
            tokenSeparators={[',']}
            maxTagCount={9}
            options={classList.map((className) => {
              return { label: className, value: className };
            })}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default MetricAddClass;

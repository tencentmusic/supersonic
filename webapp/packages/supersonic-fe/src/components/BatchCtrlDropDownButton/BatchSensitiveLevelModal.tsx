import React, { useState, useRef } from 'react';
import { Button, Modal, Space, Form, Select } from 'antd';
import { ISemantic } from '@/pages/SemanticModel/data';
import { SENSITIVE_LEVEL_OPTIONS } from '@/pages/SemanticModel/constant';
import { SemanticNodeType } from '@/pages/SemanticModel/enum';

import { formLayout } from '@/components/FormHelper/utils';
import {
  batchUpdateDimensionSensitiveLevel,
  batchUpdateMetricSensitiveLevel,
} from '@/pages/SemanticModel/service';

export type Props = {
  onCancel?: () => void;
  ids: number[];
  type: SemanticNodeType;
  open: boolean;
  onSubmit?: (values?: any) => void;
};

const FormItem = Form.Item;

const BatchSensitiveLevelModal: React.FC<Props> = ({
  ids,
  onCancel,
  type = SemanticNodeType.DIMENSION,
  open,
  onSubmit,
}) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [form] = Form.useForm();

  const batchSettingSensitiveLevel = async () => {
    const values = await form.validateFields();
    setLoading(true);
    const { data, code } = await (type === SemanticNodeType.DIMENSION
      ? batchUpdateDimensionSensitiveLevel
      : batchUpdateMetricSensitiveLevel)({
      ids,
      sensitiveLevel: values.sensitiveLevel,
    });
    setLoading(false);
    if (code === 200) {
      onSubmit?.();
    }
  };

  const renderFooter = () => {
    return (
      <>
        <Space>
          <Button
            onClick={() => {
              onCancel?.();
            }}
          >
            取 消
          </Button>

          <Button
            type="primary"
            loading={loading}
            onClick={() => {
              batchSettingSensitiveLevel();
            }}
          >
            保 存
          </Button>
        </Space>
      </>
    );
  };

  return (
    <Modal
      width={500}
      destroyOnClose
      title="批量敏感度设置"
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Form form={form} layout="horizontal" onValuesChange={(value) => {}}>
        <FormItem
          name="sensitiveLevel"
          label="敏感度"
          rules={[{ required: true, message: '请选择敏感度' }]}
        >
          <Select
            style={{ width: 140 }}
            options={SENSITIVE_LEVEL_OPTIONS}
            placeholder="请选择敏感度"
            // allowClear
            // onChange={(value) => {
            //   setFilterParams((preState) => {
            //     return {
            //       ...preState,
            //       sensitiveLevel: value,
            //     };
            //   });
            // }}
          />
        </FormItem>
      </Form>
    </Modal>
  );
};

export default BatchSensitiveLevelModal;

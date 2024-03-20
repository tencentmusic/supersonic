import React, { useState } from 'react'
import { Modal, Form, Input, Select } from 'antd'
import type { EntityCanvasModel } from '../interface'

const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 6 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 16 },
  },
}

interface Props {
  visible: boolean
  onOk: (value: any) => void
  onCancel: () => void
  sourceEntity?: EntityCanvasModel
  targetEntity?: EntityCanvasModel
}

const CreateRelationModal = (props: Props) => {
  const { visible, sourceEntity, targetEntity, onOk, onCancel } = props
  const [confirmLoading, setConfirmLoading] = useState<boolean>(false)
  const [form] = Form.useForm()

  const handleOK = () => {
    form.validateFields().then(values => {
      setConfirmLoading(true)
      const cb = () => {
        setConfirmLoading(false)
      }
      onOk({ ...values, cb })
    })
  }

  return (
    <Modal
      title="关联模型"
      visible={visible}
      confirmLoading={confirmLoading}
      wrapClassName="create-relation-container"
      okText="确定"
      cancelText="取消"
      onOk={handleOK}
      onCancel={onCancel}
      mask={false}
      centered
      destroyOnClose={true}
    >
      <Form form={form}>
        <Form.Item
          {...formItemLayout}
          name="SOURCE_GUID"
          label="SOURCE_GUID"
          rules={[{ required: true }]}
          initialValue={`${sourceEntity?.entityName || ''}(${sourceEntity?.entityId || ''})`}
        >
          <Input />
        </Form.Item>
        <Form.Item
          {...formItemLayout}
          name="TARGET_GUID"
          label="TARGET_GUID"
          rules={[{ required: true }]}
          initialValue={`${targetEntity?.entityName || ''}(${targetEntity?.entityId || ''})`}
        >
          <Input />
        </Form.Item>
        <Form.Item
          {...formItemLayout}
          name="RELATION_TYPE"
          label="选择关联关系"
          rules={[{ required: true }]}
          initialValue={'N:1'}
        >
          <Select placeholder="请选择关联关系">
            <Select.Option value="N:1">多对一</Select.Option>
            <Select.Option value="1:N">一对多</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default CreateRelationModal

import { useEffect, useState } from 'react';
import type { FC } from 'react';
import { Modal, Form, Input, Select, Checkbox } from 'antd';
import { isFunction } from 'lodash';
import { objToArray } from '@/utils/utils';
import type { ParamsItemProps, OprType } from '../data';
import { IDataSource } from '../../data';
import TextArea from 'antd/lib/input/TextArea';
import ParamsSqlEditor from './SqlParamsSqlEditor';

// const EnumSqlParamsType = {
//   auth: '权限变量',
//   query: '查询变量',
// };

const EnumSqlValueType = {
  STRING: '字符串',
  NUMBER: '数字',
  EXPR: 'SQL表达式',
};

const { Option } = Select;

const ParamsTextArea: FC<ParamsItemProps> = ({ value, onChange }) => {
  return (
    <TextArea
      value={value && value[0]}
      onChange={(e) => {
        if (onChange) {
          onChange([e.target.value]);
        }
      }}
      placeholder="请输入表达式"
      rows={3}
    />
  );
};

type IProps = {
  oprType: OprType;
  modalVisible: boolean;
  onSave: (values: IDataSource.ISqlParamsItem) => Promise<any>;
  onCancel?: (oprType: OprType) => void;
  initValue?: IDataSource.ISqlParamsItem;
  nameList?: string[];
};

const SqlParamsDetailModal: FC<IProps> = ({
  oprType = 'add',
  initValue = {} as IDataSource.ISqlParamsItem,
  modalVisible,
  onSave,
  onCancel,
  nameList,
}) => {
  const [valueType, setValueType] = useState<IDataSource.ISqlParamsValueType>();
  const [oldName, setOldName] = useState<string>();

  const formLayout = {
    labelCol: { span: 7 },
    wrapperCol: { span: 13 },
  };

  const [form] = Form.useForm();

  const submitSave = async () => {
    const fieldsValue = await form.validateFields();
    onSave({ ...fieldsValue, index: initValue.index });
  };

  const handleCancel = async () => {
    if (onCancel && isFunction(onCancel)) {
      onCancel(oprType);
    }
  };

  useEffect(() => {
    form.setFieldsValue({
      ...initValue,
    });
    setValueType(initValue.valueType);
    setOldName(initValue.name);
  }, [initValue]);

  return (
    <Modal
      forceRender
      title={`${oprType === 'add' ? '新建' : '编辑'}sql参数`}
      open={modalVisible}
      onOk={submitSave}
      onCancel={handleCancel}
    >
      <Form
        {...formLayout}
        initialValues={{
          ...initValue,
        }}
        form={form}
      >
        <Form.Item
          name="name"
          label="参数名称"
          rules={[
            { required: true, message: '请输入参数名称' },
            {
              validator(_, value, confirm) {
                if (
                  nameList?.some((item) => {
                    return item === value && value !== oldName;
                  })
                ) {
                  confirm('名称不能重复');
                } else {
                  confirm();
                }
              },
            },
          ]}
        >
          <Input placeholder="请输入参数名称" />
        </Form.Item>
        {/* <Form.Item
          name="type"
          label="参数类型"
          rules={[{ required: true, message: '请选择参数类型' }]}
        >
          <Select placeholder="请选择" disabled={true}>
            {objToArray(EnumSqlParamsType).map((d) => (
              <Option key={d.value} value={d.value}>
                {d.label}
              </Option>
            ))}
          </Select>
        </Form.Item> */}
        <Form.Item
          name="valueType"
          label="值类型"
          rules={[{ required: true, message: '请选择值类型' }]}
        >
          <Select
            placeholder="请选择值类型"
            onChange={(e) => {
              setValueType(e as IDataSource.ISqlParamsValueType);
              if (e === 'EXPR') {
                form.setFieldsValue({
                  defaultValues: undefined,
                });
              }
            }}
          >
            {objToArray(EnumSqlValueType).map((d) => (
              <Option key={d.value} value={d.value}>
                {d.label}
              </Option>
            ))}
          </Select>
        </Form.Item>
        {/* <Form.Item name="alias" label="别名">
          <Input placeholder="请输入参数别名" />
        </Form.Item> */}
        {/* {valueType !== 'sql' && (
          <Form.Item name="udf" label="是否使用表达式">
            <Checkbox
              checked={udf}
              onChange={(e) => {
                setUdf(e.target.checked);
                if (e.target.checked) {
                  form.setFieldsValue({
                    defaultValues: '',
                  });
                }
              }}
            />
          </Form.Item>
        )} */}
        {valueType === 'EXPR' ? (
          <Form.Item name="defaultValues" label="表达式">
            <ParamsTextArea />
          </Form.Item>
        ) : (
          <Form.Item name="defaultValues" label="默认值">
            <ParamsSqlEditor />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default SqlParamsDetailModal;

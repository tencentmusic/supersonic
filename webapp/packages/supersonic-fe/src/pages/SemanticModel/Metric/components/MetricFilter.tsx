import { Form, Select, Input } from 'antd';
import StandardFormRow from '@/components/StandardFormRow';
import TagSelect from '@/components/TagSelect';
import React, { useEffect } from 'react';
import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';

const FormItem = Form.Item;
const { Option } = Select;

type Props = {
  filterValues?: any;
  onFiltersChange: (_: any, values: any) => void;
};

const MetricFilter: React.FC<Props> = ({ filterValues = {}, onFiltersChange }) => {
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({
      ...filterValues,
    });
  }, [form, filterValues]);

  const handleValuesChange = (value: any, values: any) => {
    onFiltersChange(value, values);
  };

  const onSearch = (value) => {
    if (!value) {
      return;
    }
    onFiltersChange(value, form.getFieldsValue());
  };

  const filterList = [
    {
      title: '指标类型',
      key: 'type',
      options: [
        {
          value: 'ATOMIC',
          label: '原子指标',
        },
        { value: 'DERIVED', label: '衍生指标' },
      ],
    },
    {
      title: '敏感度',
      key: 'sensitiveLevel',
      options: SENSITIVE_LEVEL_OPTIONS,
    },
  ];

  return (
    <Form
      layout="inline"
      form={form}
      colon={false}
      onValuesChange={(value, values) => {
        if (value.keywords || value.keywordsType) {
          return;
        }
        handleValuesChange(value, values);
      }}
      initialValues={{
        keywordsType: 'name',
      }}
    >
      <StandardFormRow key="search" block>
        <Input.Group compact>
          <FormItem name={'keywordsType'} noStyle>
            <Select>
              <Option value="name">中文名</Option>
              <Option value="bizName">英文名</Option>
              <Option value="id">ID</Option>
            </Select>
          </FormItem>
          <FormItem name={'keywords'} noStyle>
            <Input.Search
              placeholder="请输入需要查询的指标信息"
              allowClear
              onSearch={onSearch}
              style={{ width: 300 }}
              enterButton
            />
          </FormItem>
        </Input.Group>
      </StandardFormRow>
      {filterList.map((item) => {
        const { title, key, options } = item;
        return (
          <StandardFormRow key={key} title={title} block>
            <FormItem name={key}>
              <TagSelect reverseCheckAll single>
                {options.map((item: any) => (
                  <TagSelect.Option key={item.value} value={item.value}>
                    {item.label}
                  </TagSelect.Option>
                ))}
              </TagSelect>
            </FormItem>
          </StandardFormRow>
        );
      })}
    </Form>
  );
};

export default MetricFilter;

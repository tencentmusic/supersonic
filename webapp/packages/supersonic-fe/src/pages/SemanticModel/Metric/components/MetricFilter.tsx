import { Form, Input } from 'antd';
import StandardFormRow from '@/components/StandardFormRow';
import TagSelect from '@/components/TagSelect';
import React, { useEffect } from 'react';
import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { SearchOutlined } from '@ant-design/icons';
import DomainTreeSelect from '../../components/DomainTreeSelect';
import styles from '../style.less';

const FormItem = Form.Item;

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

  const onSearch = (value: any) => {
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
        if (value.name) {
          return;
        }
        handleValuesChange(value, values);
      }}
    >
      <StandardFormRow key="search" block>
        <div className={styles.searchBox}>
          <FormItem name={'key'} noStyle>
            <div className={styles.searchInput}>
              <Input.Search
                placeholder="请输入需要查询指标的ID、指标名称、字段名称"
                enterButton={<SearchOutlined style={{ marginTop: 5 }} />}
                onSearch={onSearch}
              />
            </div>
          </FormItem>
        </div>
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
      <StandardFormRow key="domainIds" title="所属主题域" block>
        <FormItem name="domainIds">
          <DomainTreeSelect />
        </FormItem>
      </StandardFormRow>
    </Form>
  );
};

export default MetricFilter;

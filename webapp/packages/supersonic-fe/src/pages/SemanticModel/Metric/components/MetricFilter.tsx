import { Form, Input } from 'antd';
import TagSelect from '@/components/TagSelect';
import React, { ReactNode, useEffect } from 'react';
import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { SearchOutlined } from '@ant-design/icons';
import DomainTreeSelect from '../../components/DomainTreeSelect';
import styles from '../style.less';

const FormItem = Form.Item;

type Props = {
  initFilterValues?: any;
  extraNode?: ReactNode;
  onFiltersChange: (_: any, values: any) => void;
};

const MetricFilter: React.FC<Props> = ({ initFilterValues = {}, extraNode, onFiltersChange }) => {
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({
      ...initFilterValues,
    });
  }, [form]);

  const handleValuesChange = (value: any, values: any) => {
    localStorage.setItem('metricMarketShowType', !!values.showType ? '1' : '0');
    onFiltersChange(value, values);
  };

  const onSearch = (value: any) => {
    onFiltersChange(value, form.getFieldsValue());
  };

  const filterList = [
    {
      title: '展示类型',
      key: 'showFilter',
      options: [
        {
          label: '我创建的',
          value: 'onlyShowMe',
        },
        {
          label: '我收藏的',
          value: 'hasCollect',
        },
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
      className={styles.metricFilterForm}
      layout="inline"
      form={form}
      colon={false}
      onValuesChange={(value, values) => {
        if (value.key) {
          return;
        }
        handleValuesChange(value, values);
      }}
    >
      <div className={styles.metricFilterRow}>
        <div className={styles.metricFilterField}>
          <div className={styles.metricFilterFieldLabel}>搜索指标</div>
          <FormItem name="key" className={styles.metricFilterControl}>
            <Input
              className={styles.metricSearchInputInner}
              placeholder="搜索指标 ID、名称、英文名、标签"
              allowClear
              suffix={
                <SearchOutlined
                  className={styles.metricSearchSuffix}
                  onClick={() => {
                    onSearch(form.getFieldValue('key'));
                  }}
                />
              }
              onPressEnter={(event) => {
                onSearch((event.target as HTMLInputElement).value);
              }}
            />
          </FormItem>
        </div>
        <div className={styles.metricFilterField}>
          <div className={styles.metricFilterFieldLabel}>主题域</div>
          <FormItem name="domainIds" className={styles.metricFilterControl}>
            <DomainTreeSelect width="100%" />
          </FormItem>
        </div>
        {filterList.map((item) => {
          const { title, key, options } = item;
          return (
            <div className={styles.metricFilterField} key={key}>
              <div className={styles.metricFilterFieldLabel}>{title}</div>
              <FormItem name={key} className={styles.metricFilterControl}>
                <TagSelect reverseCheckAll single>
                  {options.map((option: any) => (
                    <TagSelect.Option key={option.value} value={option.value}>
                      {option.label}
                    </TagSelect.Option>
                  ))}
                </TagSelect>
              </FormItem>
            </div>
          );
        })}
        {extraNode && <div className={styles.metricFilterAction}>{extraNode}</div>}
      </div>
    </Form>
  );
};

export default MetricFilter;

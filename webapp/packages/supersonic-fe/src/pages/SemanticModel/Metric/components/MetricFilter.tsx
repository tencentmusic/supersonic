import { Form, Input, Space, Row, Col, Switch } from 'antd';
import StandardFormRow from '@/components/StandardFormRow';
import TagSelect from '@/components/TagSelect';
import React, { useEffect } from 'react';
import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { SearchOutlined } from '@ant-design/icons';
import DomainTreeSelect from '../../components/DomainTreeSelect';
import styles from '../style.less';

const FormItem = Form.Item;

type Props = {
  initFilterValues?: any;
  onFiltersChange: (_: any, values: any) => void;
};

const MetricFilter: React.FC<Props> = ({ initFilterValues = {}, onFiltersChange }) => {
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
        if (value.key) {
          return;
        }
        handleValuesChange(value, values);
      }}
    >
      <StandardFormRow key="search" block>
        <div className={styles.searchBox}>
          <Row>
            <Col flex="100px">
              <span
                style={{
                  fontSize: '18px',
                  fontWeight: 'bold',
                  position: 'relative',
                  top: '12px',
                }}
              >
                指标搜索
              </span>
            </Col>
            <Col flex="auto">
              <FormItem name="key" noStyle>
                <div className={styles.searchInput}>
                  <Input.Search
                    placeholder="请输入需要查询指标的ID、指标名称、字段名称、标签"
                    enterButton={<SearchOutlined style={{ marginTop: 5 }} />}
                    onSearch={(value) => {
                      onSearch(value);
                    }}
                  />
                </div>
              </FormItem>
            </Col>
          </Row>
        </div>
      </StandardFormRow>
      <Space size={40}>
        <StandardFormRow key="showType" title="切换为卡片" block>
          <FormItem name="showType" valuePropName="checked">
            <Switch size="small" />
          </FormItem>
        </StandardFormRow>
        <StandardFormRow key="onlyShowMe" title="仅显示我的" block>
          <FormItem name="onlyShowMe" valuePropName="checked">
            <Switch size="small" />
          </FormItem>
        </StandardFormRow>
        <StandardFormRow key="domainIds" title="所属主题域" block>
          <FormItem name="domainIds">
            <DomainTreeSelect />
          </FormItem>
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
      </Space>
    </Form>
  );
};

export default MetricFilter;

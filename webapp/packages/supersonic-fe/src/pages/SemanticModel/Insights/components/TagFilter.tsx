import { Form, Input, Space, Row, Col, Switch, Select } from 'antd';
import StandardFormRow from '@/components/StandardFormRow';
import TagSelect from '@/components/TagSelect';
import React, { useEffect } from 'react';
import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { SearchOutlined } from '@ant-design/icons';
import DomainTreeSelect from '../../components/DomainTreeSelect';
import { ISemantic } from '../../data';
import styles from '../style.less';

const FormItem = Form.Item;

type Props = {
  tagObjectList: ISemantic.ITagObjectItem[];
  initFilterValues?: any;
  onFiltersChange: (_: any, values: any) => void;
};

const TagFilter: React.FC<Props> = ({ tagObjectList, initFilterValues = {}, onFiltersChange }) => {
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({
      ...initFilterValues,
    });
  }, [form]);

  useEffect(() => {
    const target = tagObjectList?.[0];
    if (!target) {
      return;
    }
    form.setFieldValue('tagObjectId', target.id);
  }, [tagObjectList]);

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
                标签搜索
              </span>
            </Col>
            <Col flex="auto">
              <FormItem name="key" noStyle>
                <div className={styles.searchInput}>
                  <Input.Search
                    placeholder="请输入需要查询标签的ID、标签名称、英文名称"
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
        <StandardFormRow key="tagObjectId" title="所属对象" block>
          <FormItem name="tagObjectId">
            <Select
              style={{ minWidth: 150 }}
              placeholder="请选择所属对象"
              options={tagObjectList.map((item: ISemantic.ITagObjectItem) => {
                return {
                  label: item.name,
                  value: item.id,
                };
              })}
            />
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
        {/* <StandardFormRow key="domainIds" title="所属主题域" block>
          <FormItem name="domainIds">
            <DomainTreeSelect />
          </FormItem>
        </StandardFormRow> */}
      </Space>
    </Form>
  );
};

export default TagFilter;

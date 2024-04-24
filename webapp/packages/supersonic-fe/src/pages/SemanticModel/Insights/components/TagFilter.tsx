import { Form, Input, Space, Row, Col, Switch, Select } from 'antd';
import StandardFormRow from '@/components/StandardFormRow';
import TagSelect from '@/components/TagSelect';
import React, { useEffect, useState, useRef } from 'react';
import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { SearchOutlined } from '@ant-design/icons';
import DomainTreeSelect from '../../components/DomainTreeSelect';
import { ISemantic } from '../../data';
import styles from '../style.less';

const FormItem = Form.Item;

type Props = {
  tagObjectList: ISemantic.ITagObjectItem[];
  extraNode?: ReactNode;
  initFilterValues?: any;
  onFilterInit?: (values: any) => void;
  onFiltersChange: (_: any, values: any) => void;
};

const TagFilter: React.FC<Props> = ({
  tagObjectList,
  extraNode,
  initFilterValues = {},
  onFilterInit,
  onFiltersChange,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({
      ...initFilterValues,
    });
  }, [form]);

  const [currentDomainId, setCurrentDomainId] = useState<number>();

  const [tagObjectOptions, setTagObjectOptions] = useState<OptionsItem[]>([]);

  const initState = useRef<boolean>(false);

  useEffect(() => {
    const options = tagObjectList
      .filter((item) => {
        if (currentDomainId) {
          return item.domainId === currentDomainId;
        } else {
          return true;
        }
      })
      .map((item: ISemantic.ITagObjectItem) => {
        return {
          label: item.name,
          value: item.id,
        };
      });
    setTagObjectOptions(options);
    const target = options[0];
    form.setFieldValue('tagObjectId', target?.value);

    if (currentDomainId && target?.value && !initState.current) {
      initState.current = true;
      const data = form.getFieldsValue();
      onFilterInit?.({ ...data, tagObjectId: target?.value });
    }
  }, [currentDomainId, tagObjectList]);

  const handleValuesChange = (value: any, values: any) => {
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
        if (value.domainId) {
          setCurrentDomainId(value.domainId);
          const options = tagObjectList.filter((item) => {
            if (value.domainId) {
              return item.domainId === value.domainId;
            } else {
              return true;
            }
          });
          handleValuesChange(value, {
            ...values,
            tagObjectId: options[0]?.id,
          });
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
      <Row style={{ width: '100%' }}>
        <Col flex="auto">
          <Space size={40}>
            <StandardFormRow key="domainId" title="主题域" block>
              <FormItem name="domainId">
                <DomainTreeSelect
                  firstLevelOnly={true}
                  treeSelectProps={{ multiple: false, allowClear: false }}
                  onDefaultValue={(value) => {
                    setCurrentDomainId(value);
                  }}
                />
              </FormItem>
            </StandardFormRow>
            <StandardFormRow key="tagObjectId" title="标签对象" block>
              <FormItem name="tagObjectId">
                <Select
                  style={{ minWidth: 150 }}
                  placeholder="请选择所属对象"
                  options={tagObjectOptions}
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
          </Space>
        </Col>
        {extraNode && <Col flex="130px">{extraNode}</Col>}
      </Row>
    </Form>
  );
};

export default TagFilter;

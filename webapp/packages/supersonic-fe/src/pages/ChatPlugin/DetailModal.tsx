import React, { useEffect, useState } from 'react';
import { Modal, Select, Form, Input, InputNumber, message, Button, Radio } from 'antd';
import { getDimensionList, getDomainList, savePlugin } from './service';
import {
  DimensionType,
  DomainType,
  ParamTypeEnum,
  ParseModeEnum,
  PluginType,
  FunctionParamFormItemType,
  PluginTypeEnum,
} from './type';
import { getLeafList, uuid } from '@/utils/utils';
import styles from './style.less';
import { PARSE_MODE_MAP, PLUGIN_TYPE_MAP } from './constants';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { isArray, set } from 'lodash';

const FormItem = Form.Item;
const { TextArea } = Input;

type Props = {
  detail?: PluginType;
  onSubmit: (values: any) => void;
  onCancel: () => void;
};

const DetailModal: React.FC<Props> = ({ detail, onSubmit, onCancel }) => {
  const [domainList, setDomainList] = useState<DomainType[]>([]);
  const [domainDimensionList, setDomainDimensionList] = useState<Record<number, DimensionType[]>>(
    {},
  );
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [pluginType, setPluginType] = useState<PluginTypeEnum>();
  const [functionName, setFunctionName] = useState<string>();
  const [functionParams, setFunctionParams] = useState<FunctionParamFormItemType[]>([]);
  const [examples, setExamples] = useState<{ id: string; question?: string }[]>([]);
  const [filters, setFilters] = useState<any[]>([]);
  const [form] = Form.useForm();

  const initDomainList = async () => {
    const res = await getDomainList();
    setDomainList([{ id: -1, name: '全部' }, ...getLeafList(res.data)]);
  };

  useEffect(() => {
    initDomainList();
  }, []);

  const initDomainDimensions = async (params: any) => {
    const domainIds = params
      .filter((param: any) => !!param.domainId)
      .map((param: any) => param.domainId);
    const res = await Promise.all(domainIds.map((domainId: number) => getDimensionList(domainId)));
    setDomainDimensionList(
      domainIds.reduce(
        (result: Record<number, DimensionType[]>, domainId: number, index: number) => {
          result[domainId] = res[index].data.list;
          return result;
        },
        {},
      ),
    );
  };

  useEffect(() => {
    if (detail) {
      const { paramOptions } = detail.config || {};
      const height = paramOptions?.find(
        (option: any) => option.paramType === 'FORWARD' && option.key === 'height',
      )?.value;
      form.setFieldsValue({
        ...detail,
        url: detail.config?.url,
        height,
      });
      if (paramOptions?.length > 0) {
        const params = paramOptions.filter(
          (option: any) => option.paramType !== ParamTypeEnum.FORWARD,
        );
        setFilters(params);
        initDomainDimensions(params);
      }
      setPluginType(detail.type);
      const parseModeObj = JSON.parse(detail.parseModeConfig || '{}');
      setFunctionName(parseModeObj.name);
      const { properties } = parseModeObj.parameters || {};
      setFunctionParams(
        properties
          ? Object.keys(properties).map((key: string, index: number) => {
              return {
                id: `${index}`,
                name: key,
                type: properties[key].type,
                description: properties[key].description,
              };
            })
          : [],
      );
      setExamples(
        parseModeObj.examples
          ? parseModeObj.examples.map((item: string, index: number) => ({
              id: index,
              question: item,
            }))
          : [],
      );
    }
  }, [detail]);

  const layout = {
    labelCol: { span: 4 },
    wrapperCol: { span: 20 },
  };

  const getFunctionParam = (description: string) => {
    return {
      name: functionName,
      description,
      parameters: {
        type: 'object',
        properties: functionParams
          .filter((param) => !!param.name?.trim())
          .reduce((acc, cur) => {
            acc[cur.name || ''] = {
              type: cur.type,
              description: cur.description,
            };
            return acc;
          }, {}),
        required: functionParams.filter((param) => !!param.name?.trim()).map((param) => param.name),
      },
      examples: examples
        .filter((example) => !!example.question?.trim())
        .map((example) => example.question),
    };
  };

  const onOk = async () => {
    const values = await form.validateFields();
    setConfirmLoading(true);
    let paramOptions = isArray(filters)
      ? filters?.filter(
          (filter) =>
            typeof filter === 'object' && (filter.paramType !== null || filter.value != null),
        )
      : [];
    paramOptions = paramOptions.concat([
      {
        paramType: ParamTypeEnum.FORWARD,
        key: 'height',
        value: values.height || undefined,
      },
    ]);
    const config = {
      url: values.url,
      paramOptions,
    };
    await savePlugin({
      ...values,
      id: detail?.id,
      domainList: isArray(values.domainList) ? values.domainList : [values.domainList],
      config: JSON.stringify(config),
      parseModeConfig: JSON.stringify(getFunctionParam(values.pattern)),
    });
    setConfirmLoading(false);
    onSubmit(values);
    message.success(detail?.id ? '编辑成功' : '新建成功');
  };

  const updateDimensionList = async (value: number) => {
    if (domainDimensionList[value]) {
      return;
    }
    const res = await getDimensionList(value);
    setDomainDimensionList({ ...domainDimensionList, [value]: res.data.list });
  };

  return (
    <Modal
      open
      title={detail ? '编辑插件' : '新建插件'}
      width={900}
      confirmLoading={confirmLoading}
      onOk={onOk}
      onCancel={onCancel}
    >
      <Form {...layout} form={form} style={{ maxWidth: 820 }}>
        <FormItem name="domainList" label="主题域">
          <Select
            placeholder="请选择主题域"
            options={domainList.map((domain) => ({
              label: domain.name,
              value: domain.id,
            }))}
            showSearch
            filterOption={(input, option) =>
              ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
            }
            mode="multiple"
            allowClear
          />
        </FormItem>
        <FormItem
          name="name"
          label="插件名称"
          rules={[{ required: true, message: '请输入插件名称' }]}
        >
          <Input placeholder="请输入插件名称" allowClear />
        </FormItem>
        <FormItem
          name="type"
          label="插件类型"
          rules={[{ required: true, message: '请选择插件类型' }]}
        >
          <Select
            placeholder="请选择插件类型"
            options={Object.keys(PLUGIN_TYPE_MAP).map((key) => ({
              label: PLUGIN_TYPE_MAP[key],
              value: key,
            }))}
            onChange={(value) => {
              setPluginType(value);
              if (value === PluginTypeEnum.DSL) {
                form.setFieldsValue({ parseMode: ParseModeEnum.FUNCTION_CALL });
                // setFunctionName('DSL');
                setFunctionParams([
                  {
                    id: uuid(),
                    name: 'query_text',
                    type: 'string',
                    description: '用户的原始自然语言查询',
                  },
                ]);
              }
            }}
          />
        </FormItem>
        <FormItem
          name="pattern"
          label="插件描述"
          rules={[{ required: true, message: '请输入插件描述' }]}
        >
          <TextArea placeholder="请输入插件描述，多个描述换行分隔" allowClear />
        </FormItem>
        <FormItem name="exampleQuestions" label="示例问题">
          <div className={styles.paramsSection}>
            {examples.map((example) => {
              const { id, question } = example;
              return (
                <div className={styles.filterRow} key={id}>
                  <Input
                    placeholder="示例问题"
                    value={question}
                    className={styles.questionExample}
                    onChange={(e) => {
                      example.question = e.target.value;
                      setExamples([...examples]);
                    }}
                    allowClear
                  />
                  <DeleteOutlined
                    onClick={() => {
                      setExamples(examples.filter((item) => item.id !== id));
                    }}
                  />
                </div>
              );
            })}
            <Button
              onClick={() => {
                setExamples([...examples, { id: uuid() }]);
              }}
            >
              <PlusOutlined />
              新增示例问题
            </Button>
          </div>
        </FormItem>
        <FormItem label="函数名称">
          <Input
            value={functionName}
            onChange={(e) => {
              setFunctionName(e.target.value);
            }}
            placeholder="请输入函数名称，只能包含因为字母和下划线"
            allowClear
          />
        </FormItem>
        <FormItem name="params" label="函数参数" hidden={pluginType === PluginTypeEnum.DSL}>
          <div className={styles.paramsSection}>
            {functionParams.map((functionParam: FunctionParamFormItemType) => {
              const { id, name, type, description } = functionParam;
              return (
                <div className={styles.filterRow} key={id}>
                  <Input
                    placeholder="参数名称"
                    value={name}
                    className={styles.filterParamName}
                    onChange={(e) => {
                      functionParam.name = e.target.value;
                      setFunctionParams([...functionParams]);
                    }}
                    allowClear
                  />
                  <Select
                    placeholder="参数类型"
                    options={[
                      { label: '字符串', value: 'string' },
                      { label: '整型', value: 'int' },
                    ]}
                    className={styles.filterParamValueField}
                    allowClear
                    value={type}
                    onChange={(value) => {
                      functionParam.type = value;
                      setFunctionParams([...functionParams]);
                    }}
                  />
                  <Input
                    placeholder="参数描述"
                    value={description}
                    className={styles.filterParamValueField}
                    onChange={(e) => {
                      functionParam.description = e.target.value;
                      setFunctionParams([...functionParams]);
                    }}
                    allowClear
                  />
                  <DeleteOutlined
                    onClick={() => {
                      setFunctionParams(functionParams.filter((item) => item.id !== id));
                    }}
                  />
                </div>
              );
            })}
            <Button
              onClick={() => {
                setFunctionParams([...functionParams, { id: uuid() }]);
              }}
            >
              <PlusOutlined />
              新增函数参数
            </Button>
          </div>
        </FormItem>
        {(pluginType === PluginTypeEnum.WEB_PAGE || pluginType === PluginTypeEnum.WEB_SERVICE) && (
          <>
            <FormItem name="url" label="地址" rules={[{ required: true, message: '请输入地址' }]}>
              <Input placeholder="请输入地址" allowClear />
            </FormItem>
            <FormItem name="params" label="参数">
              <div className={styles.paramsSection}>
                {filters.map((filter: any) => {
                  return (
                    <div className={styles.filterRow} key={filter.id}>
                      <Input
                        placeholder="参数名称"
                        value={filter.key}
                        className={styles.filterParamName}
                        onChange={(e) => {
                          filter.key = e.target.value;
                          setFilters([...filters]);
                        }}
                        allowClear
                      />
                      <Radio.Group
                        onChange={(e) => {
                          filter.paramType = e.target.value;
                          setFilters([...filters]);
                        }}
                        value={filter.paramType}
                      >
                        <Radio value={ParamTypeEnum.SEMANTIC}>维度</Radio>
                        <Radio value={ParamTypeEnum.CUSTOM}>自定义</Radio>
                      </Radio.Group>
                      {filter.paramType === ParamTypeEnum.CUSTOM && (
                        <Input
                          placeholder="请输入"
                          value={filter.value}
                          className={styles.filterParamValueField}
                          onChange={(e) => {
                            filter.value = e.target.value;
                            setFilters([...filters]);
                          }}
                          allowClear
                        />
                      )}
                      {filter.paramType === ParamTypeEnum.SEMANTIC && (
                        <>
                          <Select
                            placeholder="主题域"
                            options={domainList.map((domain) => ({
                              label: domain.name,
                              value: domain.id,
                            }))}
                            showSearch
                            filterOption={(input, option) =>
                              ((option?.label ?? '') as string)
                                .toLowerCase()
                                .includes(input.toLowerCase())
                            }
                            className={styles.filterParamName}
                            allowClear
                            value={filter.domainId}
                            onChange={(value) => {
                              filter.domainId = value;
                              setFilters([...filters]);
                              updateDimensionList(value);
                            }}
                          />
                          <Select
                            placeholder="请选择维度，需先选择主题域"
                            options={(domainDimensionList[filter.domainId] || []).map(
                              (dimension) => ({
                                label: dimension.name,
                                value: `${dimension.id}`,
                              }),
                            )}
                            showSearch
                            className={styles.filterParamValueField}
                            filterOption={(input, option) =>
                              ((option?.label ?? '') as string)
                                .toLowerCase()
                                .includes(input.toLowerCase())
                            }
                            allowClear
                            value={filter.elementId}
                            onChange={(value) => {
                              filter.elementId = value;
                              setFilters([...filters]);
                            }}
                          />
                        </>
                      )}
                      <DeleteOutlined
                        onClick={() => {
                          setFilters(filters.filter((item) => item.id !== filter.id));
                        }}
                      />
                    </div>
                  );
                })}
                <Button
                  onClick={() => {
                    setFilters([...filters, { id: uuid(), key: undefined, value: undefined }]);
                  }}
                >
                  <PlusOutlined />
                  新增参数
                </Button>
              </div>
            </FormItem>
          </>
        )}
        <FormItem name="height" label="高度">
          <InputNumber placeholder="单位px" />
        </FormItem>
      </Form>
    </Modal>
  );
};

export default DetailModal;

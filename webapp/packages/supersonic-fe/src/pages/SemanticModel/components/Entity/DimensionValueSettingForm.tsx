import { useState, useEffect } from 'react';
import { Form, Switch, Space, Button, Tooltip, message, Select } from 'antd';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { useModel } from '@umijs/max';
import { RedoOutlined, InfoCircleOutlined } from '@ant-design/icons';
import DisabledWheelNumberInput from '@/components/DisabledWheelNumberInput';
import { formLayout } from '@/components/FormHelper/utils';
import { ProCard } from '@ant-design/pro-components';
import {
  DictTaskState,
  KnowledgeConfigTypeEnum,
  KnowledgeConfigStatusEnum,
  KnowledgeConfigTypeWordingMap,
} from '../../enum';
import {
  searchKnowledgeConfigQuery,
  searchDictLatestTaskList,
  createDictTask,
  editDictConfig,
  createDictConfig,
  deleteDictTask,
  queryMetric,
  getModelList,
  getMetricData,
} from '../../service';
import type { ISemantic } from '../../data';
import type { StateType } from '../../model';
import styles from '../style.less';
import CommonEditList from '../../components/CommonEditList';

type Props = {
  dataItem: ISemantic.IDimensionItem | ISemantic.ITagItem;
  type?: KnowledgeConfigTypeEnum;
  onSubmit?: () => void;
};

const FormItem = Form.Item;

const DimensionValueSettingForm: React.FC<Props> = ({
  dataItem,
  type = KnowledgeConfigTypeEnum.DIMENSION,
}) => {
  const [form] = Form.useForm();
  const domainModel = useModel('SemanticModel.domainData');
  const { selectDomainId } = domainModel;
  const [dimensionVisible, setDimensionVisible] = useState<boolean>(false);
  const [taskItemState, setTaskItemState] = useState<ISemantic.IDictKnowledgeTaskItem>();
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [refreshLoading, setRefreshLoading] = useState<boolean>(false);
  const [knowledgeConfig, setKnowledgeConfig] = useState<ISemantic.IDictKnowledgeConfigItem>();
  const [modelList, setModelList] = useState<ISemantic.IModelItem[]>([]);
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [importDictState, setImportDictState] = useState<boolean>(false);
  const [metricList, setMetricList] = useState<ISemantic.IMetricItem[]>();
  const defaultKnowledgeConfig: ISemantic.IDictKnowledgeConfigItemConfig = {
    blackList: [],
    whiteList: [],
    ruleList: [],
  };

  useEffect(() => {
    searchKnowledgeConfig();
    queryDictLatestTaskList();
  }, []);

  useEffect(() => {
    if (!selectDomainId) {
      return;
    }
    queryModelList(selectDomainId);
  }, [selectDomainId]);

  const queryModelList = async (domainId: number) => {
    const { code, data } = await getModelList(domainId);
    if (code === 200) {
      setModelList(data);
    } else {
      message.error('获取模型列表失败!');
    }
  };

  const queryMetricList = async (modelId: number) => {
    const { code, data, msg } = await queryMetric({ modelId });
    if (code === 200 && Array.isArray(data?.list)) {
      setMetricList(data.list);
    } else {
      message.error(msg);
    }
  };

  const taskRender = () => {
    if (taskItemState?.taskStatus) {
      return (
        <span style={{ color: '#5493ff', fontWeight: 'bold' }}>
          {DictTaskState[`${taskItemState?.taskStatus || 'unknown'}`]}
        </span>
      );
    }
    return '--';
  };

  const searchKnowledgeConfig = async () => {
    setRefreshLoading(true);
    const { code, data } = await searchKnowledgeConfigQuery({
      type,
      itemId: dataItem.id,
    });

    setRefreshLoading(false);
    if (code !== 200) {
      message.error('获取字典导入配置失败!');
      return;
    }
    const configItem = data[0];
    if (configItem) {
      const { status, config } = configItem;
      if (status === KnowledgeConfigStatusEnum.ONLINE) {
        setDimensionVisible(true);
      } else {
        setDimensionVisible(false);
      }
      form.setFieldsValue({
        ...config,
      });
      setKnowledgeConfig(configItem);
      const { metricId } = config;
      if (metricId) {
        queryMetricData(metricId);
      }
    } else {
      form.setFieldsValue({
        ...defaultKnowledgeConfig,
      });
      createDictConfigQuery(dataItem, defaultKnowledgeConfig);
    }
  };

  const queryDictLatestTaskList = async () => {
    setRefreshLoading(true);
    const { code, data } = await searchDictLatestTaskList({
      type,
      itemId: dataItem.id,
    });
    setRefreshLoading(false);
    if (code !== 200) {
      message.error('获取字典导入任务失败!');
      return;
    }

    if (data?.id) {
      if (data.taskStatus !== 'running') {
        setImportDictState(false);
      }
      setTaskItemState(data);
    }
  };

  const queryMetricData = async (metricId: string) => {
    const { code, data, msg } = await getMetricData(metricId);
    if (code === 200) {
      const { modelId } = data;
      queryMetricList(modelId);
      form.setFieldValue('modelId', modelId);
      return;
    }
    message.error(msg);
  };

  const createDictConfigQuery = async (
    dimension: ISemantic.IDimensionItem,
    config: ISemantic.IDictKnowledgeConfigItemConfig,
  ) => {
    const { code, data } = await createDictConfig({
      type,
      itemId: dimension.id,
      config,
      status: 1,
    });

    if (code !== 200) {
      message.error('字典导入配置创建失败!');
      return;
    }
    setKnowledgeConfig(data);
  };

  const createDictTaskQuery = async (dimension: ISemantic.IDimensionItem) => {
    setImportDictState(true);
    const { code } = await createDictTask({
      type,
      itemId: dimension.id,
    });

    if (code !== 200) {
      message.error('字典导入任务创建失败!');
      return;
    }
    setTimeout(() => {
      queryDictLatestTaskList();
    }, 2000);
  };

  const editDictTaskQuery = async (
    status: KnowledgeConfigStatusEnum = KnowledgeConfigStatusEnum.ONLINE,
  ) => {
    if (!knowledgeConfig?.id) {
      return;
    }
    const config = await form.validateFields();
    setSaveLoading(true);
    const { code } = await editDictConfig({
      ...knowledgeConfig,
      config: {
        ...knowledgeConfig.config,
        ...config,
      },
      status,
    });
    setSaveLoading(false);
    if (code === 200) {
      message.success('字典导入配置保存成功!');
      return;
    }
    message.error('字典导入配置保存失败!');
  };

  const deleteDictTaskQuery = async (dimension: ISemantic.IDimensionItem) => {
    setDeleteLoading(true);
    const { code } = await deleteDictTask({
      type,
      itemId: dimension.id,
    });
    setDeleteLoading(false);
    if (code !== 200) {
      message.error('字典清除失败!');
      return;
    }
  };

  return (
    <>
      <Form
        {...formLayout}
        form={form}
        style={{ margin: '0 30px 30px 30px' }}
        layout="vertical"
        className={styles.form}
        onValuesChange={(value, values) => {
          if (value.modelId === undefined && values.modelId == undefined) {
            setMetricList([]);
            form.setFieldValue('metricId', undefined);
            return;
          }
          if (value.modelId) {
            form.setFieldValue('metricId', undefined);
            queryMetricList(value.modelId);
          }
        }}
      >
        <FormItem
          style={{ marginTop: 15 }}
          label={
            <FormItemTitle
              title={
                <>
                  <Space>
                    <span style={{ fontSize: 16 }}>
                      {KnowledgeConfigTypeWordingMap[type]}值可见
                    </span>
                    <Switch
                      defaultChecked
                      size="small"
                      checked={dimensionVisible}
                      onChange={(value) => {
                        editDictTaskQuery(
                          value
                            ? KnowledgeConfigStatusEnum.ONLINE
                            : KnowledgeConfigStatusEnum.OFFLINE,
                        );
                        setDimensionVisible(value);
                      }}
                    />
                  </Space>
                </>
              }
              subTitle={`设置可见后，${KnowledgeConfigTypeWordingMap[type]}值将在搜索时可以被联想出来`}
            />
          }
        >
          {dimensionVisible && (
            <Space size={20} style={{ marginBottom: 20 }}>
              <Tooltip title={`立即将${KnowledgeConfigTypeWordingMap[type]}值导入字典`}>
                <Button
                  type="link"
                  size="small"
                  style={{ padding: 0 }}
                  disabled={importDictState}
                  onClick={(event) => {
                    createDictTaskQuery(dataItem);
                    setTaskItemState({
                      ...(taskItemState || ({} as ISemantic.IDictKnowledgeTaskItem)),
                      taskStatus: 'running',
                    });

                    event.stopPropagation();
                  }}
                >
                  <Space>
                    立即导入字典 <InfoCircleOutlined />
                  </Space>
                </Button>
              </Tooltip>

              <Tooltip title="刷新字典任务状态">
                <Space>
                  <Button
                    style={{ cursor: 'pointer' }}
                    type="text"
                    loading={refreshLoading}
                    onClick={() => {
                      queryDictLatestTaskList();
                    }}
                  >
                    导入状态
                    <Space>
                      <RedoOutlined />: <span>{taskRender(dataItem)}</span>
                    </Space>
                  </Button>
                </Space>
              </Tooltip>

              <Button
                type="link"
                size="small"
                style={{ padding: 0 }}
                disabled={taskItemState?.taskStatus === 'running'}
                loading={deleteLoading}
                onClick={(event) => {
                  deleteDictTaskQuery(dataItem);
                  setTaskItemState({
                    ...(taskItemState || ({} as ISemantic.IDictKnowledgeTaskItem)),
                    taskStatus: '',
                  });
                  event.stopPropagation();
                }}
              >
                <Tooltip title="清除当前配置的字典">
                  <Space>
                    清除字典 <InfoCircleOutlined />
                  </Space>
                </Tooltip>
              </Button>
            </Space>
          )}
        </FormItem>
        {dimensionVisible && (
          <>
            <ProCard
              title={
                <span
                  style={{
                    color: '#2f374c',
                    fontSize: 16,
                    fontWeight: 400,
                  }}
                >
                  指标设置
                </span>
              }
              bordered
              extra={
                <Button
                  type="primary"
                  onClick={() => {
                    editDictTaskQuery();
                  }}
                  loading={saveLoading}
                >
                  保 存
                </Button>
              }
              style={{ marginBottom: 20 }}
            >
              <FormItem
                label={
                  <FormItemTitle
                    title="参考指标"
                    subTitle="字典中维度值的重要性参照指标值大小，指标值越大，维度值最越重要"
                  />
                }
              >
                <Space.Compact>
                  <FormItem noStyle name="modelId">
                    <Select
                      allowClear
                      showSearch
                      style={{ minWidth: 300 }}
                      optionFilterProp="label"
                      placeholder={`请选择所属model`}
                      options={modelList?.map((item) => {
                        return {
                          value: item.id,
                          label: item.name,
                        };
                      })}
                    />
                  </FormItem>
                  <FormItem name="metricId" noStyle>
                    <Select
                      allowClear
                      showSearch
                      optionFilterProp="label"
                      style={{ minWidth: 300 }}
                      placeholder={`请选择参考指标`}
                      options={metricList?.map((item) => {
                        return {
                          value: item.id,
                          label: item.name,
                        };
                      })}
                    />
                  </FormItem>
                </Space.Compact>
              </FormItem>
              <FormItem
                name="limit"
                label={
                  <FormItemTitle title="最大维度值个数" subTitle="维度写入字典的维度值的最大个数" />
                }
              >
                <DisabledWheelNumberInput placeholder={`请输入维度值`} style={{ minWidth: 200 }} />
              </FormItem>
              <div>
                <div
                  style={{
                    color: '#2f374c',
                    fontSize: 16,
                    display: 'flex',
                    marginBottom: 10,
                  }}
                >
                  <span style={{ flex: 'auto' }}>{KnowledgeConfigTypeWordingMap[type]}值过滤</span>
                </div>

                <FormItem name="blackList" label="黑名单">
                  <Select
                    mode="tags"
                    placeholder={`输入${KnowledgeConfigTypeWordingMap[type]}值后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔`}
                    tokenSeparators={[',']}
                    maxTagCount={9}
                  />
                </FormItem>

                <FormItem name="whiteList" label="白名单">
                  <Select
                    mode="tags"
                    placeholder={`输入${KnowledgeConfigTypeWordingMap[type]}值后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔`}
                    tokenSeparators={[',']}
                    maxTagCount={9}
                  />
                </FormItem>

                <FormItem name="ruleList">
                  <CommonEditList title="过滤规则" />
                </FormItem>
              </div>
            </ProCard>
          </>
        )}
      </Form>
    </>
  );
};

export default DimensionValueSettingForm;

import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { Form, Switch, Space, Button, Tooltip, message, Select } from 'antd';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { RedoOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { formLayout } from '@/components/FormHelper/utils';
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
} from '../../service';
import type { ISemantic } from '../../data';
import { isString } from 'lodash';
import styles from '../style.less';
import CommonEditList from '../../components/CommonEditList';

type Props = {
  dataItem: ISemantic.IDimensionItem | ISemantic.ITagItem;
  type?: KnowledgeConfigTypeEnum;
  onSubmit?: () => void;
};

const FormItem = Form.Item;

const DimensionValueSettingForm: ForwardRefRenderFunction<any, Props> = (
  { dataItem, type = KnowledgeConfigTypeEnum.DIMENSION },
  ref,
) => {
  const [form] = Form.useForm();

  const exchangeFields = ['blackList', 'whiteList'];
  const [dimensionVisible, setDimensionVisible] = useState<boolean>(false);
  const [taskItemState, setTaskItemState] = useState<ISemantic.IDictKnowledgeTaskItem>();
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [refreshLoading, setRefreshLoading] = useState<boolean>(false);
  const [knowledgeConfig, setKnowledgeConfig] = useState<ISemantic.IDictKnowledgeConfigItem>();

  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [importDictState, setImportDictState] = useState<boolean>(false);

  const defaultKnowledgeConfig: ISemantic.IDictKnowledgeConfigItemConfig = {
    blackList: [],
    whiteList: [],
    ruleList: [],
  };

  useEffect(() => {
    searchKnowledgeConfig();
    queryDictLatestTaskList();
  }, []);

  const taskRender = () => {
    if (taskItemState?.taskStatus) {
      return (
        <span style={{ color: '#5493ff', fontWeight: 'bold' }}>
          {DictTaskState[taskItemState?.taskStatus || 'unknown']}
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

  const getFormValidateFields = async () => {
    const fields = await form.validateFields();
    const fieldValue = Object.keys(fields).reduce((formField: any, key: string) => {
      const targetValue = fields[key];
      if (exchangeFields.includes(key)) {
        if (isString(targetValue)) {
          formField[key] = targetValue.split(',');
        } else {
          formField[key] = [];
        }
      } else {
        formField[key] = targetValue;
      }
      return formField;
    }, {});
    return {
      ...fieldValue,
    };
  };

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
  }));

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
    if (code !== 200) {
      message.error('字典导入配置保存失败!');
      return;
    }
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
            {/* <Divider
              style={{
                marginBottom: 35,
              }}
            /> */}
            <div style={{ padding: 20, border: '1px solid #eee', borderRadius: 10 }}>
              <div
                style={{
                  color: '#2f374c',
                  fontSize: 16,
                  display: 'flex',
                  marginBottom: 20,
                }}
              >
                <span style={{ flex: 'auto' }}>{KnowledgeConfigTypeWordingMap[type]}值过滤</span>
                <span style={{ marginLeft: 'auto' }}>
                  <Button
                    type="primary"
                    onClick={() => {
                      editDictTaskQuery();
                    }}
                    loading={saveLoading}
                  >
                    保 存
                  </Button>
                </span>
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
          </>
        )}
      </Form>
    </>
  );
};

export default forwardRef(DimensionValueSettingForm);

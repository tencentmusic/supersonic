import { useState, useEffect } from 'react';
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
  searchDictLatestTaskList,
  createDictTask,
  editDictConfig,
  deleteDictTask,
  createDictConfig,
} from '../../service';
import type { ISemantic } from '../../data';
import styles from '../style.less';

type Props = {
  dataItem: ISemantic.IDimensionItem;
  type?: KnowledgeConfigTypeEnum;
  knowledgeConfig?: ISemantic.IDictKnowledgeConfigItem;
  onSubmit?: () => void;
  onDictChange?: () => void;
  onVisibleChange?: (visible: KnowledgeConfigStatusEnum) => void;
};

const FormItem = Form.Item;

const DimensionValueSettingForm: React.FC<Props> = ({
  dataItem,
  knowledgeConfig,
  type = KnowledgeConfigTypeEnum.DIMENSION,
  onSubmit,
  onDictChange,
  onVisibleChange,
}) => {
  const [form] = Form.useForm();

  const [dimensionVisible, setDimensionVisible] = useState<boolean>(false);
  const [taskItemState, setTaskItemState] = useState<ISemantic.IDictKnowledgeTaskItem>();
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [refreshLoading, setRefreshLoading] = useState<boolean>(false);

  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [importDictState, setImportDictState] = useState<boolean>(false);

  useEffect(() => {
    queryDictLatestTaskList();
  }, []);

  useEffect(() => {
    if (!knowledgeConfig) {
      return;
    }
    const configItem = knowledgeConfig;
    const { status, config } = configItem;
    if (status === KnowledgeConfigStatusEnum.ONLINE) {
      setDimensionVisible(true);
    } else {
      setDimensionVisible(false);
    }
    form.setFieldsValue({
      ...config,
    });
  }, [knowledgeConfig]);

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

  const createDictTaskQuery = async (dimension: ISemantic.IDimensionItem) => {
    setImportDictState(true);
    const { code } = await createDictTask({
      type,
      itemId: dimension.id,
    });
    onDictChange?.();
    if (code !== 200) {
      message.error('字典导入任务创建失败!');
      return;
    }
    setTimeout(() => {
      queryDictLatestTaskList();
    }, 2000);
  };

  const createDictConfigQuery = async () => {
    setSaveLoading(true);
    const { code } = await createDictConfig({
      type: KnowledgeConfigTypeEnum.DIMENSION,
      itemId: dataItem.id,
      status: KnowledgeConfigStatusEnum.ONLINE,
    });
    setSaveLoading(false);
    if (code === 200) {
      message.success('维度值设置保存成功!');
      onSubmit?.();
      return;
    }
    message.error('维度值设置保存失败!');
  };

  const editDictTaskQuery = async (
    status: KnowledgeConfigStatusEnum = KnowledgeConfigStatusEnum.ONLINE,
  ) => {
    if (!knowledgeConfig?.id) {
      createDictConfigQuery();
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
      message.success('维度值设置保存成功!');
      onSubmit?.();
      return;
    }
    message.error('维度值设置保存失败!');
  };

  const deleteDictTaskQuery = async (dimension: ISemantic.IDimensionItem) => {
    setDeleteLoading(true);
    const { code } = await deleteDictTask({
      type,
      itemId: dimension.id,
    });
    onDictChange?.();
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
        style={{ marginTop: '20px' }}
        layout="vertical"
        className={styles.form}
        onValuesChange={(value, values) => {}}
      >
        <FormItem
          style={{ margin: 0 }}
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
                        const state = value
                          ? KnowledgeConfigStatusEnum.ONLINE
                          : KnowledgeConfigStatusEnum.OFFLINE;
                        editDictTaskQuery(state);
                        setDimensionVisible(value);
                        onVisibleChange?.(state);
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
              <Tooltip title={`维度值可见后将定期启动导入任务，如果想立即启动可手动触发`}>
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
                      <RedoOutlined />: <span>{taskRender()}</span>
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
      </Form>
    </>
  );
};

export default DimensionValueSettingForm;

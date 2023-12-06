import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { Form, Input, Switch, Space, Button, Divider, Tooltip, message } from 'antd';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { RedoOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { formLayout } from '@/components/FormHelper/utils';
import { DictTaskState, TransType } from '../../enum';
import {
  getDomainExtendDetailConfig,
  addDomainExtend,
  editDomainExtend,
  searchDictLatestTaskList,
  createDictTask,
} from '../../service';
import type { IChatConfig, ISemantic } from '../../data';
import { isString } from 'lodash';
import styles from '../style.less';
import CommonEditList from '../../components/CommonEditList';

type Props = {
  modelId: number;
  dimensionItem: ISemantic.IDimensionItem;
  onSubmit?: () => void;
};

type TaskStateMap = Record<string, DictTaskState>;

const FormItem = Form.Item;

const DimensionValueSettingForm: ForwardRefRenderFunction<any, Props> = (
  { modelId, dimensionItem },
  ref,
) => {
  const [form] = Form.useForm();

  const exchangeFields = ['blackList', 'whiteList'];
  const [modelRichConfigData, setModelRichConfigData] = useState<IChatConfig.IConfig>();
  const [dimensionVisible, setDimensionVisible] = useState<boolean>(false);
  const [taskStateMap, setTaskStateMap] = useState<TaskStateMap>({});
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [refreshLoading, setRefreshLoading] = useState<boolean>(false);

  const queryThemeListData: any = async () => {
    const { code, data } = await getDomainExtendDetailConfig({
      modelId,
    });

    if (code === 200) {
      setModelRichConfigData(data);
      const targetKnowledgeInfos = data?.chatAggRichConfig?.knowledgeInfos || [];
      const targetConfig = targetKnowledgeInfos.find(
        (item: IChatConfig.IKnowledgeInfosItem) => item.itemId === dimensionItem.id,
      );
      if (targetConfig) {
        const { knowledgeAdvancedConfig, searchEnable } = targetConfig;
        setDimensionVisible(searchEnable);
        const { blackList, whiteList, ruleList } = knowledgeAdvancedConfig;
        form.setFieldsValue({
          blackList: blackList.join(','),
          whiteList: whiteList.join(','),
          ruleList: ruleList || [],
        });
      }
      return;
    }

    message.error('获取问答设置信息失败');
  };

  useEffect(() => {
    queryThemeListData();
    queryDictLatestTaskList();
  }, []);

  const taskRender = (dimension: ISemantic.IDimensionItem) => {
    const { id, type } = dimension;
    const target = taskStateMap[id];
    if (type === TransType.DIMENSION && target) {
      return DictTaskState[target] || '未知状态';
    }
    return '--';
  };

  const queryDictLatestTaskList = async () => {
    setRefreshLoading(true);
    const { code, data } = await searchDictLatestTaskList({
      modelId,
    });
    setRefreshLoading(false);
    if (code !== 200) {
      message.error('获取字典导入任务失败!');
      return;
    }
    const tastMap = data.reduce(
      (stateMap: TaskStateMap, item: { dimId: number; status: DictTaskState }) => {
        const { dimId, status } = item;
        stateMap[dimId] = status;
        return stateMap;
      },
      {},
    );
    setTaskStateMap(tastMap);
  };

  const getFormValidateFields = async () => {
    const fields = await form.validateFields();
    const fieldValue = Object.keys(fields).reduce((formField, key: string) => {
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

  const createDictTaskQuery = async (dimension: ISemantic.IDimensionItem) => {
    const { code } = await createDictTask({
      updateMode: 'REALTIME_ADD',
      modelAndDimPair: {
        [modelId]: [dimension.id],
      },
    });

    if (code !== 200) {
      message.error('字典导入任务创建失败!');
      return;
    }
    setTimeout(() => {
      queryDictLatestTaskList();
    }, 2000);
  };

  const saveEntity = async (searchEnable = dimensionVisible) => {
    setSaveLoading(true);
    const globalKnowledgeConfigFormFields: any = await getFormValidateFields();
    const tempData = { ...modelRichConfigData };
    const targetKnowledgeInfos = modelRichConfigData?.chatAggRichConfig?.knowledgeInfos;
    let knowledgeInfos: IChatConfig.IKnowledgeInfosItem[] = [];
    if (Array.isArray(targetKnowledgeInfos)) {
      knowledgeInfos = targetKnowledgeInfos.reduce(
        (
          knowledgeInfosList: IChatConfig.IKnowledgeInfosItem[],
          item: IChatConfig.IKnowledgeInfosItem,
        ) => {
          if (item.itemId === dimensionItem.id) {
            knowledgeInfosList.push({
              ...item,
              knowledgeAdvancedConfig: {
                ...item.knowledgeAdvancedConfig,
                ...globalKnowledgeConfigFormFields,
              },
              searchEnable,
            });
          } else {
            knowledgeInfosList.push({
              ...item,
            });
          }
          return knowledgeInfosList;
        },
        [],
      );
    }

    const { id, modelId, chatAggRichConfig } = tempData;
    const saveParams = {
      id,
      modelId,
      chatAggConfig: {
        ...chatAggRichConfig,
        knowledgeInfos,
      },
    };
    let saveDomainExtendQuery = addDomainExtend;
    if (id) {
      saveDomainExtendQuery = editDomainExtend;
    }

    const { code, msg } = await saveDomainExtendQuery({
      ...saveParams,
    });
    setSaveLoading(false);
    if (code === 200) {
      return;
    }
    message.error(msg);
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
                    <span style={{ fontSize: 16 }}>维度值可见</span>
                    <Switch
                      defaultChecked
                      size="small"
                      checked={dimensionVisible}
                      onChange={(value) => {
                        saveEntity(value);
                        setDimensionVisible(value);
                      }}
                    />
                  </Space>
                </>
              }
              subTitle={'设置可见后，维度值将在搜索时可以被联想出来'}
            />
          }
        >
          {dimensionVisible && (
            <Space size={20} style={{ marginBottom: 20 }}>
              <Button
                type="link"
                size="small"
                style={{ padding: 0 }}
                onClick={(event) => {
                  createDictTaskQuery(dimensionItem);
                  event.stopPropagation();
                }}
              >
                <Tooltip title="立即将维度值导入字典">
                  <Space>
                    立即导入字典 <InfoCircleOutlined />
                  </Space>
                </Tooltip>
              </Button>

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
                    <RedoOutlined />:
                  </Button>
                </Space>
              </Tooltip>

              <span>{taskRender(dimensionItem)}</span>
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
                <span style={{ flex: 'auto' }}>维度值过滤</span>
                <span style={{ marginLeft: 'auto' }}>
                  <Button
                    type="primary"
                    onClick={() => {
                      saveEntity();
                    }}
                    loading={saveLoading}
                  >
                    保 存
                  </Button>
                </span>
              </div>

              <FormItem name="blackList" label="黑名单">
                <Input placeholder="多个维度值用英文逗号隔开" />
              </FormItem>

              <FormItem name="whiteList" label="白名单">
                <Input placeholder="多个维度值用英文逗号隔开" />
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

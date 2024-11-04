import React, { useEffect, useState } from 'react';
import { Button, Modal, message, Tabs, Input, Form, Divider } from 'antd';
import TableHeaderFilter from '@/components/TableHeaderFilter';
import { ISemantic } from '../data';
import CommonEditTable from './CommonEditTable';
import { KnowledgeConfigTypeEnum, KnowledgeConfigStatusEnum } from '../enum';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import {
  updateDimension,
  updateDimensionAliasValue,
  getDictData,
  searchKnowledgeConfigQuery,
  editDictConfig,
} from '../service';
import { uniqueArray } from '@/utils/utils';

import DimensionValueSettingForm from './Entity/DimensionValueSettingForm';
import DimensionValueFilterModal from './Entity/DimensionValueFilterModal';

export type CreateFormProps = {
  dimensionValueSettingList: ISemantic.IDimensionValueSettingItem[];
  onCancel: () => void;
  dimensionItem: ISemantic.IDimensionItem;
  open: boolean;
  onSubmit: (values?: any) => void;
};

type TableDataSource = { value: string; bizName: string; alias?: string[] };

const DimensionValueSettingModal: React.FC<CreateFormProps> = ({
  onCancel,
  open,
  dimensionItem,
  dimensionValueSettingList,
  onSubmit,
}) => {
  const [tableDataSource, setTableDataSource] = useState<TableDataSource[]>([]);
  const [dimValueMaps, setDimValueMaps] = useState<ISemantic.IDimensionValueSettingItem[]>([]);
  const [form] = Form.useForm();
  const [refreshLoading, setRefreshLoading] = useState<boolean>(false);

  const [knowledgeConfig, setKnowledgeConfig] = useState<ISemantic.IDictKnowledgeConfigItem>();
  const [dimensionVisibleState, setDimensionVisibleState] = useState<KnowledgeConfigStatusEnum>(
    KnowledgeConfigStatusEnum.OFFLINE,
  );
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [llmLoading, setLlmLoading] = useState<boolean>(false);
  const [filterParams, setFilterParams] = useState<Record<string, any>>({});
  const [menuKey, setMenuKey] = useState<string>('default');
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [dimensionValueFilterModalVisible, setDimensionValueFilterModalVisible] =
    useState<boolean>(false);

  const defaultPagination = {
    current: 1,
    pageSize: 10,
    total: 0,
  };
  const [pagination, setPagination] = useState(defaultPagination);
  useEffect(() => {
    searchKnowledgeConfig();
    queryDictData();
    setDimValueMaps(dimensionValueSettingList);
  }, [dimensionValueSettingList]);

  const handleSubmit = async () => {
    await saveDimension({ dimValueMaps });
    onSubmit?.(dimValueMaps);
  };

  const saveDimension = async (fieldsValue: any) => {
    if (!dimensionItem?.id) {
      return;
    }
    const queryParams = {
      ...dimensionItem,
      ...fieldsValue,
    };
    const { code, msg } = await updateDimension(queryParams);
    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const editConfigQuery = async (config: Record<string, any>) => {
    if (!knowledgeConfig?.id) {
      message.success('配置不存在!');
      return;
    }
    setSaveLoading(true);
    const queryConfig = {
      ...knowledgeConfig,
      config: {
        ...knowledgeConfig.config,
        ...config,
      },
      status: dimensionVisibleState,
    };
    const { code } = await editDictConfig(queryConfig);
    setSaveLoading(false);
    if (code === 200) {
      searchKnowledgeConfig();
      message.success('维度值设置保存成功!');
      return;
    }
    message.error('维度值设置保存失败!');
  };

  const searchKnowledgeConfig = async () => {
    setRefreshLoading(true);
    const { code, data } = await searchKnowledgeConfigQuery({
      type: KnowledgeConfigTypeEnum.DIMENSION,
      itemId: dimensionItem.id,
    });

    setRefreshLoading(false);
    if (code !== 200) {
      message.error('获取字典导入配置失败!');
      return;
    }
    const configItem = data[0];
    if (configItem) {
      const { status, config } = configItem;
      setDimensionVisibleState(status);

      form.setFieldsValue({
        ...config,
      });
      setKnowledgeConfig(configItem);
    } else {
    }
  };

  const queryDictData = async (params = {}) => {
    setLlmLoading(true);
    const { code, data } = await getDictData({
      modelId: dimensionItem.modelId,
      itemId: dimensionItem.id,
      ...filterParams,
      ...defaultPagination,
      ...params,
    });

    setLlmLoading(false);
    if (code === 200) {
      const { list, total, pageSize, current } = data;
      setPagination({
        current,
        pageSize,
        total,
      });
      if (Array.isArray(list)) {
        setTableDataSource(list);
      }
    } else {
      message.error('大语言模型解析异常');
    }
  };

  const renderFooter = () => {
    return (
      <>
        {/* <Button onClick={onCancel}>取消</Button> */}
        {menuKey === 'default' && (
          <>
            <Button
              type="primary"
              onClick={() => {
                // handleSubmit();
                onCancel();
              }}
            >
              确 定
            </Button>
          </>
        )}
      </>
    );
  };

  const columns = [
    {
      title: '维度值',
      dataIndex: 'value',
      width: 200,
      editable: false,
      tooltip: '数据库中存储的维度值数据。 比如数据库中维度平台的维度值有kw、qy等',
      formItemProps: {
        fieldProps: {
          placeholder: '请填写维度值',
        },
        rules: [
          {
            required: true,
            whitespace: true,
            message: '此项是必填项',
          },
        ],
      },
    },
    {
      title: '别名',
      dataIndex: 'alias',
      valueType: 'select',
      width: 480,
      tooltip:
        '解析用户查询意图时,支持别名到技术名称的转换。比如用户输入kw、kuwo、酷我,完成设置后,都可以将其转换成技术名称kw',
      fieldProps: {
        placeholder: '输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔',
        mode: 'tags',
        maxTagCount: 5,
        tokenSeparators: [','],
      },
    },
    {
      title: '黑名单',
      dataIndex: 'black',
      width: 80,
      editable: false,
      render: (_, record: TableDataSource) => {
        const list = knowledgeConfig?.config?.blackList;
        if (Array.isArray(list) && list.includes(record.value)) {
          return <span style={{ color: '#1677ff' }}>是</span>;
        }
        return '-';
      },
    },
    {
      title: '白名单',
      dataIndex: 'white',
      width: 80,
      editable: false,
      render: (_, record: TableDataSource) => {
        const list = knowledgeConfig?.config?.whiteList;
        if (Array.isArray(list) && list.includes(record.value)) {
          return <span style={{ color: '#1677ff' }}>是</span>;
        }
        return '-';
      },
    },
  ];

  const rowSelection = {
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(selectedRowKeys);
    },
  };

  const onMenuClick = (key: string, selectedKeys: React.Key[]) => {
    if (selectedKeys.length === 0) {
      message.error('请选择至少一个维度值');
      return;
    }
    switch (key) {
      case 'batchDimensionValueBlackList': {
        const configBlackList = knowledgeConfig?.config?.blackList || [];
        const blackList = uniqueArray([...selectedKeys, ...configBlackList]);
        editConfigQuery({
          blackList,
        });
        break;
      }
      case 'batchDimensionValueWhiteList': {
        const configWhiteList = knowledgeConfig?.config?.whiteList || [];
        const whiteList = uniqueArray([...selectedKeys, ...configWhiteList]);
        editConfigQuery({
          whiteList,
        });
        break;
      }
      case 'batchRemoveDimensionValueBlackList': {
        const configBlackList = knowledgeConfig?.config?.blackList || [];
        const blackList = configBlackList.filter((item) => {
          return !selectedKeys.includes(item);
        });
        editConfigQuery({
          blackList,
        });
        break;
      }
      case 'batchRemoveDimensionValueWhiteList':
        {
          const configWhiteList = knowledgeConfig?.config?.whiteList || [];
          const whiteList = configWhiteList.filter((item) => {
            return !selectedKeys.includes(item);
          });
          editConfigQuery({
            whiteList,
          });
        }
        break;
      case 'batchAddRuleList':
        {
          editConfigQuery({
            ruleList: selectedKeys,
          });
        }
        break;
      default:
        break;
    }
  };

  const refreshTableData = () => {
    queryDictData({
      ...pagination,
      ...filterParams,
    });
  };

  const modifyDimensionValue = async (params) => {
    const { code, data } = await updateDimensionAliasValue(params);
  };
  return (
    <Modal
      width={1200}
      destroyOnClose
      title={`维度值设置[${dimensionItem?.name}]`}
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <div>
        <DimensionValueSettingForm
          dataItem={dimensionItem}
          knowledgeConfig={knowledgeConfig}
          onVisibleChange={(state) => {
            setDimensionVisibleState(state);
          }}
          onSubmit={() => {
            searchKnowledgeConfig();
          }}
          onDictChange={() => {
            refreshTableData();
          }}
        />
        {dimensionVisibleState === KnowledgeConfigStatusEnum.ONLINE && (
          <>
            <Divider style={{ margin: 10 }} />
            <CommonEditTable
              rowKey="value"
              tableDataSource={tableDataSource}
              columnList={columns}
              // onDataSourceChange={(tableData) => {
              //   const dimValueMaps = tableData.map((item: TableDataSource) => {
              //     return {
              //       ...item,
              //     };
              //   });
              //   setDimValueMaps(dimValueMaps);
              // }}
              onRecordSave={(record) => {
                modifyDimensionValue({
                  id: dimensionItem.id,
                  dimValueMaps: {
                    ...record,
                  },
                });
              }}
              hideCtrlBtn={['deleteBtn']}
              editableProTableProps={{
                toolBarRender: () => [
                  <Button
                    key="3"
                    type="primary"
                    onClick={() => {
                      setDimensionValueFilterModalVisible(true);
                    }}
                  >
                    维度值过滤
                  </Button>,
                  <BatchCtrlDropDownButton
                    key="ctrlBtnList"
                    hiddenList={['batchStart', 'batchStop', 'batchDownload', 'batchDelete']}
                    extenderList={[
                      'batchDimensionValueBlackList',
                      'batchDimensionValueWhiteList',
                      'batchRemoveDimensionValueBlackList',
                      'batchRemoveDimensionValueWhiteList',
                    ]}
                    onMenuClick={(key) => {
                      onMenuClick(key, selectedRowKeys);
                    }}
                  />,
                ],

                rowSelection: {
                  type: 'checkbox',
                  ...rowSelection,
                },
                recordCreatorProps: false,
                pagination: pagination,
                headerTitle: (
                  <TableHeaderFilter
                    components={[
                      {
                        label: '维度值搜索',
                        component: (
                          <Input.Search
                            style={{ width: 280 }}
                            placeholder="请输入维度值名称"
                            onSearch={(value) => {
                              setFilterParams((preState) => {
                                return {
                                  ...preState,
                                  keyValue: value,
                                };
                              });
                              queryDictData({ keyValue: value });
                            }}
                          />
                        ),
                      },
                    ]}
                  />
                ),
                onTableChange: (data: any) => {
                  const { current, pageSize, total } = data;
                  setPagination({
                    current,
                    pageSize,
                    total,
                  });
                  queryDictData({ current, pageSize });
                },
              }}
            />
          </>
        )}
      </div>
      {dimensionValueFilterModalVisible && (
        <DimensionValueFilterModal
          config={knowledgeConfig!}
          onCancel={() => {
            setDimensionValueFilterModalVisible(false);
          }}
          onMenuClick={onMenuClick}
        />
      )}
    </Modal>
  );
};

export default DimensionValueSettingModal;

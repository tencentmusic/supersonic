import React, { useEffect, useState } from 'react';
import { Button, Modal, message, Space, Tooltip, Tabs, Input } from 'antd';
import TableHeaderFilter from '@/components/TableHeaderFilter';
import { ISemantic } from '../data';
import CommonEditTable from './CommonEditTable';
import { updateDimension, getDictData } from '../service';
import DimensionValueSettingForm from './Entity/DimensionValueSettingForm';

export type CreateFormProps = {
  dimensionValueSettingList: ISemantic.IDimensionValueSettingItem[];
  onCancel: () => void;
  dimensionItem: ISemantic.IDimensionItem;
  open: boolean;
  onSubmit: (values?: any) => void;
};

type TableDataSource = { techName: string; bizName: string; alias?: string[] };

const DimensionValueSettingModal: React.FC<CreateFormProps> = ({
  onCancel,
  open,
  dimensionItem,
  dimensionValueSettingList,
  onSubmit,
}) => {
  const [tableDataSource, setTableDataSource] = useState<TableDataSource[]>([]);
  const [dimValueMaps, setDimValueMaps] = useState<ISemantic.IDimensionValueSettingItem[]>([]);
  const [llmLoading, setLlmLoading] = useState<boolean>(false);
  const [filterParams, setFilterParams] = useState<Record<string, any>>({});
  const [menuKey, setMenuKey] = useState<string>('default');
  const defaultPagination = {
    current: 1,
    pageSize: 20,
    total: 0,
  };
  const [pagination, setPagination] = useState(defaultPagination);
  useEffect(() => {
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
        <Button onClick={onCancel}>取消</Button>
        {menuKey === 'default' && (
          <>
            {/* <Button
              type="primary"
              loading={llmLoading}
              onClick={() => {
                generatorDimensionValue();
              }}
            >
              <Space>
                智能填充
                <Tooltip title="智能填充将根据维度相关信息，使用大语言模型获取可能被使用的维度值">
                  <InfoCircleOutlined />
                </Tooltip>
              </Space>
            </Button> */}
            <Button
              type="primary"
              onClick={() => {
                handleSubmit();
              }}
            >
              完成
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
    // {
    //   title: '业务名称',
    //   dataIndex: 'bizName',
    //   width: 200,
    //   tooltip:
    //     '查询完成后,最终返回给用户的维度值信息。比如将技术名称kw转换成酷我平台,最终返回给用户是酷我平台',
    //   fieldProps: {
    //     placeholder: '请填写业务名称',
    //   },
    //   formItemProps: {
    //     rules: [
    //       {
    //         required: true,
    //         whitespace: true,
    //         message: '此项是必填项',
    //       },
    //     ],
    //   },
    // },
    {
      title: '别名',
      dataIndex: 'alias',
      valueType: 'select',
      width: 500,
      tooltip:
        '解析用户查询意图时,支持别名到技术名称的转换。比如用户输入kw、kuwo、酷我,完成设置后,都可以将其转换成技术名称kw',
      fieldProps: {
        placeholder: '输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔',
        mode: 'tags',
        maxTagCount: 5,
        tokenSeparators: [','],
      },
    },
  ];

  const tabItem = [
    {
      label: '维度值管理',
      key: 'default',
      children: (
        <CommonEditTable
          tableDataSource={tableDataSource}
          columnList={columns}
          onDataSourceChange={(tableData) => {
            const dimValueMaps = tableData.map((item: TableDataSource) => {
              return {
                ...item,
              };
            });
            setDimValueMaps(dimValueMaps);
          }}
          hideCtrlBtn={['deleteBtn']}
          editableProTableProps={{
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
      ),
    },
    {
      label: '维度值设置',
      key: 'setting',
      children: <DimensionValueSettingForm dataItem={dimensionItem} />,
    },
  ];

  const handleMenuChange = (key: string) => {
    setMenuKey(key);
  };

  return (
    <Modal
      width={1200}
      destroyOnClose
      title="维度值设置"
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Tabs
        items={tabItem}
        size="large"
        activeKey={menuKey}
        onChange={(menuKey: string) => {
          handleMenuChange(menuKey);
        }}
      />
    </Modal>
  );
};

export default DimensionValueSettingModal;

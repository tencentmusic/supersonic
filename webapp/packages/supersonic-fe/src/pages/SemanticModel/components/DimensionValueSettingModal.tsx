import React, { useEffect, useState } from 'react';
import { Button, Modal, message, Space, Tooltip, Tabs } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { ISemantic } from '../data';
import CommonEditTable from './CommonEditTable';
import { updateDimension, mockDimensionValuesAlias } from '../service';
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
  const [menuKey, setMenuKey] = useState<string>('default');

  useEffect(() => {
    setTableDataSource(dimensionValueSettingList);
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

  const generatorDimensionValue = async () => {
    setLlmLoading(true);
    const { code, data } = await mockDimensionValuesAlias({ ...dimensionItem });
    setLlmLoading(false);
    if (code === 200) {
      if (Array.isArray(data)) {
        setDimValueMaps([...dimValueMaps, ...data]);
        setTableDataSource([...tableDataSource, ...data]);
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
            <Button
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
            </Button>
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
      title: '技术名称',
      dataIndex: 'techName',
      width: 200,
      tooltip: '数据库中存储的维度值数据。 比如数据库中维度平台的维度值有kw、qy等',
      formItemProps: {
        fieldProps: {
          placeholder: '请填写技术名称',
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
      title: '业务名称',
      dataIndex: 'bizName',
      width: 200,
      tooltip:
        '查询完成后,最终返回给用户的维度值信息。比如将技术名称kw转换成酷我平台,最终返回给用户是酷我平台',
      fieldProps: {
        placeholder: '请填写业务名称',
      },
      formItemProps: {
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
      label: '维度值填充',
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

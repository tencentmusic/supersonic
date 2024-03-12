import { getLeafNodes } from '@/utils/utils';
import { PlusOutlined } from '@ant-design/icons';
import { Button, Input, message, Popconfirm, Select, Table, Tag } from 'antd';
import moment from 'moment';
import { useEffect, useState } from 'react';
import { PLUGIN_TYPE_MAP } from './constants';
import DetailModal from './DetailModal';
import { deletePlugin, getModelList, getPluginList } from './service';
import styles from './style.less';
import { ModelType, PluginType, PluginTypeEnum } from './type';

const { Search } = Input;

const PluginManage = () => {
  const [name, setName] = useState<string>();
  const [type, setType] = useState<PluginTypeEnum>();
  const [pattern, setPattern] = useState<string>();
  const [model, setModel] = useState<string>();
  const [data, setData] = useState<PluginType[]>([]);
  const [modelList, setModelList] = useState<ModelType[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPluginDetail, setCurrentPluginDetail] = useState<PluginType>();
  const [detailModalVisible, setDetailModalVisible] = useState(false);

  const initModelList = async () => {
    const res = await getModelList();
    setModelList(getLeafNodes(res.data));
  };

  const updateData = async (filters?: any) => {
    setLoading(true);
    const res = await getPluginList({ name, type, pattern, model, ...(filters || {}) });
    setLoading(false);
    setData(res.data?.map((item) => ({ ...item, config: JSON.parse(item.config || '{}') })) || []);
  };

  useEffect(() => {
    initModelList();
    updateData();
  }, []);

  const onCheckPluginDetail = (record: PluginType) => {
    setCurrentPluginDetail(record);
    setDetailModalVisible(true);
  };

  const onDeletePlugin = async (record: PluginType) => {
    await deletePlugin(record.id);
    message.success('插件删除成功');
    updateData();
  };

  const columns: any[] = [
    {
      title: '插件名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '数据集',
      dataIndex: 'dataSetList',
      key: 'dataSetList',
      width: 200,
      render: (value: number[]) => {
        if (value?.includes(-1)) {
          return '默认';
        }
        return value ? (
          <div className={styles.modelColumn}>
            {value.map((id) => {
              const name = modelList.find((model) => model.id === id)?.name;
              return name ? <Tag key={id}>{name}</Tag> : null;
            })}
          </div>
        ) : (
          '-'
        );
      },
    },
    {
      title: '插件类型',
      dataIndex: 'type',
      key: 'type',
      render: (value: string) => {
        return (
          <Tag color={value === PluginTypeEnum.WEB_PAGE ? 'blue' : 'cyan'}>
            {PLUGIN_TYPE_MAP[value]}
          </Tag>
        );
      },
    },
    {
      title: '函数描述',
      dataIndex: 'pattern',
      key: 'pattern',
      width: 450,
    },
    {
      title: '更新人',
      dataIndex: 'updatedBy',
      key: 'updatedBy',
      render: (value: string) => {
        return value || '-';
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (value: string) => {
        return value ? moment(value).format('YYYY-MM-DD HH:mm') : '-';
      },
    },
    {
      title: '操作',
      dataIndex: 'x',
      key: 'x',
      render: (_: any, record: any) => {
        return (
          <div className={styles.operator}>
            <a
              onClick={() => {
                onCheckPluginDetail(record);
              }}
            >
              编辑
            </a>
            <Popconfirm
              title="确定删除吗？"
              onConfirm={() => {
                onDeletePlugin(record);
              }}
            >
              <a>删除</a>
            </Popconfirm>
          </div>
        );
      },
    },
  ];

  const onModelChange = (value: string) => {
    setModel(value);
    updateData({ model: value });
  };

  const onTypeChange = (value: PluginTypeEnum) => {
    setType(value);
    updateData({ type: value });
  };

  const onSearch = () => {
    updateData();
  };

  const onCreatePlugin = () => {
    setCurrentPluginDetail(undefined);
    setDetailModalVisible(true);
  };

  const onSavePlugin = () => {
    setDetailModalVisible(false);
    updateData();
  };

  return (
    <div className={styles.pluginManage}>
      <div className={styles.filterSection}>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>主题域</div>
          <Select
            className={styles.filterItemControl}
            placeholder="请选择主题域"
            options={modelList.map((model) => ({ label: model.name, value: model.id }))}
            value={model}
            allowClear
            onChange={onModelChange}
          />
        </div>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>插件名称</div>
          <Search
            className={styles.filterItemControl}
            placeholder="请输入插件名称"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
            }}
            onSearch={onSearch}
          />
        </div>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>函数描述</div>
          <Search
            className={styles.filterItemControl}
            placeholder="请输入函数描述"
            value={pattern}
            onChange={(e) => {
              setPattern(e.target.value);
            }}
            onSearch={onSearch}
          />
        </div>
        <div className={styles.filterItem}>
          <div className={styles.filterItemTitle}>插件类型</div>
          <Select
            className={styles.filterItemControl}
            placeholder="请选择插件类型"
            options={Object.keys(PLUGIN_TYPE_MAP).map((key) => ({
              label: PLUGIN_TYPE_MAP[key],
              value: key,
            }))}
            value={type}
            allowClear
            onChange={onTypeChange}
          />
        </div>
      </div>
      <div className={styles.pluginList}>
        <div className={styles.titleBar}>
          <div className={styles.title}>插件列表</div>
          <Button type="primary" onClick={onCreatePlugin}>
            <PlusOutlined />
            新建插件
          </Button>
        </div>
        <Table
          columns={columns}
          dataSource={data}
          size="small"
          pagination={{ defaultPageSize: 20 }}
          loading={loading}
        />
      </div>
      {detailModalVisible && (
        <DetailModal
          detail={currentPluginDetail}
          onSubmit={onSavePlugin}
          onCancel={() => {
            setDetailModalVisible(false);
          }}
        />
      )}
    </div>
  );
};

export default PluginManage;

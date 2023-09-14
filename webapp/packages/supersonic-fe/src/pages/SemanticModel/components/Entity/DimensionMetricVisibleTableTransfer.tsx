import { Table, Transfer, Checkbox, Button, Space, message, Tooltip } from 'antd';
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface';
import type { TransferItem } from 'antd/es/transfer';
import type { CheckboxChangeEvent } from 'antd/es/checkbox';
import difference from 'lodash/difference';
import React, { useState, useEffect } from 'react';
import { connect } from 'umi';
import type { StateType } from '../../model';
import type { IChatConfig } from '../../data';
import DimensionValueSettingModal from './DimensionValueSettingModal';
import TransTypeTag from '../TransTypeTag';
import TableTitleTooltips from '../../components/TableTitleTooltips';
import { RedoOutlined } from '@ant-design/icons';
import { SemanticNodeType, DictTaskState, TransType } from '../../enum';
import { createDictTask, searchDictLatestTaskList } from '@/pages/SemanticModel/service';
import styles from '../style.less';
interface RecordType {
  id: number;
  key: string;
  name: string;
  bizName: string;
  type: TransType.DIMENSION | TransType.METRIC;
}

type Props = {
  domainManger: StateType;
  knowledgeInfosMap?: IChatConfig.IKnowledgeInfosItemMap;
  onKnowledgeInfosMapChange?: (knowledgeInfosMap: IChatConfig.IKnowledgeInfosItemMap) => void;
  [key: string]: any;
};

type TaskStateMap = Record<string, DictTaskState>;

const DimensionMetricVisibleTableTransfer: React.FC<Props> = ({
  domainManger,
  knowledgeInfosMap,
  onKnowledgeInfosMapChange,
  ...restProps
}) => {
  const { selectModelId: modelId } = domainManger;
  const [dimensionValueSettingModalVisible, setDimensionValueSettingModalVisible] =
    useState<boolean>(false);
  const [currentRecord, setCurrentRecord] = useState<RecordType>({} as RecordType);
  const [currentDimensionSettingFormData, setCurrentDimensionSettingFormData] =
    useState<IChatConfig.IKnowledgeConfig>();

  const [recordLoadingMap, setRecordLoadingMap] = useState<Record<string, boolean>>({});

  const [taskStateMap, setTaskStateMap] = useState<TaskStateMap>({});

  useEffect(() => {
    queryDictLatestTaskList();
  }, []);

  const updateKnowledgeInfosMap = (record: RecordType, updateData: Record<string, any>) => {
    const { bizName, id } = record;
    const knowledgeMap = {
      ...knowledgeInfosMap,
    };
    const target = knowledgeMap[bizName];
    if (target) {
      knowledgeMap[bizName] = {
        ...target,
        ...updateData,
      };
    } else {
      knowledgeMap[bizName] = {
        itemId: id,
        bizName,
        ...updateData,
      };
    }
    onKnowledgeInfosMapChange?.(knowledgeMap);
  };

  const queryDictLatestTaskList = async () => {
    const { code, data } = await searchDictLatestTaskList({
      modelId,
    });
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

  const createDictTaskQuery = async (recordData: RecordType) => {
    setRecordLoadingMap({
      ...recordLoadingMap,
      [recordData.id]: true,
    });
    const { code } = await createDictTask({
      updateMode: 'REALTIME_ADD',
      modelAndDimPair: {
        [modelId]: [recordData.id],
      },
    });
    setRecordLoadingMap({
      ...recordLoadingMap,
      [recordData.id]: false,
    });
    if (code !== 200) {
      message.error('字典导入任务创建失败!');
      return;
    }
    setTimeout(() => {
      queryDictLatestTaskList();
    }, 2000);
  };

  const deleteDictTask = async (recordData: RecordType) => {
    const { code } = await createDictTask({
      updateMode: 'REALTIME_DELETE',
      modelAndDimPair: {
        [modelId]: [recordData.id],
      },
    });
    if (code !== 200) {
      message.error('删除字典导入任务创建失败!');
    }
  };

  let rightColumns: ColumnsType<RecordType> = [
    {
      dataIndex: 'name',
      title: '名称',
    },
    {
      dataIndex: 'type',
      width: 80,
      title: '类型',
      render: (type: SemanticNodeType) => {
        return <TransTypeTag type={type} />;
      },
    },
    {
      dataIndex: 'y',
      title: (
        <TableTitleTooltips
          title="维度值可见"
          tooltips="勾选可见后，维度值将在搜索时可以被联想出来"
        />
      ),
      width: 120,
      render: (_: any, record: RecordType) => {
        const { type, bizName } = record;
        return type === TransType.DIMENSION ? (
          <Checkbox
            checked={knowledgeInfosMap?.[bizName]?.searchEnable}
            onChange={(e: CheckboxChangeEvent) => {
              updateKnowledgeInfosMap(record, { searchEnable: e.target.checked });
              if (!e.target.checked) {
                deleteDictTask(record);
              }
            }}
            onClick={(event) => {
              event.stopPropagation();
            }}
          />
        ) : (
          <></>
        );
      },
    },
    {
      dataIndex: 'taskState',
      width: 130,
      title: (
        <Space>
          导入字典状态
          <span
            className={styles.taskStateRefreshIcon}
            onClick={() => {
              queryDictLatestTaskList();
            }}
          >
            <Tooltip title="刷新字典任务状态">
              <RedoOutlined />
            </Tooltip>
          </span>
        </Space>
      ),
      render: (_, record) => {
        const { id, type } = record;
        const target = taskStateMap[id];
        if (type === TransType.DIMENSION && target) {
          return DictTaskState[target] || '未知状态';
        }
        return '--';
      },
    },
    {
      title: '操作',
      dataIndex: 'x',
      render: (_: any, record: RecordType) => {
        const { type, bizName, id } = record;
        return type === TransType.DIMENSION ? (
          <Space>
            <Button
              style={{ padding: 0 }}
              key="importDictBtn"
              type="link"
              disabled={!knowledgeInfosMap?.[bizName]?.searchEnable}
              loading={!!recordLoadingMap[id]}
              onClick={(event) => {
                createDictTaskQuery(record);
                event.stopPropagation();
              }}
            >
              导入字典
            </Button>
            <Button
              style={{ padding: 0 }}
              key="editable"
              type="link"
              disabled={!knowledgeInfosMap?.[bizName]?.searchEnable}
              onClick={(event) => {
                setCurrentRecord(record);
                setCurrentDimensionSettingFormData(
                  knowledgeInfosMap?.[bizName]?.knowledgeAdvancedConfig,
                );
                setDimensionValueSettingModalVisible(true);
                event.stopPropagation();
              }}
            >
              可见维度值设置
            </Button>
          </Space>
        ) : (
          <></>
        );
      },
    },
  ];

  const leftColumns: ColumnsType<RecordType> = [
    {
      dataIndex: 'name',
      title: '名称',
    },
    {
      dataIndex: 'type',
      title: '类型',
      render: (type) => {
        return <TransTypeTag type={type} />;
      },
    },
  ];
  if (!knowledgeInfosMap) {
    rightColumns = leftColumns;
  }
  return (
    <>
      <Transfer {...restProps}>
        {({
          direction,
          filteredItems,
          onItemSelectAll,
          onItemSelect,
          selectedKeys: listSelectedKeys,
        }) => {
          const columns = direction === 'left' ? leftColumns : rightColumns;
          const rowSelection: TableRowSelection<TransferItem> = {
            onSelectAll(selected, selectedRows) {
              const treeSelectedKeys = selectedRows.map(({ key }) => key);
              const diffKeys = selected
                ? difference(treeSelectedKeys, listSelectedKeys)
                : difference(listSelectedKeys, treeSelectedKeys);
              onItemSelectAll(diffKeys as string[], selected);
            },
            onSelect({ key }, selected) {
              onItemSelect(key as string, selected);
            },
            selectedRowKeys: listSelectedKeys,
          };

          return (
            <Table
              rowSelection={rowSelection}
              columns={columns}
              dataSource={filteredItems as any}
              size="small"
              pagination={false}
              scroll={{ y: 450 }}
              onRow={({ key }) => ({
                onClick: () => {
                  onItemSelect(key as string, !listSelectedKeys.includes(key as string));
                },
              })}
            />
          );
        }}
      </Transfer>
      <DimensionValueSettingModal
        visible={dimensionValueSettingModalVisible}
        initialValues={currentDimensionSettingFormData}
        onSubmit={(formValues) => {
          updateKnowledgeInfosMap(currentRecord, { knowledgeAdvancedConfig: formValues });
          setDimensionValueSettingModalVisible(false);
        }}
        onCancel={() => {
          setDimensionValueSettingModalVisible(false);
        }}
      />
    </>
  );
};

// export default DimensionMetricVisibleTableTransfer;
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DimensionMetricVisibleTableTransfer);

import { Button, Drawer, message, Row, Col, Divider, Tag, Space, Popconfirm } from 'antd';
import React, { useState, useEffect, ReactNode } from 'react';
import { SemanticNodeType } from '../../enum';
import { deleteDimension, deleteMetric, deleteDatasource } from '../../service';
import moment from 'moment';
import styles from '../style.less';
import TransTypeTag from '../../components/TransTypeTag';
import { SENSITIVE_LEVEL_ENUM } from '../../constant';

type Props = {
  nodeData: any;
  onNodeChange: (params?: { eventName?: string }) => void;
  onEditBtnClick?: (nodeData: any) => void;
  [key: string]: any;
};

interface DescriptionItemProps {
  title: string;
  content: React.ReactNode;
}

type InfoListItemChildrenItem = {
  label: string;
  value: string;
  content?: ReactNode;
  hideItem?: boolean;
};

type InfoListItem = {
  title: string;
  hideItem?: boolean;
  render?: () => ReactNode;
  children?: InfoListItemChildrenItem[];
};

const DescriptionItem = ({ title, content }: DescriptionItemProps) => (
  <div style={{ marginBottom: 7, fontSize: 14 }}>
    <Space>
      <span style={{ width: 'max-content' }}>{title}:</span>
      {content}
    </Space>
  </div>
);

const NodeInfoDrawer: React.FC<Props> = ({
  nodeData,
  onNodeChange,
  onEditBtnClick,
  ...restProps
}) => {
  const [infoList, setInfoList] = useState<InfoListItem[]>([]);

  useEffect(() => {
    if (!nodeData) {
      return;
    }
    const {
      alias,
      bizName,
      createdBy,
      createdAt,
      updatedAt,
      description,
      sensitiveLevel,
      modelName,
      nodeType,
    } = nodeData;

    const list = [
      {
        title: '基本信息',
        children: [
          {
            label: '英文名称',
            value: bizName,
          },
          {
            label: '别名',
            hideItem: !alias,
            value: alias || '-',
          },
          {
            label: '所属模型',
            value: modelName,
            content: <Tag>{modelName}</Tag>,
          },

          {
            label: '描述',
            value: description,
          },
        ],
      },
      {
        title: '应用信息',
        children: [
          {
            label: '敏感度',
            value: SENSITIVE_LEVEL_ENUM[sensitiveLevel],
          },
        ],
      },
      {
        title: '创建信息',
        children: [
          {
            label: '创建人',
            value: createdBy,
          },
          {
            label: '创建时间',
            value: createdAt ? moment(createdAt).format('YYYY-MM-DD HH:mm:ss') : '',
          },
          {
            label: '更新时间',
            value: updatedAt ? moment(updatedAt).format('YYYY-MM-DD HH:mm:ss') : '',
          },
        ],
      },
    ];

    const datasourceList = [
      {
        title: '基本信息',
        children: [
          {
            label: '英文名称',
            value: bizName,
          },
          {
            label: '描述',
            value: description,
          },
        ],
      },
      {
        title: '创建信息',
        children: [
          {
            label: '创建人',
            value: createdBy,
          },
          {
            label: '创建时间',
            value: createdAt ? moment(createdAt).format('YYYY-MM-DD HH:mm:ss') : '',
          },
          {
            label: '更新时间',
            value: updatedAt ? moment(updatedAt).format('YYYY-MM-DD HH:mm:ss') : '',
          },
        ],
      },
    ];

    setInfoList(nodeType === SemanticNodeType.DATASOURCE ? datasourceList : list);
  }, [nodeData]);

  const handleDeleteConfirm = async () => {
    let deleteQuery;
    if (nodeData?.nodeType === SemanticNodeType.METRIC) {
      deleteQuery = deleteMetric;
    }
    if (nodeData?.nodeType === SemanticNodeType.DIMENSION) {
      deleteQuery = deleteDimension;
    }
    if (nodeData?.nodeType === SemanticNodeType.DATASOURCE) {
      deleteQuery = deleteDatasource;
    }
    if (!deleteQuery) {
      return;
    }
    const { code, msg } = await deleteQuery(nodeData?.uid);
    if (code === 200) {
      onNodeChange?.({ eventName: nodeData?.nodeType });
    } else {
      message.error(msg);
    }
  };
  const extraNode = (
    <div className="ant-drawer-extra">
      <Space>
        <Button
          type="primary"
          key="editBtn"
          onClick={() => {
            onEditBtnClick?.(nodeData);
          }}
        >
          编辑
        </Button>

        <Popconfirm
          title="确认删除？"
          okText="是"
          cancelText="否"
          onConfirm={() => {
            handleDeleteConfirm();
          }}
        >
          <Button danger key="deleteBtn">
            删除
          </Button>
        </Popconfirm>
      </Space>
    </div>
  );
  return (
    <>
      <Drawer
        title={
          <Space>
            {nodeData?.name}
            <TransTypeTag type={nodeData?.nodeType} />
          </Space>
        }
        placement="right"
        mask={false}
        getContainer={false}
        footer={false}
        {...restProps}
      >
        <div key={nodeData?.id} className={styles.nodeInfoDrawerContent}>
          {infoList.map((item) => {
            const { children, title, render } = item;
            return (
              <div key={title} style={{ display: item.hideItem ? 'none' : 'block' }}>
                <p className={styles.title}>{title}</p>
                {render?.() ||
                  (Array.isArray(children) &&
                    children.map((childrenItem) => {
                      return (
                        <Row
                          key={`${childrenItem.label}-${childrenItem.value}`}
                          style={{
                            marginBottom: 10,
                            display: childrenItem.hideItem ? 'none' : 'flex',
                          }}
                        >
                          <Col span={24}>
                            <DescriptionItem
                              title={childrenItem.label}
                              content={childrenItem.content || childrenItem.value}
                            />
                          </Col>
                        </Row>
                      );
                    }))}
                <Divider />
              </div>
            );
          })}
        </div>
        {nodeData?.hasAdminRes && extraNode}
      </Drawer>
    </>
  );
};

export default NodeInfoDrawer;

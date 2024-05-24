import { Drawer, Modal, Card, Row, Col, message } from 'antd';
import { ConsoleSqlOutlined, CoffeeOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
import ModelCreateForm from '../Datasource/components/ModelCreateForm';
import { getModelDetail } from '../service';
import DataSource from '../Datasource';
import { IDataSource, ISemantic } from '../data';
import styles from './style.less';

const { Meta } = Card;
type Props = {
  open: boolean;
  modelItem: ISemantic.IModelItem;
  onTypeChange?: (type: 'fast' | 'normal') => void;
  onSubmit?: () => void;
  onCancel?: () => void;
};

const ClassModelTypeModal: React.FC<Props> = ({
  open,
  onTypeChange,
  onSubmit,
  modelItem: modelBasicItem,
  onCancel,
}) => {
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [dataSourceModalVisible, setDataSourceModalVisible] = useState(false);
  const [fastModeSql, setFastModeSql] = useState<string>('');
  const [sql, setSql] = useState<string>('');
  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState<boolean>(false);
  const [dataSourceEditOpen, setDataSourceEditOpen] = useState<boolean>(false);
  const [currentDatabaseId, setCurrentDatabaseId] = useState<number>();
  const [scriptColumns, setScriptColumns] = useState<IDataSource.IExecuteSqlColumn[]>([]);
  const [sqlParams, setSqlParams] = useState<IDataSource.ISqlParamsItem[]>([]);

  const [modelItem, setModelItem] = useState<ISemantic.IModelItem>({});

  useEffect(() => {
    if (!modelBasicItem?.id || !open) {
      setCreateDataSourceModalOpen(true);
      return;
    }
  }, [modelBasicItem, open]);

  useEffect(() => {
    if (modelBasicItem?.id) {
      queryModelDetail(modelBasicItem.id);
    }
  }, [modelBasicItem]);

  const queryModelDetail = async (modelId: number) => {
    const { code, msg, data } = await getModelDetail({
      modelId,
    });
    if (code === 200) {
      setModelItem(data);
      if (data?.modelDetail?.queryType === 'table_query') {
        setDataSourceModalVisible(true);
      } else {
        setCreateModalVisible(true);
      }
    } else {
      message.error(msg);
    }
  };

  const queryDataBaseExcuteSql = (tableName: string) => {
    const sql = `select * from ${tableName}`;
    setFastModeSql(sql);
  };
  const handleCancel = () => {
    onCancel?.();
  };

  useEffect(() => {
    setSql(modelItem?.modelDetail?.sqlQuery);

    const modelDetailFields = modelItem?.modelDetail?.fields;
    if (Array.isArray(modelDetailFields)) {
      setScriptColumns(
        modelDetailFields.map((item) => {
          return {
            nameEn: item.fieldName,
            type: item.dataType,
          };
        }),
      );
    }
  }, [modelItem]);

  return (
    <>
      <Modal
        className={styles.classDataSourceTypeModal}
        open={createDataSourceModalOpen}
        onCancel={() => {
          setCreateDataSourceModalOpen(false);
          handleCancel();
        }}
        footer={null}
        centered
        closable={false}
      >
        <Row gutter={16} style={{ marginTop: '0px' }}>
          <Col span={12}>
            <Card
              hoverable
              style={{ height: 220 }}
              onClick={() => {
                onTypeChange?.('fast');
                setCreateDataSourceModalOpen(false);

                setDataSourceModalVisible(true);
              }}
              cover={
                <CoffeeOutlined
                  width={240}
                  style={{ marginTop: '45px', height: 75, fontSize: '48px', color: '#1890ff' }}
                />
              }
            >
              <Meta title="快速创建" description="自动进行模型可视化创建" />
            </Card>
          </Col>
          <Col span={12}>
            <Card
              onClick={() => {
                onTypeChange?.('normal');
                setCreateDataSourceModalOpen(false);

                setCreateModalVisible(true);
              }}
              hoverable
              style={{ height: 220 }}
              cover={
                <ConsoleSqlOutlined
                  style={{ marginTop: '45px', height: 75, fontSize: '48px', color: '#1890ff' }}
                />
              }
            >
              <Meta title="SQL脚本" description="自定义SQL脚本创建模型" />
            </Card>
          </Col>
        </Row>
      </Modal>

      {dataSourceModalVisible && (
        <ModelCreateForm
          sql={fastModeSql}
          basicInfoFormMode="fast"
          modelItem={modelItem}
          onCancel={() => {
            setDataSourceModalVisible(false);
            handleCancel();
          }}
          onDataBaseTableChange={(tableName: string) => {
            queryDataBaseExcuteSql(tableName);
          }}
          onSubmit={() => {
            setDataSourceModalVisible(false);
            onSubmit?.();
          }}
          createModalVisible={dataSourceModalVisible}
        />
      )}
      {createModalVisible && (
        <ModelCreateForm
          sql={sql}
          databaseId={currentDatabaseId}
          basicInfoFormMode="normal"
          modelItem={modelItem}
          scriptColumns={scriptColumns}
          sqlParams={sqlParams}
          onCancel={() => {
            setCreateModalVisible(false);
            handleCancel();
          }}
          onSubmit={() => {
            setCreateModalVisible(false);
            onSubmit?.();
          }}
          createModalVisible={createModalVisible}
          onDataSourceBtnClick={() => {
            setDataSourceEditOpen(true);
          }}
          onOpenDataSourceEdit={() => {
            setDataSourceEditOpen(true);
          }}
        >
          <Drawer
            width={'100%'}
            title="模型编辑"
            open={dataSourceEditOpen}
            onClose={() => {
              setDataSourceEditOpen(false);
            }}
            footer={null}
          >
            <DataSource
              initialValues={modelItem}
              onSubmitSuccess={(dataSourceInfo) => {
                const { columns, sql, databaseId, sqlParams } = dataSourceInfo;
                setSql(sql);
                setScriptColumns(columns);
                setSqlParams(sqlParams);
                setCurrentDatabaseId(databaseId);
                setDataSourceEditOpen(false);
              }}
            />
          </Drawer>
        </ModelCreateForm>
      )}
    </>
  );
};
export default ClassModelTypeModal;

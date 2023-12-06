import { Drawer, Modal, Card, Row, Col } from 'antd';
import { ConsoleSqlOutlined, CoffeeOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import DataSourceCreateForm from '../Datasource/components/DataSourceCreateForm';
import { excuteSql } from '../service';
import type { StateType } from '../model';
import DataSource from '../Datasource';
import { IDataSource } from '../data';
import styles from './style.less';

const { Meta } = Card;
type Props = {
  open: boolean;
  dataSourceItem: IDataSource.IDataSourceItem;
  onTypeChange?: (type: 'fast' | 'normal') => void;
  onSubmit?: () => void;
  onCancel?: () => void;
  dispatch: Dispatch;
  domainManger: StateType;
};

const ClassDataSourceTypeModal: React.FC<Props> = ({
  open,
  onTypeChange,
  onSubmit,
  dataSourceItem,
  domainManger,
  onCancel,
  dispatch,
}) => {
  const { selectDomainId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);

  const [dataSourceModalVisible, setDataSourceModalVisible] = useState(false);
  const [fastModeSql, setFastModeSql] = useState<string>('');
  const [sql, setSql] = useState<string>('');

  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState<boolean>(false);
  const [dataSourceEditOpen, setDataSourceEditOpen] = useState<boolean>(false);
  const [currentDatabaseId, setCurrentDatabaseId] = useState<number>();
  const [scriptColumns, setScriptColumns] = useState<any[]>([]);

  useEffect(() => {
    if (!dataSourceItem?.id || !open) {
      setCreateDataSourceModalOpen(true);
      return;
    }
    if (dataSourceItem?.modelDetail?.queryType === 'table_query') {
      setDataSourceModalVisible(true);
    } else {
      setCreateModalVisible(true);
    }
  }, [dataSourceItem, open]);

  const queryDataBaseExcuteSql = (tableName: string) => {
    const sql = `select * from ${tableName}`;
    setFastModeSql(sql);

    dispatch({
      type: 'domainManger/queryDataBaseExcuteSql',
      payload: {
        sql,
        domainId: selectDomainId,
        tableName,
      },
    });
  };
  const handleCancel = () => {
    onCancel?.();
  };

  useEffect(() => {
    queryTableColumnListByScript(dataSourceItem);
  }, [dataSourceItem]);

  const fetchTaskResult = (params) => {
    setScriptColumns(params.columns);
  };

  const queryTableColumnListByScript = async (dataSource: IDataSource.IDataSourceItem) => {
    if (!dataSource?.modelDetail?.sqlQuery) {
      return;
    }
    const { code, data } = await excuteSql({
      sql: dataSource.modelDetail?.sqlQuery,
      id: dataSource.databaseId,
    });
    if (code === 200) {
      fetchTaskResult(data);
      setSql(dataSource?.modelDetail?.sqlQuery);
    }
  };

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
                  style={{ paddingTop: '45px', height: 75, fontSize: '48px', color: '#1890ff' }}
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
                  style={{ paddingTop: '45px', height: 75, fontSize: '48px', color: '#1890ff' }}
                />
              }
            >
              <Meta title="SQL脚本" description="自定义SQL脚本创建模型" />
            </Card>
          </Col>
        </Row>
      </Modal>

      {dataSourceModalVisible && (
        <DataSourceCreateForm
          sql={fastModeSql}
          basicInfoFormMode="fast"
          dataSourceItem={dataSourceItem}
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
        <DataSourceCreateForm
          sql={sql}
          databaseId={currentDatabaseId}
          basicInfoFormMode="normal"
          dataSourceItem={dataSourceItem}
          scriptColumns={scriptColumns}
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
            title="数据源编辑"
            open={dataSourceEditOpen}
            onClose={() => {
              setDataSourceEditOpen(false);
            }}
            footer={null}
          >
            <DataSource
              initialValues={dataSourceItem}
              onSubmitSuccess={(dataSourceInfo) => {
                const { columns, sql, databaseId } = dataSourceInfo;
                setSql(sql);
                setScriptColumns(columns);
                setCurrentDatabaseId(databaseId);
                setDataSourceEditOpen(false);
              }}
            />
          </Drawer>
        </DataSourceCreateForm>
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassDataSourceTypeModal);

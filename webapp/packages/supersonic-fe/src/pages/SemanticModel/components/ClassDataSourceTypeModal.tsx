import { Button, Drawer, Result, Modal, Card, Row, Col } from 'antd';
import { ConsoleSqlOutlined, CoffeeOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { history, connect } from 'umi';
import DataSourceCreateForm from '../Datasource/components/DataSourceCreateForm';
import type { StateType } from '../model';
import DataSource from '../Datasource';
import { IDataSource } from '../data';

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

  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState(false);

  useEffect(() => {
    if (!dataSourceItem || !open) {
      setCreateDataSourceModalOpen(open);
      return;
    }
    if (dataSourceItem?.datasourceDetail?.queryType === 'table_query') {
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

  return (
    <>
      <Modal
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
                  style={{ paddingTop: '45px', height: 120, fontSize: '48px', color: '#1890ff' }}
                />
              }
            >
              <Meta title="快速创建" description="自动进行数据源可视化创建" />
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
                  style={{ paddingTop: '45px', height: 120, fontSize: '48px', color: '#1890ff' }}
                />
              }
            >
              <Meta title="SQL脚本" description="自定义SQL脚本创建数据源" />
            </Card>
          </Col>
        </Row>
      </Modal>

      {dataSourceModalVisible && (
        <DataSourceCreateForm
          sql={fastModeSql}
          basicInfoFormMode="fast"
          domainId={Number(selectDomainId)}
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
        <Drawer
          width={'100%'}
          destroyOnClose
          title="数据源编辑"
          open={true}
          onClose={() => {
            setCreateModalVisible(false);
            handleCancel();
          }}
          footer={null}
        >
          <DataSource
            initialValues={dataSourceItem}
            onSubmitSuccess={() => {
              setCreateModalVisible(false);
              onSubmit?.();
            }}
          />
        </Drawer>
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassDataSourceTypeModal);

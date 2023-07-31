import { Modal, Card, Row, Col, Result, Button } from 'antd';
import { ConsoleSqlOutlined, CoffeeOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
import { history, connect } from 'umi';
import type { StateType } from '../model';
const { Meta } = Card;
type Props = {
  open: boolean;
  domainManger: StateType;
  onTypeChange: (type: 'fast' | 'normal') => void;
  onCancel?: () => void;
};

const ClassDataSourceTypeModal: React.FC<Props> = ({
  open,
  onTypeChange,
  domainManger,
  onCancel,
}) => {
  const { selectDomainId, dataBaseConfig } = domainManger;
  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState(false);
  useEffect(() => {
    setCreateDataSourceModalOpen(open);
  }, [open]);

  return (
    <>
      <Modal
        open={createDataSourceModalOpen}
        onCancel={() => {
          setCreateDataSourceModalOpen(false);
          onCancel?.();
        }}
        footer={null}
        centered
        closable={false}
      >
        {dataBaseConfig && dataBaseConfig.id ? (
          <Row gutter={16} style={{ marginTop: '0px' }}>
            <Col span={12}>
              <Card
                hoverable
                style={{ height: 220 }}
                onClick={() => {
                  onTypeChange('fast');
                  setCreateDataSourceModalOpen(false);
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
                  onTypeChange('normal');
                  setCreateDataSourceModalOpen(false);
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
        ) : (
          <Result
            status="warning"
            subTitle="创建数据源需要先完成数据库设置"
            extra={
              <Button
                type="primary"
                key="console"
                onClick={() => {
                  history.replace(`/semanticModel/${selectDomainId}/dataBase`);
                  onCancel?.();
                }}
              >
                去设置
              </Button>
            }
          />
        )}
      </Modal>
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassDataSourceTypeModal);

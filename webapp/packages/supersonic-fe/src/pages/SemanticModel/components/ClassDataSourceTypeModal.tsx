import { Modal, Card, Row, Col } from 'antd';
import { ConsoleSqlOutlined, CoffeeOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
const { Meta } = Card;
type Props = {
  open: boolean;
  onTypeChange: (type: 'fast' | 'normal') => void;
  onCancel?: () => void;
};

const ClassDataSourceTypeModal: React.FC<Props> = ({ open, onTypeChange, onCancel }) => {
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
      </Modal>
    </>
  );
};
export default ClassDataSourceTypeModal;

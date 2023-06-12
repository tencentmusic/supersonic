import React, { useEffect, useState, useRef } from 'react';
import { Button, message, Form, Space, Drawer, Input } from 'antd';
import { ProCard } from '@ant-design/pro-components';
import { connect } from 'umi';
import { createGroupAuth, updateGroupAuth } from '../../service';
import PermissionCreateForm from './PermissionCreateForm';
import type { StateType } from '../../model';
import SqlEditor from '@/components/SqlEditor';
import DimensionMetricVisibleTransfer from '../Entity/DimensionMetricVisibleTransfer';
import styles from '../style.less';

type Props = {
  domainManger: StateType;
  permissonData: any;
  domainId: number;
  onCancel: () => void;
  visible: boolean;
  onSubmit: (params?: any) => void;
};
const FormItem = Form.Item;
const TextArea = Input.TextArea;
const PermissionCreateDrawer: React.FC<Props> = ({
  domainManger,
  visible,
  permissonData,
  domainId,
  onCancel,
  onSubmit,
}) => {
  const { dimensionList, metricList } = domainManger;
  const [form] = Form.useForm();
  const basicInfoFormRef = useRef<any>(null);
  const [sourceDimensionList, setSourceDimensionList] = useState<any[]>([]);
  const [sourceMetricList, setSourceMetricList] = useState<any[]>([]);
  const [selectedDimensionKeyList, setSelectedDimensionKeyList] = useState<string[]>([]);
  const [selectedMetricKeyList, setSelectedMetricKeyList] = useState<string[]>([]);

  useEffect(() => {
    const list = dimensionList.reduce((highList: any[], item: any) => {
      const { name, bizName, sensitiveLevel } = item;
      if (sensitiveLevel === 2) {
        highList.push({ id: bizName, name, type: 'dimension' });
      }
      return highList;
    }, []);
    setSourceDimensionList(list);
  }, [dimensionList]);

  useEffect(() => {
    const list = metricList.reduce((highList: any[], item: any) => {
      const { name, bizName, sensitiveLevel } = item;
      if (sensitiveLevel === 2) {
        highList.push({ id: bizName, name, type: 'metric' });
      }
      return highList;
    }, []);
    setSourceMetricList(list);
  }, [metricList]);

  const saveAuth = async () => {
    const basicInfoFormValues = await basicInfoFormRef.current.formRef.validateFields();
    const values = await form.validateFields();
    const { dimensionFilters, dimensionFilterDescription } = values;

    const { authRules = [] } = permissonData;
    let target = authRules?.[0];
    if (!target) {
      target = { dimensions: dimensionList };
    } else {
      target.dimensions = dimensionList;
    }
    permissonData.authRules = [target];

    let saveAuthQuery = createGroupAuth;
    if (basicInfoFormValues.groupId) {
      saveAuthQuery = updateGroupAuth;
    }
    const { code, msg } = await saveAuthQuery({
      ...basicInfoFormValues,
      dimensionFilters: [dimensionFilters],
      dimensionFilterDescription,
      authRules: [
        {
          dimensions: selectedDimensionKeyList,
          metrics: selectedMetricKeyList,
        },
      ],
      domainId,
    });

    if (code === 200) {
      onSubmit?.();
      message.success('保存成功');
      return;
    }
    message.error(msg);
  };

  useEffect(() => {
    form.resetFields();
    const { dimensionFilters, dimensionFilterDescription } = permissonData;
    form.setFieldsValue({
      dimensionFilterDescription,
      dimensionFilters: Array.isArray(dimensionFilters) ? dimensionFilters[0] || '' : '',
    });

    setSelectedDimensionKeyList(permissonData?.authRules?.[0]?.dimensions || []);
    setSelectedMetricKeyList(permissonData?.authRules?.[0]?.metrics || []);
  }, [permissonData]);

  const renderFooter = () => {
    return (
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button
            type="primary"
            onClick={() => {
              saveAuth();
            }}
          >
            完成
          </Button>
        </Space>
      </div>
    );
  };

  return (
    <>
      <Drawer
        width={'100%'}
        className={styles.permissionDrawer}
        destroyOnClose
        title={'权限组信息'}
        maskClosable={false}
        open={visible}
        footer={renderFooter()}
        onClose={onCancel}
      >
        <div style={{ overflow: 'auto', margin: '0 auto', width: '1000px' }}>
          <Space direction="vertical" style={{ width: '100%' }} size={20}>
            <ProCard title="基本信息" bordered>
              <PermissionCreateForm
                ref={basicInfoFormRef}
                permissonData={permissonData}
                domainId={domainId}
              />
            </ProCard>

            <ProCard title="列权限" bordered>
              <DimensionMetricVisibleTransfer
                titles={['未授权维度/指标', '已授权维度/指标']}
                sourceList={[...sourceDimensionList, ...sourceMetricList]}
                targetList={[...selectedDimensionKeyList, ...selectedMetricKeyList]}
                onChange={(bizNameList: string[]) => {
                  const dimensionKeyChangeList = dimensionList.reduce(
                    (dimensionChangeList: string[], item: any) => {
                      if (bizNameList.includes(item.bizName)) {
                        dimensionChangeList.push(item.bizName);
                      }
                      return dimensionChangeList;
                    },
                    [],
                  );
                  const metricKeyChangeList = metricList.reduce(
                    (metricChangeList: string[], item: any) => {
                      if (bizNameList.includes(item.bizName)) {
                        metricChangeList.push(item.bizName);
                      }
                      return metricChangeList;
                    },
                    [],
                  );
                  setSelectedDimensionKeyList(dimensionKeyChangeList);
                  setSelectedMetricKeyList(metricKeyChangeList);
                }}
              />
            </ProCard>

            <ProCard bordered title="行权限">
              <div>
                <Form form={form} layout="vertical">
                  <FormItem name="dimensionFilters" label="表达式">
                    <SqlEditor height={'150px'} />
                  </FormItem>
                  <FormItem name="dimensionFilterDescription" label="描述">
                    <TextArea placeholder="行权限描述" />
                  </FormItem>
                </Form>
              </div>
            </ProCard>
          </Space>
        </div>
      </Drawer>
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(PermissionCreateDrawer);

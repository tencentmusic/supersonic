import React, { useEffect, useState, useRef } from 'react';
import { Button, message, Form, Space, Drawer, Input } from 'antd';
import { ProCard } from '@ant-design/pro-components';
import { useModel } from 'umi';
import { createGroupAuth, updateGroupAuth } from '../../service';
import PermissionCreateForm from './PermissionCreateForm';
import type { StateType } from '../../model';
import SqlEditor from '@/components/SqlEditor';
import { TransType } from '../../enum';
import DimensionMetricVisibleTransfer from '../Entity/DimensionMetricVisibleTransfer';
import { wrapperTransTypeAndId } from '../../utils';
import styles from '../style.less';

type Props = {
  permissonData: any;
  onCancel: () => void;
  visible: boolean;
  onSubmit: (params?: any) => void;
};
const FormItem = Form.Item;
const TextArea = Input.TextArea;
const PermissionCreateDrawer: React.FC<Props> = ({
  visible,
  permissonData,
  onCancel,
  onSubmit,
}) => {
  const modelModel = useModel('SemanticModel.modelData');
  const dimensionModel = useModel('SemanticModel.dimensionData');
  const metricModel = useModel('SemanticModel.metricData');

  const { selectModelId: modelId } = modelModel;
  const { MdimensionList: dimensionList } = dimensionModel;
  const { MmetricList: metricList } = metricModel;

  const [form] = Form.useForm();
  const basicInfoFormRef = useRef<any>(null);
  const [selectedDimensionKeyList, setSelectedDimensionKeyList] = useState<string[]>([]);
  const [selectedMetricKeyList, setSelectedMetricKeyList] = useState<string[]>([]);
  const [selectedKeyList, setSelectedKeyList] = useState<string[]>([]);

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
      modelId,
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
    const dimensionAuth = permissonData?.authRules?.[0]?.dimensions || [];
    const metricAuth = permissonData?.authRules?.[0]?.metrics || [];
    setSelectedDimensionKeyList(dimensionAuth);
    setSelectedMetricKeyList(metricAuth);

    const dimensionKeys = dimensionList.reduce((dimensionChangeList: string[], item: any) => {
      if (dimensionAuth.includes(item.bizName)) {
        dimensionChangeList.push(wrapperTransTypeAndId(TransType.DIMENSION, item.id));
      }
      return dimensionChangeList;
    }, []);
    const metricKeys = metricList.reduce((metricChangeList: string[], item: any) => {
      if (metricAuth.includes(item.bizName)) {
        metricChangeList.push(wrapperTransTypeAndId(TransType.METRIC, item.id));
      }
      return metricChangeList;
    }, []);
    setSelectedKeyList([...dimensionKeys, ...metricKeys]);
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
        <div style={{ overflow: 'auto', margin: '0 auto', width: '1200px' }}>
          <Space direction="vertical" style={{ width: '100%' }} size={20}>
            <ProCard title="基本信息" bordered>
              <PermissionCreateForm ref={basicInfoFormRef} permissonData={permissonData} />
            </ProCard>

            <ProCard title="列权限" bordered tooltip="仅对敏感度为高的指标/维度进行授权">
              <DimensionMetricVisibleTransfer
                titles={['未授权维度/指标', '已授权维度/指标']}
                listStyle={{
                  width: 520,
                  height: 600,
                }}
                sourceList={[
                  ...dimensionList
                    .map((item) => {
                      const transType = TransType.DIMENSION;
                      const { id } = item;
                      return {
                        ...item,
                        transType,
                        key: wrapperTransTypeAndId(transType, id),
                      };
                    })
                    .filter((item) => item.sensitiveLevel === 2),
                  ...metricList
                    .map((item) => {
                      const transType = TransType.METRIC;
                      const { id } = item;
                      return {
                        ...item,
                        transType,
                        key: wrapperTransTypeAndId(transType, id),
                      };
                    })
                    .filter((item) => item.sensitiveLevel === 2),
                ]}
                targetList={selectedKeyList}
                onChange={(newTargetKeys: string[]) => {
                  setSelectedKeyList(newTargetKeys);
                  const dimensionKeyChangeList = dimensionList.reduce(
                    (dimensionChangeList: string[], item: any) => {
                      if (
                        newTargetKeys.includes(wrapperTransTypeAndId(TransType.DIMENSION, item.id))
                      ) {
                        dimensionChangeList.push(item.bizName);
                      }
                      return dimensionChangeList;
                    },
                    [],
                  );
                  const metricKeyChangeList = metricList.reduce(
                    (metricChangeList: string[], item: any) => {
                      if (
                        newTargetKeys.includes(wrapperTransTypeAndId(TransType.METRIC, item.id))
                      ) {
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

export default PermissionCreateDrawer;

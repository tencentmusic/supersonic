import React, { useEffect, useState, useRef } from 'react';
import { Button, message, Form, Space, Drawer, Input, Switch } from 'antd';
import { ProCard } from '@ant-design/pro-components';
import {
  createDataSetAuthGroup,
  updateDataSetAuthGroup,
} from '@/services/datasetAuth';
import DatasetPermissionForm from './DatasetPermissionForm';
import SqlEditor from '@/components/SqlEditor';
import { TransType } from '../../enum';
import DimensionMetricVisibleTransfer from '../Entity/DimensionMetricVisibleTransfer';
import { wrapperTransTypeAndId } from '../../utils';
import { MSG } from '@/common/messages';
import styles from '../style.less';

type Props = {
  datasetId: number;
  permissonData: any;
  dimensionList: any[];
  metricList: any[];
  onCancel: () => void;
  visible: boolean;
  onSubmit: (params?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const DatasetPermissionDrawer: React.FC<Props> = ({
  visible,
  datasetId,
  permissonData,
  dimensionList,
  metricList,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();
  const basicInfoFormRef = useRef<any>(null);
  const [selectedDimensionKeyList, setSelectedDimensionKeyList] = useState<string[]>([]);
  const [selectedMetricKeyList, setSelectedMetricKeyList] = useState<string[]>([]);
  const [selectedKeyList, setSelectedKeyList] = useState<string[]>([]);
  const [inheritFromModel, setInheritFromModel] = useState<boolean>(true);

  const saveAuth = async () => {
    const basicInfoFormValues = await basicInfoFormRef.current.formRef.validateFields();
    const values = await form.validateFields();
    const { dimensionFilters, dimensionFilterDescription } = values;

    let saveAuthQuery = createDataSetAuthGroup;
    if (basicInfoFormValues.groupId) {
      saveAuthQuery = updateDataSetAuthGroup;
    }

    const { code, msg } = await saveAuthQuery({
      ...basicInfoFormValues,
      datasetId,
      dimensionFilters: dimensionFilters ? [dimensionFilters] : [],
      dimensionFilterDescription,
      inheritFromModel: inheritFromModel ? 1 : 0,
      authRules: [
        {
          dimensions: selectedDimensionKeyList,
          metrics: selectedMetricKeyList,
        },
      ],
    });

    if (code === 200) {
      onSubmit?.();
      message.success(MSG.SAVE_SUCCESS);
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
    setInheritFromModel(permissonData.inheritFromModel !== 0);

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
          <Button type="primary" onClick={saveAuth}>
            完成
          </Button>
        </Space>
      </div>
    );
  };

  return (
    <Drawer
      width={'100%'}
      className={styles.permissionDrawer}
      destroyOnClose
      title={'数据集权限组信息'}
      maskClosable={false}
      open={visible}
      footer={renderFooter()}
      onClose={onCancel}
    >
      <div style={{ overflow: 'auto', margin: '0 auto', width: '1200px' }}>
        <Space direction="vertical" style={{ width: '100%' }} size={20}>
          <ProCard title="基本信息" bordered>
            <DatasetPermissionForm ref={basicInfoFormRef} permissonData={permissonData} />
            <Form.Item label="继承模型权限" style={{ marginBottom: 0 }}>
              <Switch
                checked={inheritFromModel}
                onChange={(checked) => setInheritFromModel(checked)}
                checkedChildren="是"
                unCheckedChildren="否"
              />
              <span style={{ marginLeft: 8, color: '#999' }}>
                开启后将继承关联模型的权限配置
              </span>
            </Form.Item>
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
                    if (newTargetKeys.includes(wrapperTransTypeAndId(TransType.METRIC, item.id))) {
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
              <Form form={form} layout="vertical" name="datasetPermissionForm">
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
  );
};

export default DatasetPermissionDrawer;

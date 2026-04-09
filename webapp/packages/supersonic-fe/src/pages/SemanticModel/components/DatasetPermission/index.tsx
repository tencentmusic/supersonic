import React, { useEffect, useState } from 'react';
import { Card, message, Form, Switch, Divider } from 'antd';
import { useModel } from '@umijs/max';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import DatasetPermissionTable from './DatasetPermissionTable';
import PermissionStatus from './PermissionStatus';
import { updateView, getDataSetDetail } from '../../service';
import styles from '../style.less';

type Props = {
  datasetId: number;
};

const FormItem = Form.Item;

const DatasetPermissionSection: React.FC<Props> = ({ datasetId }) => {
  const [form] = Form.useForm();
  const [datasetData, setDatasetData] = useState<any>({});
  const [dimensionList, setDimensionList] = useState<any[]>([]);
  const [metricList, setMetricList] = useState<any[]>([]);

  const dimensionModel = useModel('SemanticModel.dimensionData');
  const metricModel = useModel('SemanticModel.metricData');

  useEffect(() => {
    if (dimensionModel.MdimensionList) {
      setDimensionList(dimensionModel.MdimensionList);
    }
  }, [dimensionModel.MdimensionList]);

  useEffect(() => {
    if (metricModel.MmetricList) {
      setMetricList(metricModel.MmetricList);
    }
  }, [metricModel.MmetricList]);

  const queryDatasetDetail = async () => {
    if (!datasetId) {
      return;
    }
    const { code, data, msg } = await getDataSetDetail(datasetId);
    if (code === 200) {
      setDatasetData(data);
      form.setFieldsValue({
        admin: data.admin ? data.admin.split(',') : [],
        viewer: data.viewer ? data.viewer.split(',') : [],
        isOpen: data.isOpen === 1,
      });
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDatasetDetail();
  }, [datasetId]);

  const handleAdminChange = async (values: string | string[]) => {
    const admin = Array.isArray(values) ? values.join(',') : values;
    const { code, msg } = await updateView({
      ...datasetData,
      admin,
    });
    if (code === 200) {
      message.success('管理员更新成功');
      setDatasetData({ ...datasetData, admin });
    } else {
      message.error(msg);
    }
  };

  const handleViewerChange = async (values: string | string[]) => {
    const viewer = Array.isArray(values) ? values.join(',') : values;
    const { code, msg } = await updateView({
      ...datasetData,
      viewer,
    });
    if (code === 200) {
      message.success('可查看用户更新成功');
      setDatasetData({ ...datasetData, viewer });
    } else {
      message.error(msg);
    }
  };

  const handleIsOpenChange = async (checked: boolean) => {
    const isOpen = checked ? 1 : 0;
    const { code, msg } = await updateView({
      ...datasetData,
      isOpen,
    });
    if (code === 200) {
      message.success('公开设置更新成功');
      setDatasetData({ ...datasetData, isOpen });
    } else {
      message.error(msg);
    }
  };

  return (
    <div className={styles.permissionSection}>
      <PermissionStatus datasetId={datasetId} />

      <Card title="基本权限设置" bordered={false} style={{ marginBottom: 16 }}>
        <Form form={form} layout="vertical" name="datasetPermissionBaseForm">
          <FormItem name="admin" label="管理员">
            <SelectTMEPerson
              placeholder="请选择管理员"
              onChange={handleAdminChange}
            />
          </FormItem>
          <FormItem name="viewer" label="可查看用户">
            <SelectTMEPerson
              placeholder="请选择可查看用户"
              onChange={handleViewerChange}
            />
          </FormItem>
          <FormItem name="isOpen" label="是否公开" valuePropName="checked">
            <Switch
              checkedChildren="公开"
              unCheckedChildren="私有"
              onChange={handleIsOpenChange}
            />
            <span style={{ marginLeft: 8, color: '#999' }}>
              公开后所有用户可查看此数据集
            </span>
          </FormItem>
        </Form>
      </Card>

      <Divider />

      <Card title="细粒度权限组" bordered={false}>
        <DatasetPermissionTable
          datasetId={datasetId}
          dimensionList={dimensionList}
          metricList={metricList}
        />
      </Card>
    </div>
  );
};

export default DatasetPermissionSection;

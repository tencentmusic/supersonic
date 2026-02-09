import { useEffect, forwardRef, useImperativeHandle, useState } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button, Space, Switch, Divider, InputNumber, Collapse } from 'antd';
import {
  saveDatabase,
  testDatabaseConnect,
  getDatabaseParameters,
  getDatabaseDetail,
  getDatabaseList,
} from '../../service';
import { formLayout } from '@/components/FormHelper/utils';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { ConfigParametersItem } from '../../../System/types';
import { genneratorFormItemList } from '../../utils';
import { ISemantic } from '../../data';
import { encryptPassword, decryptPassword } from '@/utils/utils';
import CryptoJS from 'crypto-js';
import CronInput from '../../../ReportSchedule/components/CronInput';
import {
  getSyncConfigList,
  createSyncConfig,
  updateSyncConfig,
} from '@/services/dataSync';
import type { DataSyncConfig } from '@/services/dataSync';
import styles from '../style.less';

type Props = {
  domainId?: number;
  databaseId?: number;
  hideSubmitBtn?: boolean;
  onSubmit?: (params?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const DatabaseCreateForm: ForwardRefRenderFunction<any, Props> = (
  { domainId, databaseId, onSubmit, hideSubmitBtn = false },
  ref,
) => {
  const encryptKey = CryptoJS.enc.Utf8.parse('supersonic@2024');
  const [form] = Form.useForm();
  const [selectedDbType, setSelectedDbType] = useState<string>('');
  const [databaseOptions, setDatabaseOptions] = useState<{ value: string; label: string }[]>([]);
  const [databaseConfig, setDatabaseConfig] = useState<Record<string, ConfigParametersItem[]>>({});
  const [testLoading, setTestLoading] = useState<boolean>(false);
  const [dataBaseDetail, setDataBaseDetail] = useState<ISemantic.IDatabaseItem>();

  // Sync config states
  const [syncEnabled, setSyncEnabled] = useState<boolean>(false);
  const [existingSyncConfig, setExistingSyncConfig] = useState<DataSyncConfig | undefined>();
  const [allDatabases, setAllDatabases] = useState<ISemantic.IDatabaseItem[]>([]);

  useEffect(() => {
    form.resetFields();
    if (dataBaseDetail) {
      form.setFieldsValue({ ...dataBaseDetail });
      setSelectedDbType(dataBaseDetail?.type);
    }
  }, [dataBaseDetail]);

  useEffect(() => {
    if (databaseId) {
      queryDatabaseDetail(databaseId);
      loadSyncConfig(databaseId);
    }
  }, [databaseId]);

  useEffect(() => {
    queryDatabaseConfig();
    loadAllDatabases();
  }, []);

  const queryDatabaseDetail = async (id: number) => {
    const { code, msg, data } = await getDatabaseDetail(id);
    if (code === 200) {
      setDataBaseDetail({ ...data, password: data.password ? decryptPassword(data.password) : '' });
      return;
    }
    message.error(msg);
  };

  const queryDatabaseConfig = async () => {
    const { code, msg, data } = await getDatabaseParameters();
    if (code === 200) {
      const options = Object.keys(data).map((sqlName: string) => {
        return {
          value: sqlName,
          label: sqlName,
        };
      });
      setDatabaseConfig(data);
      setDatabaseOptions(options);
      return;
    }
    message.error(msg);
  };

  const loadAllDatabases = async () => {
    const { code, data } = await getDatabaseList();
    if (code === 200) {
      setAllDatabases(data || []);
    }
  };

  const loadSyncConfig = async (dbId: number) => {
    try {
      const res = await getSyncConfigList({ current: 1, pageSize: 100 });
      const configs: DataSyncConfig[] = res?.records || [];
      const found = configs.find((c: DataSyncConfig) => c.sourceDatabaseId === dbId);
      if (found) {
        setExistingSyncConfig(found);
        setSyncEnabled(true);
        form.setFieldsValue({
          syncTargetDatabaseId: found.targetDatabaseId,
          syncCronExpression: found.cronExpression,
          syncRetryCount: found.retryCount ?? 3,
          syncConfig: found.syncConfig || '',
        });
      }
    } catch (e) {
      // Sync config load failure is non-blocking
    }
  };

  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
    saveDatabaseConfig,
    testDatabaseConnection,
  }));

  const saveSyncConfig = async (savedDatabaseId: number) => {
    if (!syncEnabled) {
      return;
    }
    const values = form.getFieldsValue();
    const syncData: Partial<DataSyncConfig> = {
      name: `${values.name || '数据库'} → 同步`,
      sourceDatabaseId: savedDatabaseId,
      targetDatabaseId: values.syncTargetDatabaseId,
      cronExpression: values.syncCronExpression,
      retryCount: values.syncRetryCount ?? 3,
      syncConfig: values.syncConfig || '',
      enabled: true,
    };

    if (existingSyncConfig?.id) {
      await updateSyncConfig(existingSyncConfig.id, syncData);
    } else {
      await createSyncConfig(syncData);
    }
  };

  const saveDatabaseConfig = async () => {
    const values = await form.validateFields();
    const { password } = values;
    const { code, msg, data } = await saveDatabase({
      ...(dataBaseDetail || {}),
      ...values,
      domainId,
      password: encryptPassword(password),
    });

    if (code === 200) {
      // Save sync config after database is saved
      const savedDbId = data?.id || databaseId;
      if (savedDbId && syncEnabled) {
        try {
          await saveSyncConfig(savedDbId);
          message.success('保存成功（含同步配置）');
        } catch (e) {
          message.warning('数据库保存成功，但同步配置保存失败');
        }
      } else {
        message.success('保存成功');
      }
      onSubmit?.();
      return;
    }
    message.error(msg);
  };

  const testDatabaseConnection = async () => {
    const values = await form.validateFields();
    const { password } = values;
    setTestLoading(true);
    const { code, data } = await testDatabaseConnect({
      ...values,
      domainId,
      password: encryptPassword(password),
    });
    setTestLoading(false);
    if (code === 200 && data) {
      message.success('连接测试通过');
      return;
    }
    message.error('连接测试失败');
  };

  // Filter out current database from target options
  const targetDatabaseOptions = allDatabases
    .filter((db) => db.id !== databaseId)
    .map((db) => ({
      value: db.id,
      label: `${db.name} (${db.type})`,
    }));

  return (
    <>
      <Form
        {...formLayout}
        form={form}
        layout="vertical"
        className={styles.form}
        onValuesChange={(value) => {
          const { type } = value;
          if (type) {
            setSelectedDbType(type);
          }
        }}
      >
        <FormItem name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="请输入数据库名称" />
        </FormItem>
        <FormItem
          name="type"
          label="数据库类型"
          rules={[{ required: true, message: '请选择数据库类型' }]}
        >
          <Select
            style={{ width: '100%' }}
            placeholder="请选择数据库类型"
            options={databaseOptions}
          />
        </FormItem>

        {databaseConfig[selectedDbType] && genneratorFormItemList(databaseConfig[selectedDbType])}

        <FormItem
          name="admins"
          label="管理员"
        >
          <SelectTMEPerson placeholder="请邀请团队成员" />
        </FormItem>
        <FormItem name="viewers" label="使用者">
          <SelectTMEPerson placeholder="请邀请团队成员" />
        </FormItem>

        <FormItem name="description" label="描述">
          <TextArea placeholder="请输入数据库描述" style={{ height: 100 }} />
        </FormItem>

        {/* Data Sync Configuration */}
        <Divider orientation="left">数据同步</Divider>

        <FormItem label="启用数据同步">
          <Switch
            checked={syncEnabled}
            onChange={(checked) => setSyncEnabled(checked)}
            checkedChildren="开"
            unCheckedChildren="关"
          />
        </FormItem>

        {syncEnabled && (
          <>
            <FormItem
              name="syncTargetDatabaseId"
              label="目标数据源"
              rules={[{ required: syncEnabled, message: '请选择目标数据源' }]}
            >
              <Select
                style={{ width: '100%' }}
                placeholder="选择目标数据源（分析库）"
                options={targetDatabaseOptions}
                showSearch
                optionFilterProp="label"
              />
            </FormItem>

            <FormItem
              name="syncCronExpression"
              label="同步频率"
              rules={[{ required: syncEnabled, message: '请设置同步频率' }]}
            >
              <CronInput />
            </FormItem>

            <Collapse
              ghost
              items={[
                {
                  key: 'advanced',
                  label: '高级配置',
                  children: (
                    <>
                      <FormItem name="syncRetryCount" label="重试次数" initialValue={3}>
                        <InputNumber min={0} max={10} style={{ width: '100%' }} />
                      </FormItem>
                      <FormItem
                        name="syncConfig"
                        label="同步规则 (JSON)"
                        tooltip="配置需要同步的表、同步模式、游标字段等"
                      >
                        <TextArea
                          rows={6}
                          placeholder={
                            '{\n  "tables": [\n    {\n      "source_table": "表名",\n      "target_table": "表名",\n      "sync_mode": "FULL",\n      "cursor_field": "updated_at",\n      "batch_size": 5000\n    }\n  ],\n  "channel_count": 2\n}'
                          }
                        />
                      </FormItem>
                    </>
                  ),
                },
              ]}
            />
          </>
        )}

        {!hideSubmitBtn && (
          <FormItem>
            <Space>
              <Button
                type="primary"
                loading={testLoading}
                onClick={() => {
                  testDatabaseConnection();
                }}
              >
                连接测试
              </Button>

              <Button
                type="primary"
                onClick={() => {
                  saveDatabaseConfig();
                }}
              >
                保 存
              </Button>
            </Space>
          </FormItem>
        )}
      </Form>
    </>
  );
};

export default forwardRef(DatabaseCreateForm);
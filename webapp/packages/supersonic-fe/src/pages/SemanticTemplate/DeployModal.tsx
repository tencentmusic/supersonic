import React, { useEffect, useRef, useState } from 'react';
import {
  Modal,
  Form,
  Select,
  Input,
  Alert,
  Divider,
  Button,
  Space,
  message,
  Spin,
  Checkbox,
} from 'antd';
import {
  SemanticTemplate,
  SemanticDeployParam,
  SemanticPreviewResult,
  previewDeployment,
  executeDeployment,
  getDeploymentById,
} from '@/services/semanticTemplate';
import { getDatabaseList } from '@/pages/SemanticModel/service';

interface DeployModalProps {
  visible: boolean;
  template: SemanticTemplate | null;
  hasExistingDeployment?: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  onPreviewResult: (data: SemanticPreviewResult) => void;
}

const POLL_INTERVAL = 2000;
const MAX_POLL_COUNT = 150;

const stepLabels: Record<string, string> = {
  CREATING_DOMAIN: '正在创建主题域...',
  CREATING_MODELS: '正在创建模型...',
  CREATING_RELATIONS: '正在创建模型关联...',
  CREATING_DATASET: '正在创建数据集...',
  CREATING_TERMS: '正在创建术语...',
  CREATING_AGENT: '正在配置智能助手...',
  ROLLING_BACK: '部署失败，正在回滚...',
  COMPLETED: '部署完成',
};

const DeployModal: React.FC<DeployModalProps> = ({
  visible,
  template,
  hasExistingDeployment = false,
  onCancel,
  onSuccess,
  onPreviewResult,
}) => {
  const [form] = Form.useForm();
  const [databases, setDatabases] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [deployLoading, setDeployLoading] = useState(false);
  const [deployStatus, setDeployStatus] = useState<string>('');
  const [allowRedeploy, setAllowRedeploy] = useState(false);
  const pollTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (visible) {
      loadDatabases();
      setDeployStatus('');
      setAllowRedeploy(false);
      // Set default values for config params
      if (template?.templateConfig?.configParams) {
        const defaultValues: Record<string, string> = {};
        template.templateConfig.configParams.forEach((param) => {
          if (param.defaultValue) {
            defaultValues[param.key] = param.defaultValue;
          }
        });
        form.setFieldsValue({ params: defaultValues });
      }
    } else {
      // Cleanup timer when modal closes
      stopPolling();
    }
  }, [visible, template]);

  useEffect(() => {
    return () => {
      stopPolling();
    };
  }, []);

  const stopPolling = () => {
    if (pollTimerRef.current) {
      clearInterval(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  };

  const loadDatabases = async () => {
    setLoading(true);
    try {
      const res = await getDatabaseList();
      setDatabases(res.data || []);
    } catch (error) {
      message.error('加载数据库列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handlePreview = async () => {
    try {
      const values = await form.validateFields();
      setPreviewLoading(true);
      const param: SemanticDeployParam = {
        databaseId: values.databaseId,
        params: values.params || {},
      };
      const res: any = await previewDeployment(template!.id, param);
      // API response is wrapped in { code, data, msg } format
      if (res?.code === 200 && res?.data) {
        onPreviewResult(res.data);
      } else {
        message.error(res?.msg || '预览部署失败');
      }
    } catch (error: any) {
      if (error.errorFields) {
        // Form validation error
        return;
      }
      message.error('预览部署失败');
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleDeploy = async () => {
    try {
      const values = await form.validateFields();
      setDeployLoading(true);
      setDeployStatus('正在提交部署...');
      const param: SemanticDeployParam = {
        databaseId: values.databaseId,
        allowRedeploy: hasExistingDeployment ? allowRedeploy : undefined,
        params: values.params || {},
      };
      const res: any = await executeDeployment(template!.id, param);
      // API response is wrapped in { code, data, msg } format
      if (res?.code === 200 && res?.data) {
        const deploymentId = res.data.id;
        setDeployStatus('正在部署...');
        startPolling(deploymentId);
      } else {
        message.error(res?.msg || '提交部署失败');
        setDeployLoading(false);
        setDeployStatus('');
      }
    } catch (error: any) {
      if (error.errorFields) {
        // Form validation error
        return;
      }
      message.error('提交部署失败');
      setDeployLoading(false);
      setDeployStatus('');
    }
  };

  const startPolling = (deploymentId: number) => {
    let pollCount = 0;

    pollTimerRef.current = setInterval(async () => {
      pollCount++;

      if (pollCount > MAX_POLL_COUNT) {
        stopPolling();
        setDeployLoading(false);
        setDeployStatus('');
        message.warning('部署超时，请在部署历史中查看最终状态');
        return;
      }

      try {
        const res: any = await getDeploymentById(deploymentId);
        const deployment = res?.code === 200 ? res.data : res;
        if (!deployment) return;

        if (deployment.status === 'SUCCESS') {
          stopPolling();
          setDeployLoading(false);
          setDeployStatus('');
          message.success('部署成功');
          onSuccess();
        } else if (deployment.status === 'FAILED') {
          stopPolling();
          setDeployLoading(false);
          setDeployStatus('');
          message.error(deployment.errorMessage || '部署失败');
        } else if (deployment.status === 'CANCELLED') {
          stopPolling();
          setDeployLoading(false);
          setDeployStatus('');
          message.warning('部署已取消');
        } else if (deployment.status === 'RUNNING') {
          const stepText = deployment.currentStep
            ? stepLabels[deployment.currentStep] || '正在部署...'
            : '正在部署...';
          setDeployStatus(stepText);
        }
      } catch (error) {
        // Polling error, continue trying
      }
    }, POLL_INTERVAL);
  };

  const renderConfigParams = () => {
    const params = template?.templateConfig?.configParams || [];
    if (params.length === 0) {
      return null;
    }

    return (
      <>
        <Divider>配置参数</Divider>
        {params.map((param) => (
          <Form.Item
            key={param.key}
            name={['params', param.key]}
            label={param.name}
            rules={[{ required: param.required, message: `请输入${param.name}` }]}
            tooltip={param.description}
            initialValue={param.defaultValue}
          >
            {param.type === 'DATABASE' ? (
              <Select
                placeholder={`请选择${param.name}`}
                options={databases.map((d) => ({ label: d.name, value: d.id }))}
              />
            ) : (
              <Input placeholder={`请输入${param.name}`} />
            )}
          </Form.Item>
        ))}
      </>
    );
  };

  return (
    <Modal
      title={`部署模板: ${template?.name}`}
      open={visible}
      width={700}
      onCancel={deployLoading ? undefined : onCancel}
      closable={!deployLoading}
      maskClosable={!deployLoading}
      footer={
        <Space>
          <Button onClick={onCancel} disabled={deployLoading}>
            取消
          </Button>
          <Button loading={previewLoading} onClick={handlePreview} disabled={deployLoading}>
            预览
          </Button>
          <Button type="primary" loading={deployLoading} onClick={handleDeploy}>
            部署
          </Button>
        </Space>
      }
    >
      <Spin spinning={loading || deployLoading} tip={deployStatus || undefined}>
        <Alert
          message="部署说明"
          description="部署将在选定的数据库中创建完整的语义层结构，包括主题域、模型、指标、数据集。如果模板包含 Agent 配置，还会自动创建一个可在 Chat 中使用的智能助手。"
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />

        {hasExistingDeployment && (
          <Alert
            message="该模板已有成功部署记录"
            description="重新部署可能会创建重复的语义对象。建议先删除之前部署的对象再重新部署。"
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            action={
              <Checkbox
                checked={allowRedeploy}
                onChange={(e) => setAllowRedeploy(e.target.checked)}
              >
                允许重新部署
              </Checkbox>
            }
          />
        )}

        <Form form={form} layout="vertical" name="deployForm">
          <Form.Item
            name="databaseId"
            label="目标数据库"
            rules={[{ required: true, message: '请选择数据库' }]}
          >
            <Select placeholder="请选择部署的目标数据库">
              {databases.map((db) => (
                <Select.Option key={db.id} value={db.id}>
                  {db.name} ({db.type})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          {renderConfigParams()}
        </Form>
      </Spin>
    </Modal>
  );
};

export default DeployModal;

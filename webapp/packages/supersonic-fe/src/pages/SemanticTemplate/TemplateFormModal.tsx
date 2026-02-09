import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, message, Tabs, Upload, Button, Alert, Space } from 'antd';
import { UploadOutlined, DownloadOutlined, FileTextOutlined, FormOutlined } from '@ant-design/icons';
import {
  SemanticTemplate,
  createTemplate,
  updateTemplate,
  saveBuiltinTemplate,
} from '@/services/semanticTemplate';

interface TemplateFormModalProps {
  visible: boolean;
  template: SemanticTemplate | null;
  onCancel: () => void;
  onSuccess: () => void;
}

const TEMPLATE_CATEGORIES = [
  { label: '访问统计', value: 'VISITS' },
  { label: '歌手/音乐', value: 'SINGER' },
  { label: '企业/商业', value: 'COMPANY' },
  { label: '电商', value: 'ECOMMERCE' },
  { label: '金融', value: 'FINANCE' },
  { label: '运营', value: 'OPERATIONS' },
  { label: '其他', value: 'OTHER' },
];

const TemplateFormModal: React.FC<TemplateFormModalProps> = ({
  visible,
  template,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState<string>('form');
  const [jsonContent, setJsonContent] = useState<string>('');
  const [jsonError, setJsonError] = useState<string>('');
  const [parsedConfig, setParsedConfig] = useState<any>(null);
  const isEdit = !!template;

  useEffect(() => {
    if (visible) {
      if (template) {
        form.setFieldsValue({
          name: template.name,
          bizName: template.bizName,
          category: template.category,
          description: template.description,
        });
        // Initialize JSON content from existing template config for edit mode
        if (template.templateConfig) {
          const exportJson = {
            name: template.name,
            description: template.description,
            category: template.category,
            domain: template.templateConfig.domain,
            models: template.templateConfig.models,
            dataSet: template.templateConfig.dataSet,
            agent: template.templateConfig.agent,
            terms: template.templateConfig.terms,
            configParams: template.templateConfig.configParams,
          };
          setJsonContent(JSON.stringify(exportJson, null, 2));
          setParsedConfig(exportJson);
          setJsonError('');
        }
        setActiveTab('form');
      } else {
        form.resetFields();
        setJsonContent('');
        setJsonError('');
        setParsedConfig(null);
      }
    }
  }, [visible, template]);

  const validateAndParseJson = (content: string): any => {
    if (!content.trim()) {
      setJsonError('');
      setParsedConfig(null);
      return null;
    }

    try {
      const parsed = JSON.parse(content);

      // Validate required fields
      if (!parsed.name) {
        setJsonError('缺少必填字段: name');
        setParsedConfig(null);
        return null;
      }

      if (!parsed.domain) {
        setJsonError('缺少必填字段: domain');
        setParsedConfig(null);
        return null;
      }

      if (!parsed.models || !Array.isArray(parsed.models) || parsed.models.length === 0) {
        setJsonError('缺少必填字段: models (至少需要一个模型)');
        setParsedConfig(null);
        return null;
      }

      setJsonError('');
      setParsedConfig(parsed);

      // Auto-fill form fields from JSON
      form.setFieldsValue({
        name: parsed.name,
        bizName: parsed.domain?.bizName || parsed.name.toLowerCase().replace(/\s+/g, '_'),
        category: parsed.category || 'OTHER',
        description: parsed.description,
      });

      return parsed;
    } catch (e: any) {
      setJsonError(`JSON 解析错误: ${e.message}`);
      setParsedConfig(null);
      return null;
    }
  };

  const handleJsonChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const content = e.target.value;
    setJsonContent(content);
    validateAndParseJson(content);
  };

  const handleFileUpload = (file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target?.result as string;
      setJsonContent(content);
      validateAndParseJson(content);
    };
    reader.readAsText(file);
    return false; // Prevent default upload behavior
  };

  // Normalize measure fields to match backend format
  const normalizeMeasures = (measures: any[]) => {
    if (!measures) return [];
    return measures.map((m) => ({
      ...m,
      // Map 'agg' to 'aggOperator' if needed
      aggOperator: m.aggOperator || m.agg,
      // Default createMetric to true
      createMetric: m.createMetric !== false,
    }));
  };

  // Normalize models to match backend format
  const normalizeModels = (models: any[]) => {
    if (!models) return [];
    return models.map((model) => ({
      ...model,
      // If tableName not specified, use bizName
      tableName: model.tableName || model.bizName,
      measures: normalizeMeasures(model.measures),
    }));
  };

  const buildTemplateConfig = (values: any) => {
    // If we have parsed JSON config and in JSON mode, use the parsed config
    if (parsedConfig && activeTab === 'json') {
      return {
        domain: parsedConfig.domain,
        models: normalizeModels(parsedConfig.models),
        modelRelations: parsedConfig.modelRelations,
        dataSet: parsedConfig.dataSet || {
          name: `${values.name}数据集`,
          bizName: `${values.bizName}_dataset`,
        },
        agent: parsedConfig.agent || {
          name: `${values.name}分析助手`,
          description: parsedConfig.description || `${values.name}的AI分析助手`,
          examples: [],
        },
        plugins: parsedConfig.plugins,
        terms: parsedConfig.terms,
        configParams: parsedConfig.configParams || [],
      };
    }

    // Form mode: preserve existing templateConfig if editing, only update basic fields
    if (isEdit && template?.templateConfig) {
      return {
        ...template.templateConfig,
        domain: {
          ...template.templateConfig.domain,
          name: values.name,
          description: values.description,
        },
      };
    }

    // Default empty config for new template in form mode
    return {
      domain: {
        name: values.name,
        bizName: values.bizName,
        description: values.description,
      },
      models: [],
      dataSet: {
        name: `${values.name}数据集`,
        bizName: `${values.bizName}_dataset`,
      },
      agent: {
        name: `${values.name}分析助手`,
        description: `${values.name}的AI分析助手`,
        enableSearch: true,
      },
      configParams: [],
    };
  };

  const handleSubmit = async () => {
    // Validate JSON if in JSON mode
    if (activeTab === 'json') {
      if (!jsonContent.trim()) {
        message.error('请输入或上传 JSON 配置');
        return;
      }
      if (jsonError) {
        message.error('请修复 JSON 错误后再提交');
        return;
      }
      if (!parsedConfig) {
        message.error('请输入有效的模板配置');
        return;
      }
    }

    try {
      const values = await form.validateFields();
      const templateData: Partial<SemanticTemplate> = {
        ...values,
        templateConfig: buildTemplateConfig(values),
      };

      let res: any;
      if (isEdit) {
        if (template!.isBuiltin) {
          res = await saveBuiltinTemplate(templateData);
        } else {
          res = await updateTemplate(template!.id, templateData);
        }
      } else {
        res = await createTemplate(templateData);
      }

      if (res?.code === 200) {
        message.success(`模板${isEdit ? '更新' : '创建'}成功`);
        onSuccess();
      } else {
        message.error(res?.msg || `${isEdit ? '更新' : '创建'}模板失败`);
      }
    } catch (error: any) {
      if (error.errorFields) {
        return;
      }
      message.error(`${isEdit ? '更新' : '创建'}模板失败`);
    }
  };

  const renderFormMode = () => (
    <>
      <Form.Item
        name="name"
        label="模板名称"
        rules={[{ required: true, message: '请输入模板名称' }]}
      >
        <Input placeholder="例如: 产品分析模板" />
      </Form.Item>

      <Form.Item
        name="bizName"
        label="模板代码"
        rules={[
          { required: true, message: '请输入模板代码' },
          {
            pattern: /^[a-z][a-z0-9_]*$/,
            message: '代码必须以小写字母开头，只能包含小写字母、数字和下划线',
          },
        ]}
      >
        <Input placeholder="例如: product_analytics" disabled={isEdit} />
      </Form.Item>

      <Form.Item
        name="category"
        label="模板类别"
        rules={[{ required: true, message: '请选择模板类别' }]}
      >
        <Select placeholder="请选择模板类别" options={TEMPLATE_CATEGORIES} />
      </Form.Item>

      <Form.Item name="description" label="描述">
        <Input.TextArea rows={3} placeholder="描述这个模板的用途..." />
      </Form.Item>
    </>
  );

  // Export current template as JSON file
  const handleExportJson = () => {
    if (!jsonContent) {
      message.warning('暂无可导出的配置');
      return;
    }
    const blob = new Blob([jsonContent], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${template?.bizName || 'template'}_config.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    message.success('导出成功');
  };

  const renderJsonMode = () => (
    <>
      <Alert
        type="info"
        showIcon
        message="JSON 模板格式"
        description={
          <div>
            必填字段: <code>name</code>, <code>domain</code>, <code>models</code>
            <br />
            可选字段: <code>metrics</code>, <code>dataSet</code>, <code>agent</code>,{' '}
            <code>terms</code>
            <br />
            <code>agent.enableReportSchedulePlugin</code>: 是否自动创建定时报表插件（默认 true）
            {isEdit && (
              <>
                <br />
                <strong>编辑模式</strong>: 可直接修改JSON或导出后编辑再导入
              </>
            )}
          </div>
        }
        style={{ marginBottom: 16 }}
      />

      <Space style={{ marginBottom: 12 }}>
        <Upload beforeUpload={handleFileUpload} accept=".json" showUploadList={false}>
          <Button icon={<UploadOutlined />}>上传 JSON 文件</Button>
        </Upload>
        {isEdit && (
          <Button icon={<DownloadOutlined />} onClick={handleExportJson}>
            导出当前配置
          </Button>
        )}
      </Space>

      <Input.TextArea
        rows={16}
        value={jsonContent}
        onChange={handleJsonChange}
        placeholder={`{
  "name": "模板名称",
  "description": "模板描述",
  "domain": {
    "name": "域名称",
    "bizName": "domain_biz_name"
  },
  "models": [
    {
      "name": "模型名称",
      "bizName": "table_name",
      "dimensions": [...],
      "measures": [...]
    }
  ],
  "agent": {
    "name": "助手名称",
    "examples": ["示例问题1", "示例问题2"],
    "enableReportSchedulePlugin": false
  }
}`}
        style={{ fontFamily: 'monospace', fontSize: 12 }}
      />

      {jsonError && (
        <Alert type="error" message={jsonError} style={{ marginTop: 12 }} showIcon />
      )}

      {parsedConfig && !jsonError && (
        <Alert
          type="success"
          message="JSON 解析成功"
          description={
            <div>
              模板: {parsedConfig.name}
              <br />
              模型数: {parsedConfig.models?.length || 0}
              <br />
              指标数: {parsedConfig.metrics?.length || 0}
            </div>
          }
          style={{ marginTop: 12 }}
          showIcon
        />
      )}

      {/* Hidden form fields that get auto-filled from JSON */}
      <div style={{ display: 'none' }}>
        <Form.Item name="name">
          <Input />
        </Form.Item>
        <Form.Item name="bizName">
          <Input />
        </Form.Item>
        <Form.Item name="category" initialValue="OTHER">
          <Input />
        </Form.Item>
        <Form.Item name="description">
          <Input />
        </Form.Item>
      </div>
    </>
  );

  const tabItems = [
    {
      key: 'form',
      label: (
        <span>
          <FormOutlined /> {isEdit ? '基本信息' : '表单创建'}
        </span>
      ),
      children: renderFormMode(),
    },
    {
      key: 'json',
      label: (
        <span>
          <FileTextOutlined /> {isEdit ? 'JSON 配置' : 'JSON 导入'}
        </span>
      ),
      children: renderJsonMode(),
    },
  ];

  return (
    <Modal
      title={isEdit ? '编辑模板' : '创建模板'}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText={isEdit ? '更新' : '创建'}
      cancelText="取消"
      width={800}
      destroyOnClose
    >
      <Form form={form} layout="vertical" name="templateForm">
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
      </Form>
    </Modal>
  );
};

export default TemplateFormModal;

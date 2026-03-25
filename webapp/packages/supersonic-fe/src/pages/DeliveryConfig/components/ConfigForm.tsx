import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Switch, message } from 'antd';
import {
  createConfig,
  updateConfig,
  DeliveryConfig,
  DeliveryType,
  DELIVERY_TYPE_OPTIONS,
} from '@/services/deliveryConfig';

const { TextArea } = Input;

// 默认消息模板
const DEFAULT_TEMPLATES = {
  // 钉钉
  dingtalk_markdown: `### 📊 \${reportName}

**执行时间**: \${executionTime}
**数据量**: \${rowCount} 条

[点击下载报表](\${downloadUrl})`,

  dingtalk_actionCard: `### 📊 \${reportName}

报表已生成，共 \${rowCount} 条数据。

执行时间: \${executionTime}`,

  // 飞书
  feishu_post: `📊 \${reportName}

执行时间: \${executionTime}
数据量: \${rowCount} 条

点击下载: \${downloadUrl}`,

  feishu_interactive: `📊 \${reportName}

报表已生成，共 \${rowCount} 条数据。`,

  // 企业微信
  wechat_text: `【报表推送】\${reportName}
执行时间: \${executionTime}
数据量: \${rowCount} 条
下载链接: \${downloadUrl}`,

  wechat_markdown: `### 📊 <font color="info">\${reportName}</font>

> **执行时间**: \${executionTime}
> **数据量**: \${rowCount} 条

[点击下载报表](\${downloadUrl})`,

  wechat_news: `\${reportName} - \${executionTime}`,
};

interface ConfigFormProps {
  visible: boolean;
  record?: DeliveryConfig;
  onCancel: () => void;
  onSubmit: () => void;
}

const ConfigForm: React.FC<ConfigFormProps> = ({ visible, record, onCancel, onSubmit }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [deliveryType, setDeliveryType] = useState<DeliveryType>('EMAIL');

  useEffect(() => {
    if (visible) {
      if (record) {
        // Edit mode - populate form
        form.setFieldsValue({
          name: record.name,
          deliveryType: record.deliveryType,
          description: record.description,
          enabled: record.enabled,
          deliveryConfig: record.deliveryConfig,
        });
        setDeliveryType(record.deliveryType);

        // Parse and set channel-specific fields
        if (record.deliveryConfig) {
          try {
            const config = JSON.parse(record.deliveryConfig);
            setChannelConfig(record.deliveryType, config);
          } catch (e) {
            // Invalid JSON, leave as is
          }
        }
      } else {
        // Create mode - reset form
        form.resetFields();
        form.setFieldsValue({ enabled: true, deliveryType: 'EMAIL' });
        setDeliveryType('EMAIL');
      }
    }
  }, [visible, record, form]);

  const setChannelConfig = (type: DeliveryType, config: any) => {
    switch (type) {
      case 'EMAIL':
        form.setFieldsValue({
          emailTo: config.to?.join(', '),
          emailCc: config.cc?.join(', '),
          emailSubject: config.subject,
          emailBody: config.body,
        });
        break;
      case 'WEBHOOK':
        form.setFieldsValue({
          webhookUrl: config.url,
          webhookMethod: config.method || 'POST',
          webhookHeaders: config.headers ? JSON.stringify(config.headers, null, 2) : '',
          webhookBodyTemplate: config.bodyTemplate,
        });
        break;
      case 'FEISHU':
        form.setFieldsValue({
          feishuWebhookUrl: config.webhookUrl,
          feishuSecret: config.secret,
          feishuMsgType: config.msgType || 'post',
          feishuTitle: config.title,
          feishuContent: config.content,
          feishuDownloadUrl: config.downloadUrl,
        });
        break;
      case 'DINGTALK':
        form.setFieldsValue({
          dingtalkWebhookUrl: config.webhookUrl,
          dingtalkSecret: config.secret,
          dingtalkMsgType: config.msgType || 'markdown',
          dingtalkTitle: config.title,
          dingtalkContent: config.content,
          dingtalkDownloadUrl: config.downloadUrl,
          dingtalkAtMobiles: config.atMobiles?.join(', '),
        });
        break;
      case 'WECHAT_WORK':
        form.setFieldsValue({
          wechatWorkWebhookUrl: config.webhookUrl,
          wechatWorkMsgType: config.msgType || 'markdown',
          wechatWorkTitle: config.title,
          wechatWorkContent: config.content,
          wechatWorkDownloadUrl: config.downloadUrl,
        });
        break;
    }
  };

  const buildChannelConfig = (type: DeliveryType, values: any): string => {
    let config: any = {};

    switch (type) {
      case 'EMAIL':
        config = {
          to: values.emailTo
            ?.split(',')
            .map((s: string) => s.trim())
            .filter(Boolean),
          cc: values.emailCc
            ?.split(',')
            .map((s: string) => s.trim())
            .filter(Boolean),
          subject: values.emailSubject,
          body: values.emailBody,
        };
        break;
      case 'WEBHOOK':
        config = {
          url: values.webhookUrl,
          method: values.webhookMethod || 'POST',
          headers: values.webhookHeaders ? JSON.parse(values.webhookHeaders) : undefined,
          bodyTemplate: values.webhookBodyTemplate,
        };
        break;
      case 'FEISHU':
        config = {
          webhookUrl: values.feishuWebhookUrl,
          secret: values.feishuSecret,
          msgType: values.feishuMsgType,
          title: values.feishuTitle,
          content: values.feishuContent,
          downloadUrl: values.feishuDownloadUrl,
        };
        break;
      case 'DINGTALK':
        config = {
          webhookUrl: values.dingtalkWebhookUrl,
          secret: values.dingtalkSecret,
          msgType: values.dingtalkMsgType,
          title: values.dingtalkTitle,
          content: values.dingtalkContent,
          downloadUrl: values.dingtalkDownloadUrl,
          atMobiles: values.dingtalkAtMobiles
            ?.split(',')
            .map((s: string) => s.trim())
            .filter(Boolean),
        };
        break;
      case 'WECHAT_WORK':
        config = {
          webhookUrl: values.wechatWorkWebhookUrl,
          msgType: values.wechatWorkMsgType,
          title: values.wechatWorkTitle,
          content: values.wechatWorkContent,
          downloadUrl: values.wechatWorkDownloadUrl,
        };
        break;
    }

    return JSON.stringify(config);
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const deliveryConfig = buildChannelConfig(values.deliveryType, values);

      const payload: Partial<DeliveryConfig> = {
        name: values.name,
        deliveryType: values.deliveryType,
        description: values.description,
        enabled: values.enabled,
        deliveryConfig,
      };

      if (record) {
        await updateConfig(record.id, payload);
        message.success('更新成功');
      } else {
        await createConfig(payload);
        message.success('创建成功');
      }

      onSubmit();
    } catch (error: any) {
      if (error.errorFields) {
        // Validation error
        return;
      }
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  const handleTypeChange = (type: DeliveryType) => {
    setDeliveryType(type);
    // Auto-fill default template for the new channel type
    if (type === 'DINGTALK') {
      const template = DEFAULT_TEMPLATES.dingtalk_markdown;
      form.setFieldsValue({ dingtalkContent: template, dingtalkMsgType: 'markdown' });
    } else if (type === 'FEISHU') {
      const template = DEFAULT_TEMPLATES.feishu_post;
      form.setFieldsValue({ feishuContent: template, feishuMsgType: 'post' });
    } else if (type === 'WECHAT_WORK') {
      const template = DEFAULT_TEMPLATES.wechat_markdown;
      form.setFieldsValue({ wechatWorkContent: template, wechatWorkMsgType: 'markdown' });
    }
  };

  // Get template key based on channel and message type
  const getTemplateKey = (channel: string, msgType: string): string => {
    return `${channel}_${msgType}`;
  };

  // Auto-fill template when message type changes
  const handleMsgTypeChange = (channel: 'dingtalk' | 'feishu' | 'wechat', msgType: string) => {
    const templateKey = getTemplateKey(channel, msgType);
    const template = DEFAULT_TEMPLATES[templateKey as keyof typeof DEFAULT_TEMPLATES];
    if (template) {
      const fieldName = channel === 'wechat' ? 'wechatWorkContent' : `${channel}Content`;
      // Only auto-fill if the current content is empty or matches a default template
      const currentContent = form.getFieldValue(fieldName);
      const isDefaultTemplate = Object.values(DEFAULT_TEMPLATES).includes(currentContent);
      if (!currentContent || isDefaultTemplate) {
        form.setFieldsValue({ [fieldName]: template });
      }
    }
  };

  const renderChannelFields = () => {
    switch (deliveryType) {
      case 'EMAIL':
        return (
          <>
            <Form.Item
              name="emailTo"
              label="收件人"
              rules={[{ required: true, message: '请输入收件人邮箱' }]}
              extra="多个邮箱用逗号分隔"
            >
              <Input placeholder="user1@example.com, user2@example.com" />
            </Form.Item>
            <Form.Item name="emailCc" label="抄送" extra="多个邮箱用逗号分隔">
              <Input placeholder="cc@example.com" />
            </Form.Item>
            <Form.Item
              name="emailSubject"
              label="邮件主题"
              extra="支持变量: ${reportName} 报表名称, ${executionTime} 执行时间"
            >
              <Input placeholder="报表推送: ${reportName} - ${executionTime}" />
            </Form.Item>
            <Form.Item
              name="emailBody"
              label="邮件正文"
              extra="支持变量: ${reportName}, ${executionTime}, ${rowCount}; 报表文件将作为附件发送"
            >
              <TextArea rows={4} placeholder="您好，${reportName} 报表已生成，共 ${rowCount} 条数据，请查收附件。" />
            </Form.Item>
          </>
        );

      case 'WEBHOOK':
        return (
          <>
            <Form.Item
              name="webhookUrl"
              label="Webhook URL"
              rules={[{ required: true, message: '请输入 Webhook URL' }]}
            >
              <Input placeholder="https://your-webhook-endpoint.com/callback" />
            </Form.Item>
            <Form.Item name="webhookMethod" label="HTTP Method" initialValue="POST">
              <Select>
                <Select.Option value="POST">POST</Select.Option>
                <Select.Option value="PUT">PUT</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="webhookHeaders" label="Headers (JSON)">
              <TextArea rows={3} placeholder='{"Authorization": "Bearer xxx"}' />
            </Form.Item>
            <Form.Item name="webhookBodyTemplate" label="Body Template">
              <TextArea
                rows={4}
                placeholder='{"reportName": "${reportName}", "rows": ${rowCount}}'
              />
            </Form.Item>
          </>
        );

      case 'FEISHU':
        return (
          <>
            <Form.Item
              name="feishuWebhookUrl"
              label="飞书 Webhook URL"
              rules={[{ required: true, message: '请输入飞书机器人 Webhook URL' }]}
            >
              <Input placeholder="https://open.feishu.cn/open-apis/bot/v2/hook/xxx" />
            </Form.Item>
            <Form.Item name="feishuSecret" label="签名密钥" extra="可选，用于签名校验">
              <Input.Password placeholder="SECxxx" />
            </Form.Item>
            <Form.Item
              name="feishuMsgType"
              label="消息类型"
              initialValue="post"
              extra="富文本支持标题、链接、文本样式；卡片消息支持按钮交互"
            >
              <Select
                onChange={(value) => handleMsgTypeChange('feishu', value)}
              >
                <Select.Option value="post">富文本</Select.Option>
                <Select.Option value="interactive">卡片消息</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item
              name="feishuTitle"
              label="消息标题"
              extra="支持变量: ${reportName} 报表名称, ${executionTime} 执行时间, ${rowCount} 数据行数"
            >
              <Input placeholder="报表推送: ${reportName}" />
            </Form.Item>
            <Form.Item
              name="feishuContent"
              label="消息内容"
              extra="支持变量: ${reportName}, ${executionTime}, ${rowCount}, ${downloadUrl}"
            >
              <TextArea
                rows={6}
                placeholder="选择消息类型后会自动填充默认模板，可按需修改"
              />
            </Form.Item>
            <Form.Item
              name="feishuDownloadUrl"
              label="下载链接基础 URL"
              extra="用于生成报表下载按钮，完整链接: {baseUrl}?fileId={fileId}"
            >
              <Input placeholder="https://your-domain.com/download" />
            </Form.Item>
          </>
        );

      case 'DINGTALK':
        return (
          <>
            <Form.Item
              name="dingtalkWebhookUrl"
              label="钉钉 Webhook URL"
              rules={[{ required: true, message: '请输入钉钉机器人 Webhook URL' }]}
            >
              <Input placeholder="https://oapi.dingtalk.com/robot/send?access_token=xxx" />
            </Form.Item>
            <Form.Item name="dingtalkSecret" label="签名密钥" extra="可选，用于签名校验">
              <Input.Password placeholder="SECxxx" />
            </Form.Item>
            <Form.Item
              name="dingtalkMsgType"
              label="消息类型"
              initialValue="markdown"
              extra="富文本支持标题、链接等格式；卡片消息支持按钮跳转"
            >
              <Select
                onChange={(value) => handleMsgTypeChange('dingtalk', value)}
              >
                <Select.Option value="markdown">富文本</Select.Option>
                <Select.Option value="actionCard">卡片消息</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item
              name="dingtalkTitle"
              label="消息标题"
              extra="支持变量: ${reportName} 报表名称, ${executionTime} 执行时间, ${rowCount} 数据行数"
            >
              <Input placeholder="报表推送: ${reportName}" />
            </Form.Item>
            <Form.Item
              name="dingtalkContent"
              label="消息内容"
              extra="支持变量: ${reportName}, ${executionTime}, ${rowCount}, ${downloadUrl}"
            >
              <TextArea
                rows={6}
                placeholder="选择消息类型后会自动填充默认模板，可按需修改"
              />
            </Form.Item>
            <Form.Item
              name="dingtalkDownloadUrl"
              label="下载链接基础 URL"
              extra="用于生成报表下载按钮，完整链接: {baseUrl}?fileId={fileId}"
            >
              <Input placeholder="https://your-domain.com/download" />
            </Form.Item>
            <Form.Item name="dingtalkAtMobiles" label="@手机号" extra="多个手机号用逗号分隔，被@的人会收到通知">
              <Input placeholder="13800138000, 13900139000" />
            </Form.Item>
          </>
        );

      case 'WECHAT_WORK':
        return (
          <>
            <Form.Item
              name="wechatWorkWebhookUrl"
              label="企业微信 Webhook URL"
              rules={[{ required: true, message: '请输入企业微信机器人 Webhook URL' }]}
            >
              <Input placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx" />
            </Form.Item>
            <Form.Item
              name="wechatWorkMsgType"
              label="消息类型"
              initialValue="markdown"
              extra="纯文本为简单文字；富文本支持标题、链接、加粗等；图文消息带图片和链接"
            >
              <Select
                onChange={(value) => handleMsgTypeChange('wechat', value)}
              >
                <Select.Option value="text">纯文本</Select.Option>
                <Select.Option value="markdown">富文本</Select.Option>
                <Select.Option value="news">图文消息</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item
              name="wechatWorkTitle"
              label="消息标题"
              extra="支持变量: ${reportName} 报表名称, ${executionTime} 执行时间, ${rowCount} 数据行数"
            >
              <Input placeholder="报表推送: ${reportName}" />
            </Form.Item>
            <Form.Item
              name="wechatWorkContent"
              label="消息内容"
              extra="支持变量: ${reportName}, ${executionTime}, ${rowCount}, ${downloadUrl}"
            >
              <TextArea
                rows={6}
                placeholder="选择消息类型后会自动填充默认模板，可按需修改"
              />
            </Form.Item>
            <Form.Item
              name="wechatWorkDownloadUrl"
              label="下载链接基础 URL"
              extra="用于生成报表下载按钮，完整链接: {baseUrl}?fileId={fileId}"
            >
              <Input placeholder="https://your-domain.com/download" />
            </Form.Item>
          </>
        );

      default:
        return null;
    }
  };

  return (
    <Modal
      title={record ? '编辑推送配置' : '新建推送配置'}
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      width={640}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="配置名称"
          rules={[{ required: true, message: '请输入配置名称' }]}
        >
          <Input placeholder="如: 日报邮件推送" />
        </Form.Item>

        <Form.Item
          name="deliveryType"
          label="渠道类型"
          rules={[{ required: true, message: '请选择渠道类型' }]}
        >
          <Select options={DELIVERY_TYPE_OPTIONS} onChange={handleTypeChange} />
        </Form.Item>

        <Form.Item name="description" label="描述">
          <TextArea rows={2} placeholder="可选描述信息" />
        </Form.Item>

        <Form.Item name="enabled" label="启用" valuePropName="checked">
          <Switch />
        </Form.Item>

        {/* Channel-specific fields */}
        {renderChannelFields()}
      </Form>
    </Modal>
  );
};

export default ConfigForm;

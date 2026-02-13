import React, { useEffect, useState } from 'react';
import {
  Card,
  Row,
  Col,
  Button,
  Tag,
  Space,
  message,
  Empty,
  Popconfirm,
  Dropdown,
  Spin,
} from 'antd';
import {
  RocketOutlined,
  EyeOutlined,
  PlusOutlined,
  HistoryOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { useModel } from '@umijs/max';
import { ROUTE_AUTH_CODES } from '../../../config/routes';
import {
  getTemplateList,
  deleteTemplate,
  SemanticTemplate,
  SemanticPreviewResult,
  SemanticTemplateListResp,
  TEMPLATE_STATUS,
  canEditTemplate,
  canDeleteTemplate,
} from '@/services/semanticTemplate';
import DeployModal from './DeployModal';
import PreviewDrawer from './PreviewDrawer';
import DeployHistory from './DeployHistory';
import TemplateFormModal from './TemplateFormModal';
import styles from './style.less';

const SemanticTemplatePage: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  // SaaS 管理员可以编辑内置模板
  const isSaasAdmin = currentUser?.superAdmin || currentUser?.isAdmin === 1;
  // 权限检查
  const permissions = currentUser?.permissions || [];
  const canDeploy = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_DEPLOY);
  const canCreate = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_CREATE);
  const canUpdate = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_UPDATE);
  const canDeletePerm = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_DELETE);

  const [builtinTemplates, setBuiltinTemplates] = useState<SemanticTemplate[]>([]);
  const [customTemplates, setCustomTemplates] = useState<SemanticTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<SemanticTemplate | null>(null);
  const [deployModalVisible, setDeployModalVisible] = useState(false);
  const [previewDrawerVisible, setPreviewDrawerVisible] = useState(false);
  const [previewData, setPreviewData] = useState<SemanticPreviewResult | null>(null);
  const [historyVisible, setHistoryVisible] = useState(false);
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<SemanticTemplate | null>(null);

  const loadTemplates = async () => {
    setLoading(true);
    try {
      const res: any = await getTemplateList();
      // API response is wrapped in { code, data, msg } format
      if (res?.code === 200 && res?.data) {
        const data = res.data as SemanticTemplateListResp;
        setBuiltinTemplates(data.builtinTemplates || []);
        setCustomTemplates(data.customTemplates || []);
      } else {
        setBuiltinTemplates([]);
        setCustomTemplates([]);
      }
    } catch (error) {
      message.error('加载模板列表失败');
      setBuiltinTemplates([]);
      setCustomTemplates([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTemplates();
  }, []);

  const handleDeploy = (template: SemanticTemplate) => {
    setSelectedTemplate(template);
    setDeployModalVisible(true);
  };

  const handlePreview = (template: SemanticTemplate) => {
    setSelectedTemplate(template);
    const config = template.templateConfig;

    // 从 models 的 measures/dimensions 提取指标和维度预览
    const metrics: any[] = [];
    const dimensions: any[] = [];
    config?.models?.forEach((model: any) => {
      model.measures?.forEach((m: any) => {
        if (m.createMetric !== false) {
          metrics.push({ name: m.name, bizName: m.bizName });
        }
      });
      model.dimensions?.forEach((d: any) => {
        dimensions.push({ name: d.name, bizName: d.bizName, type: d.type });
      });
    });

    const previewResult: SemanticPreviewResult = {
      domain: config?.domain,
      models: config?.models,
      metrics,
      dimensions,
      dataSet: config?.dataSet
        ? {
            name: config.dataSet.name || '',
            bizName: config.dataSet.bizName || '',
            description: config.dataSet.description,
          }
        : undefined,
      agent: config?.agent
        ? {
            name: config.agent.name || '',
            description: config.agent.description,
            examples: config.agent.examples,
          }
        : undefined,
      terms: config?.terms?.map((t: any) => ({
        name: t.name,
        description: t.description,
        alias: t.alias,
      })),
    };
    setPreviewData(previewResult);
    setPreviewDrawerVisible(true);
  };

  const handleEdit = (template: SemanticTemplate) => {
    setEditingTemplate(template);
    setFormModalVisible(true);
  };

  const handleDelete = async (template: SemanticTemplate) => {
    try {
      await deleteTemplate(template.id);
      message.success('模板删除成功');
      loadTemplates();
    } catch (error) {
      message.error('删除模板失败');
    }
  };

  const handleTemplateAction = (key: string, template: SemanticTemplate) => {
    if (key === 'edit') {
      handleEdit(template);
    } else if (key === 'delete') {
      // Will be handled by Popconfirm
    }
  };

  const handleCreate = () => {
    setEditingTemplate(null);
    setFormModalVisible(true);
  };

  const handleFormSuccess = () => {
    setFormModalVisible(false);
    setEditingTemplate(null);
    loadTemplates();
  };

  const handleDeploySuccess = () => {
    setDeployModalVisible(false);
    setSelectedTemplate(null);
    message.success('部署启动成功');
  };

  const renderTemplateCard = (template: SemanticTemplate) => {
    const isDeployed = template.status === TEMPLATE_STATUS.DEPLOYED;
    // 模板状态 + 用户权限 双重检查
    const canEdit = canEditTemplate(template) && canUpdate;
    const canDelete = canDeleteTemplate(template) && canDeletePerm;
    // 构建操作菜单项
    const getMoreMenuItems = () => {
      if (template.isBuiltin) {
        // 内置模板：仅 SaaS 管理员可编辑
        if (isSaasAdmin) {
          return [
            {
              key: 'edit',
              label: '编辑',
              icon: <EditOutlined />,
            },
          ];
        }
        return [];
      }
      // 自定义模板：无编辑和删除权限时不显示菜单
      if (!canUpdate && !canDeletePerm) {
        return [];
      }
      const items: any[] = [];
      if (canUpdate) {
        items.push({
          key: 'edit',
          label: '编辑',
          icon: <EditOutlined />,
          disabled: !canEdit,
        });
      }
      if (canDeletePerm) {
        items.push({
          key: 'delete',
          label: canDelete ? (
            <Popconfirm
              title="确定要删除这个模板吗？"
              onConfirm={() => handleDelete(template)}
            >
              <span style={{ color: '#ff4d4f' }}>删除</span>
            </Popconfirm>
          ) : (
            <span style={{ color: '#999' }}>删除</span>
          ),
          icon: <DeleteOutlined style={{ color: canDelete ? '#ff4d4f' : '#999' }} />,
          disabled: !canDelete,
        });
      }
      return items;
    };

    const moreMenuItems = getMoreMenuItems();

    return (
      <Col xs={24} sm={12} lg={8} xl={6} key={template.id}>
        <Card
          hoverable
          className={styles.templateCard}
          cover={
            template.previewImage ? (
              <img src={template.previewImage} alt={template.name} />
            ) : (
              <div className={styles.cardPlaceholder}>
                <RocketOutlined style={{ fontSize: 48, color: '#1890ff' }} />
              </div>
            )
          }
          actions={[
            canDeploy ? (
              <Button type="link" key="deploy" onClick={() => handleDeploy(template)}>
                <RocketOutlined /> 部署
              </Button>
            ) : (
              <span key="deploy" />
            ),
            !isDeployed ? (
              <Button type="link" key="preview" onClick={() => handlePreview(template)}>
                <EyeOutlined /> 预览
              </Button>
            ) : (
              <span key="preview" />
            ),
            moreMenuItems.length > 0 ? (
              <Dropdown
                key="more"
                menu={{
                  items: moreMenuItems,
                  onClick: ({ key }) => handleTemplateAction(key, template),
                }}
              >
                <Button type="link">
                  <MoreOutlined />
                </Button>
              </Dropdown>
            ) : (
              <span key="spacer" />
            ),
          ]}
        >
          <Card.Meta
            title={
              <Space>
                {template.name}
                {template.isBuiltin ? (
                  <Tag color="blue">内置</Tag>
                ) : isDeployed ? (
                  <Tag color="green">已部署</Tag>
                ) : (
                  <Tag color="orange">草稿</Tag>
                )}
              </Space>
            }
            description={template.description || '暂无描述'}
          />
          <div className={styles.templateType}>
            <Tag>{template.category}</Tag>
          </div>
        </Card>
      </Col>
    );
  };

  return (
    <div className={styles.container}>
      {/* 页面顶部操作栏 - 全局操作 */}
      <div className={styles.pageHeader}>
        {canDeploy && (
          <Button onClick={() => setHistoryVisible(true)}>
            <HistoryOutlined /> 部署历史
          </Button>
        )}
      </div>

      <Spin spinning={loading}>
        {/* 内置模板 */}
        <Card
          title="内置模板"
          className={styles.sectionCard}
          extra={
            <span className={styles.sectionTip}>
              系统预置的Demo模板，所有租户可用
            </span>
          }
        >
          {builtinTemplates.length > 0 ? (
            <Row gutter={[16, 16]}>{builtinTemplates.map(renderTemplateCard)}</Row>
          ) : (
            <Empty description="暂无内置模板" />
          )}
        </Card>

        {/* 自定义模板 */}
        <Card
          title="自定义模板"
          className={styles.sectionCard}
          extra={
            <Space>
              <span className={styles.sectionTip}>自定义的模板</span>
              {canCreate && (
                <Button type="primary" onClick={handleCreate}>
                  <PlusOutlined /> 新建模板
                </Button>
              )}
            </Space>
          }
        >
          {customTemplates.length > 0 ? (
            <Row gutter={[16, 16]}>{customTemplates.map(renderTemplateCard)}</Row>
          ) : (
            <Empty description="暂无自定义模板，点击「新建模板」创建" />
          )}
        </Card>
      </Spin>

      {/* Deploy Modal */}
      <DeployModal
        visible={deployModalVisible}
        template={selectedTemplate}
        hasExistingDeployment={selectedTemplate?.status === TEMPLATE_STATUS.DEPLOYED}
        onCancel={() => setDeployModalVisible(false)}
        onSuccess={handleDeploySuccess}
        onPreviewResult={(data) => {
          setPreviewData(data);
          setPreviewDrawerVisible(true);
        }}
      />

      {/* Preview Drawer */}
      <PreviewDrawer
        visible={previewDrawerVisible}
        data={previewData}
        onClose={() => setPreviewDrawerVisible(false)}
      />

      {/* Deployment History */}
      <DeployHistory visible={historyVisible} onClose={() => setHistoryVisible(false)} />

      {/* Template Form Modal */}
      <TemplateFormModal
        visible={formModalVisible}
        template={editingTemplate}
        onCancel={() => {
          setFormModalVisible(false);
          setEditingTemplate(null);
        }}
        onSuccess={handleFormSuccess}
      />
    </div>
  );
};

export default SemanticTemplatePage;

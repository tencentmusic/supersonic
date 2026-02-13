-- Headless module data (H2)


-- ========================================
-- 7. 用户-角色关联初始化
-- ========================================
MERGE INTO s2_user_role (user_id, role_id, created_at, created_by) KEY(user_id, role_id) VALUES
(1, 1, CURRENT_TIMESTAMP, 'system'),  -- admin -> 系统管理员 (租户级)
(1, 6, CURRENT_TIMESTAMP, 'system'),  -- admin -> 平台超级管理员 (平台级)
(2, 2, CURRENT_TIMESTAMP, 'system'),  -- jack -> 分析师
(3, 3, CURRENT_TIMESTAMP, 'system'),  -- tom -> 查看者
(4, 4, CURRENT_TIMESTAMP, 'system'),  -- lucy -> 租户管理员
(5, 2, CURRENT_TIMESTAMP, 'system');  -- alice -> 分析师

-- ========================================
-- 8. 可用日期信息初始化
-- ========================================
MERGE INTO s2_available_date_info (id, item_id, type, date_format, start_date, end_date, unavailable_date, status, created_at, created_by, updated_at, updated_by)
KEY(id) VALUES
(1, 1, 'dimension', 'yyyy-MM-dd', FORMATDATETIME(DATEADD('DAY', -28, CURRENT_DATE), 'yyyy-MM-dd'), FORMATDATETIME(DATEADD('DAY', -1, CURRENT_DATE), 'yyyy-MM-dd'), '[]', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin'),
(2, 2, 'dimension', 'yyyy-MM-dd', FORMATDATETIME(DATEADD('DAY', -28, CURRENT_DATE), 'yyyy-MM-dd'), FORMATDATETIME(DATEADD('DAY', -1, CURRENT_DATE), 'yyyy-MM-dd'), '[]', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin'),
(3, 3, 'dimension', 'yyyy-MM-dd', FORMATDATETIME(DATEADD('DAY', -28, CURRENT_DATE), 'yyyy-MM-dd'), FORMATDATETIME(DATEADD('DAY', -1, CURRENT_DATE), 'yyyy-MM-dd'), '[]', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin');


-- ========================================
-- 9. 画布配置初始化
-- ========================================
MERGE INTO s2_canvas (id, domain_id, type, config, created_at, created_by, updated_at, updated_by)
KEY(id) VALUES
(1, 1, 'modelEdgeRelation', '[{"source":"datasource-1","target":"datasource-3","type":"polyline","id":"edge-0.305251275235679741702883718912","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-94,"y":-137.5,"anchorIndex":0,"id":"-94|||-137.5"},"endPoint":{"x":-234,"y":-45,"anchorIndex":1,"id":"-234|||-45"},"sourceAnchor":2,"targetAnchor":1,"label":"模型关系编辑"},{"source":"datasource-1","target":"datasource-2","type":"polyline","id":"edge-0.466237264629309141702883756359","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-12,"y":-137.5,"anchorIndex":1,"id":"-12|||-137.5"},"endPoint":{"x":85,"y":31.5,"anchorIndex":0,"id":"85|||31.5"},"sourceAnchor":1,"targetAnchor":2,"label":"模型关系编辑"}]', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin');

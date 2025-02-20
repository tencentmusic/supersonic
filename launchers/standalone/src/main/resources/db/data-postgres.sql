-- sample user
-- The default value for the password is 123456
insert into s2_user ("name", password, salt, display_name, email, is_admin) values ('admin','c3VwZXJzb25pY0BiaWNvbdktJJYWw6A3rEmBUPzbn/6DNeYnD+y3mAwDKEMS3KVT','jGl25bVBBBW96Qi9Te4V3w==','admin','admin@xx.com', 1);
insert into s2_user ("name", password, salt,  display_name, email) values ('jack','c3VwZXJzb25pY0BiaWNvbWxGalmwa0h/trkh/3CWOYMDiku0Op1VmOfESIKmN0HG','MWERWefm/3hD6kYndF6JIg==','jack','jack@xx.com');
insert into s2_user ("name", password, salt,  display_name, email) values ('tom','c3VwZXJzb25pY0BiaWNvbVWv0CZ6HzeX8GRUpw0C8NSaQ+0hE/dAcmzRpCFwAqxK','4WCPdcXXgT89QDHLML+3hg==','tom','tom@xx.com');
insert into s2_user ("name", password, salt,  display_name, email, is_admin) values ('lucy','c3VwZXJzb25pY0BiaWNvbc7Ychfu99lPL7rLmCkf/vgF4RASa4Z++Mxo1qlDCpci','3Jnpqob6uDoGLP9eCAg5Fw==','lucy','lucy@xx.com', 1);
insert into s2_user ("name", password, salt,  display_name, email) values ('alice','c3VwZXJzb25pY0BiaWNvbe9Z4F2/DVIfAJoN1HwUTuH1KgVuiusvfh7KkWYQSNHk','K9gGyX8OAK8aH8Myj6djqQ==','alice','alice@xx.com');


INSERT INTO s2_available_date_info (item_id, type, date_format, start_date, end_date, unavailable_date, created_at, created_by, updated_at, updated_by)
VALUES (1, 'dimension', 'yyyy-MM-dd', CURRENT_DATE - INTERVAL '28 days', CURRENT_DATE - INTERVAL '1 day', '[]', '2023-06-01', 'admin', '2023-06-01', 'admin');

INSERT INTO s2_available_date_info (item_id, type, date_format, start_date, end_date, unavailable_date, created_at, created_by, updated_at, updated_by)
VALUES (2, 'dimension', 'yyyy-MM-dd', CURRENT_DATE - INTERVAL '28 days', CURRENT_DATE - INTERVAL '1 day', '[]', '2023-06-01', 'admin', '2023-06-01', 'admin');

INSERT INTO s2_available_date_info (item_id, type, date_format, start_date, end_date, unavailable_date, created_at, created_by, updated_at, updated_by)
VALUES (3, 'dimension', 'yyyy-MM-dd', CURRENT_DATE - INTERVAL '28 days', CURRENT_DATE - INTERVAL '1 day', '[]', '2023-06-01', 'admin', '2023-06-01', 'admin');

insert into s2_canvas("id", "domain_id", "type", "config" ,"created_at"  ,"created_by"  ,"updated_at"  ,"updated_by" )
values (1, 1, 'modelEdgeRelation', '[{"source":"datasource-1","target":"datasource-3","type":"polyline","id":"edge-0.305251275235679741702883718912","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-94,"y":-137.5,"anchorIndex":0,"id":"-94|||-137.5"},"endPoint":{"x":-234,"y":-45,"anchorIndex":1,"id":"-234|||-45"},"sourceAnchor":2,"targetAnchor":1,"label":"模型关系编辑"},{"source":"datasource-1","target":"datasource-2","type":"polyline","id":"edge-0.466237264629309141702883756359","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-12,"y":-137.5,"anchorIndex":1,"id":"-12|||-137.5"},"endPoint":{"x":85,"y":31.5,"anchorIndex":0,"id":"85|||31.5"},"sourceAnchor":1,"targetAnchor":2,"label":"模型关系编辑"}]', '2023-06-01', 'admin', '2023-06-01', 'admin');

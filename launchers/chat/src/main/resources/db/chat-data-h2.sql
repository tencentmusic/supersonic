insert into s2_chat_context (chat_id, modified_at , `user`, `query_text`, `semantic_parse` ,ext_data) VALUES(1, '2023-05-24 00:00:00', 'admin', '超音数访问次数', '', 'admin');

insert into s2_user (id, `name`, password, display_name, email) values (1, 'admin','admin','admin','admin@xx.com');
insert into s2_user (id, `name`, password, display_name, email) values (2, 'jack','123456','jack','jack@xx.com');
insert into s2_user (id, `name`, password, display_name, email) values (3, 'tom','123456','tom','tom@xx.com');
insert into s2_user (id, `name`, password, display_name, email) values (4, 'lucy','123456','lucy','lucy@xx.com');

insert into s2_chat_config (id, domain_id, default_metrics, visibility, entity_info, dictionary_info, created_at, updated_at, created_by, updated_by, status) values (2, 1, '[{"metricId":1,"unit":7,"period":"DAY"}]','{"blackDimIdList":[],"blackMetricIdList":[]}','{"entityIds":[2],"names":["用户","用户姓名"],"detailData":{"dimensionIds":[1,2],"metricIds":[2]}}','[{"itemId":1,"type":"DIMENSION","blackList":[],"isDictInfo":true},{"itemId":2,"type":"DIMENSION","blackList":[],"isDictInfo":true},{"itemId":3,"type":"DIMENSION","blackList":[],"isDictInfo":true}]','2023-05-24 18:00:00', '2023-05-25 11:00:00', 'admin', 'admin', 1 );
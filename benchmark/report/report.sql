-- 查询create_time为今天的数据
select * from s2_chat_parse where create_time > current_date;

-- 解析成功：查询create_time为今天的数据,并且执行成功的数据,json格式字段parse_info不为空且sqlInfo不为空
select * from s2_chat_parse where create_time > current_date and parse_info is not null and json_extract(parse_info,'$.sqlInfo') is not null;

-- 执行成功：查新s2_chat_query表中create_time为今天的数据,并且执行成功的数据
select * from s2_chat_query where create_time > current_date and query_state = 1 and query_state = 1;

-- 执行失败：查新s2_chat_query表中create_time为今天的数据,并且执行失败的数据
select * from s2_chat_query where create_time > current_date and query_state = 1 and query_state is null or query_state = 0;

-- 查询s2_chat_query表中create_time为今天的数据,并且执行成功的数据,并且查询的数据不为空
select * from s2_chat_query where create_time > current_date and query_state = 1 and query_state = 1 and query_result is not null;


-- 查看今日批量测试解析成功的数据
select question_id,chat_id,create_time,query_text,
       JSON_EXTRACT(parse_info,'$.sqlInfo.s2SQL') as s2sql,
       JSON_EXTRACT(parse_info,'$.sqlInfo.correctS2SQL') as correctS2SQL,
       JSON_EXTRACT(parse_info,'$.sqlInfo.querySQL') as querySQL,
       '请标记正确的SQL' as correctSQL,
       '请标记生成SQL是否正确' as isOk,
       '请分类不正确的原因' as reason
from s2_chat_parse scp where user_name = 'zhaodongsheng' and chat_id = '141' and create_time > current_date;
-- and parse_info is not null and JSON_EXTRACT(parse_info,'$.sqlInfo') is not null;

-- 查看今日批量测试执行成功的数据
select question_id,chat_id,create_time,query_text,
       JSON_EXTRACT(query_result,'$.querySql') as querySql,
       JSON_EXTRACT(query_result,'$.queryResults') as queryResults
from s2_chat_query where user_name = 'zhaodongsheng' and chat_id = '141' and query_state = 1 and create_time > current_date;

-- 查看今日批量测试执行失败的数据
select question_id,chat_id,create_time,query_text,
       JSON_EXTRACT(query_result,'$.querySql') as querySql,
       JSON_EXTRACT(query_result,'$.queryResults') as queryResults
from s2_chat_query where user_name = 'zhaodongsheng' and chat_id = '141' and query_state is null and create_time > current_date;

-- 查看今日批量测试执行失败的数据
select * from s2_chat_query where user_name = 'zhaodongsheng' and chat_id = '141' and query_state is null and create_time > current_date;


-- 统计每个agent的平均解析时间
select t.*,sa.name from (
                            select
                                agent_id,
                                max(JSON_EXTRACT(parse_time_cost, '$.parseTime')) as 最大时间,
                                min(JSON_EXTRACT(parse_time_cost, '$.parseTime')) as 最小时间,
                                avg(JSON_EXTRACT(parse_time_cost, '$.parseTime')) as 平均时间,
                                count(*) as 测试次数
                            from s2_chat_query where parse_time_cost is  not null
                            GROUP BY agent_id
                        )t left join s2_agent  sa on t.agent_id = sa.id
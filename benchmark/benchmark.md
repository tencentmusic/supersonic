## 使用场景
产品上线阶段批量测试问答对话的问题，统计测试结果。
注意：与evaluation模块的区别，evaluation是构建数据集多个模型的横向评估，benchmark是选定模型下，批量自动化业务问题的测试。
## 功能说明
批量自动化测试问答对话测试，支持单轮问答测试。

## 使用说明
注意：建议在开发测试环境的执行，如果需要在生产环境的测试，请避开用户使用高峰期。
1. 准备测试问题

将问题写入`test_data.csv`文件，格式如下：
```csv
question
各BG期间在职、入职、离职人员的平均薪资是多少？（注意：薪资不包括香港视源、广视以及并购控股子公司青松、仙视的数据。）
各BG期间入职且仍在职的人数有多少？
各BG当月的净增长人数及其增长率是多少？
```
将文件放入`benchmark/data`目录下。

2. 执行测试
```bash
python benchmark -u http://localhost:3100 -a 6 -c 141 -f data/renli.csv -p zds
```
参数说明：
- -a: 问答对话的id
- -c: chat_id
- -f: 测试问题文件
- -u: 用户id
如果执行报错，没有安装相关python包，可以执行`pip install -r requirements.txt`安装相关包。

3. 查看测试结果
当前，只能在数据库中查看测试结果。
```sql
select question_id,chat_id,create_time,query_text,
       JSON_EXTRACT(parse_info,'$.sqlInfo.s2SQL') as s2sql,
       JSON_EXTRACT(parse_info,'$.sqlInfo.correctS2SQL') as correctS2SQL,
       JSON_EXTRACT(parse_info,'$.sqlInfo.querySQL') as querySQL,
       '请标记正确的SQL' as correctSQL,
       '请标记生成SQL是否正确' as isOk,
       '请分类不正确的原因' as reason
from s2_chat_parse scp where user_name = 'zhaodongsheng' and chat_id = '141';

select question_id,chat_id,create_time,query_text,
       JSON_EXTRACT(query_result,'$.querySql') as querySql,
       JSON_EXTRACT(query_result,'$.queryResults') as queryResults
from s2_chat_query where user_name = 'zhaodongsheng' and chat_id = '141' and query_state = 1;

```
4. 查看帮助
```bash
python benchmark.py --help
usage: benchmark.py [-h] -u URL -a AGENTID -c CHATID -f FILEPATH -p USERNAME

optional arguments:
  -h, --help            show this help message and exit
  -u URL, --url URL     url:问答系统url,例如：https://chatdata-dev.test.com
  -a AGENTID, --agentId AGENTID
                        agentId：助手ID
  -c CHATID, --chatId CHATID
                        chatId:会话ID,需要通过浏览器开发者模式获取
  -f FILEPATH, --filePath FILEPATH
                        filePath：问题文件路径, csv格式. 请提前上传到benchmark/data目录下
  -p USERNAME, --userName USERNAME
                        userName：用户名，用户获取登录token
```

## 演示效果
```bash
python benchmark.py -u https://chatdata-dev.test.com -a 3 -c 35 -f data/shuce.csv -p zds
批量测试配置信息[url: https://chatdata-dev.test.com agentId: 3 chatId: 35 filePath: data/shuce.csv userName: zds ]
请确认输入的压力测试信息是否正确:
1. Yes
2. No
1
start to ask question: 各BG期间在职、入职、离职人员的平均薪资是多少？（注意：薪资不包括香港视源、广视以及并购控股子公司青松、仙视的数据。）
start to ask question: 各BG期间入职且仍在职的人数有多少？
start to ask question: 各BG当月的净增长人数及其增长率是多少？
```

## TODO
- [x] 问答对话测试
- [ ] 多轮对话测试
- [ ] 问答对话测试结果展示
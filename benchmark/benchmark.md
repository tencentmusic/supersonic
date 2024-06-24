## 功能说明
批量自动化测试问答对话测试，支持单轮问答测试。

## 使用说明
注意：代码中写上了开发测试环境的地址，如果需要修改为生产环境的地址。
1. 准测试问题 
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
python benchmark -a 6 -c 141 -f data/renli.csv -u zhaodongsheng
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
select * from s2_chat_parse scp where user_name = 'zhaodongsheng' and chat_id = '141';
select * from s2_chat_query where user_id = 'zhaodongsheng' and chat_id = '141';

```
4. 查看错误case

## 演示效果
```bash
python benchmark.py -a 6 -c 141 -f data/renli.csv -u zhaodongsheng
压力测试配置信息[agentId: 6 chatId: 141 file_path: data/renli.csv user_name: zhaodongsheng ]
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
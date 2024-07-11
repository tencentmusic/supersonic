[English](README.md) | [日本語版](README_JP.md) | [文档中心](https://supersonicbi.github.io/)

# SuperSonic

**SuperSonic融合Chat BI（powered by LLM）和Headless BI（powered by 语义层）打造新一代的BI平台**。这种融合确保了Chat BI能够与传统BI一样访问统一化治理的语义数据模型。此外，两种BI新范式都从中获得收益：

- Chat BI的Text2SQL生成通过检索语义数据模型得到增强。
- Headless BI的查询接口通过支持自然语言API得到拓展。

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_ideas.png" height="75%" width="75%" />

通过SuperSonic的问答对话界面，用户能够使用自然语言查询数据，系统会选择合适的可视化图表呈现结果。SuperSonic不需要修改或复制数据，只需要在物理数据模型之上构建逻辑语义模型（定义指标/维度/实体/标签，以及它们的业务含义、相互关系等），即可开启数据问答体验。与此同时，SuperSonic被设计为可插拔的框架，采用Java SPI机制来扩展定制功能。

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_demo.gif" height="100%" width="100%" />

## 项目动机

大型语言模型（LLM）如ChatGPT的出现正在重塑信息检索的方式，引领数据分析领域的一种新范式，被称为Chat BI。为了实现Chat BI，学术界和工业界主要关注利用LLM的能力将自然语言转换为SQL，通常称为Text2SQL或NL2SQL。尽管一些方法显示出有希望的结果，但它们在大规模实际应用中的可靠性还不足。

与此同时，另一种新兴范式被称为Headless BI，它专注于构建统一的语义数据模型，并引起了广泛的关注。Headless BI通过一个通用的语义层来实现，通过开放的API公开一致的数据语义。

从我们的角度来看，Chat BI和Headless BI的融合有潜力在两个方面增强Text2SQL的能力：

1. 将数据语义（如业务术语、列值等）纳入提示词中，使LLM能够更好地理解语义，以**减少幻觉**。
2. 将高级SQL语法（如连接、公式等）的生成从LLM卸载到语义层，以**减少复杂度**。

为了验证上述想法，我们开发了SuperSonic项目，并将其应用在实际的内部产品中。与此同时，我们将SuperSonic作为一个可扩展的框架开源，希望能够促进数据问答对话领域的进一步发展。

## 开箱即用的特性

- 内置Chat BI界面以便*业务用户*输入数据查询。
- 内置Headless BI界面以便*分析工程师*构建语义模型。
- 内置基于规则的语义解析器，在特定场景（比如DEMO演示、集成测试）可以提升推理效率。
- 支持文本输入联想、多轮对话、查询后问题推荐等高级特征。
- 支持三级权限控制：数据集级、列级、行级。

## 易于扩展的组件

SuperSonic的整体架构和主流程如下图所示：

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_components.png" height="65%" width="65%" /> 

- **模型知识库(Knowledge Base)：** 定期从语义模型中提取相关的模式信息，构建词典和索引，以便后续的模式映射。

- **模式映射器(Schema Mapper)：** 将自然语言文本在知识库中进行匹配，为后续的语义解析提供相关信息。

- **语义解析器(Semantic Parser)：** 理解用户查询并抽取语义信息，生成语义查询语句S2SQL。

- **语义修正器(Semantic Corrector)：** 检查语义查询语句的合法性，对不合法的信息做修正和优化处理。

- **语义翻译器(Semantic Translator)：** 将语义查询语句翻译成可在物理数据模型上执行的SQL语句。

- **问答插件(Chat Plugin)：** 通过第三方工具扩展功能。给定所有配置的插件及其功能描述和示例问题，大语言模型将选择最合适的插件。

- **问答记忆(Chat Memory)：** 将历史的查询轨迹进行封装，可被召回作为few-shot样例嵌入提示词。

## 快速体验

### 线上环境体验
访问http://117.72.46.148:9080 注册新用户体验. 请勿修改系统配置。我们每周末定期重启重置配置。

### Docker部署
- 安装好Docker以及docker-compose
- 下载docker-compose.yml；执行命令：wget https://raw.githubusercontent.com/tencentmusic/supersonic/master/docker/docker-compose.yml
- 执行："docker-compose up -d"
- 在浏览器访问http://localhost:9080 开启探索

### 本地构建

SuperSonic自带样例的语义模型和问答对话，只需以下三步即可快速体验：

- 从[release page](https://github.com/tencentmusic/supersonic/releases)下载预先构建好的发行包
- 运行 "assembly/bin/supersonic-daemon.sh start"启动standalone模式的Java服务
- 在浏览器访问http://localhost:9080 开启探索

## 如何构建和部署

请参考项目[文档](https://supersonicbi.github.io/docs/%E7%B3%BB%E7%BB%9F%E9%83%A8%E7%BD%B2/%E7%BC%96%E8%AF%91%E6%9E%84%E5%BB%BA/)。

## 微信联系方式

欢迎关注微信公众号：

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_wechat_oa.png" height="50%" width="50%" />

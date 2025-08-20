# SuperSonic Changelog

- All notable changes to this project will be documented in this file.
- "Breaking Changes" describes any changes that may break existing functionality or cause
  compatibility issues with previous versions.
## SuperSonic [1.0.0] - 2025-08-05

### 重大特性变更 / Major Features

#### 多数据库支持扩展 / Multi-Database Support
- **Oracle数据库支持**: 新增Oracle数据库引擎类型及适配器 ([8eeed87ba](https://github.com/tencentmusic/supersonic/commit/8eeed87ba) by supersonicbi)
- **StarRocks支持**: 支持StarRocks和多catalog功能 ([33268bf3d](https://github.com/tencentmusic/supersonic/commit/33268bf3d) by zyclove)
- **SAP HANA支持**: 新增SAP HANA数据库适配支持 ([2e28a4c7a](https://github.com/tencentmusic/supersonic/commit/2e28a4c7a) by wwsheng009)
- **DuckDB支持**: 支持DuckDB数据库 ([a058dc8b6](https://github.com/tencentmusic/supersonic/commit/a058dc8b6) by jerryjzhang)
- **Kyuubi支持**: 支持Kyuubi Presto Trino ([5e3bafb95](https://github.com/tencentmusic/supersonic/commit/5e3bafb95) by zyclove)
- **OpenSearch支持**: 新增OpenSearch支持 ([d942d35c9](https://github.com/tencentmusic/supersonic/commit/d942d35c9) by zyclove)

#### 智能问答增强 / AI-Enhanced Query Processing
- **LLM纠错器**: 新增LLM物理SQL纠错器 ([f899d23b6](https://github.com/tencentmusic/supersonic/commit/f899d23b6) by 柯慕灵)
- **记忆管理**: Agent记忆管理启用few-shot优先机制 ([fae9118c2](https://github.com/tencentmusic/supersonic/commit/fae9118c2) by feelshana)
- **结构化查询**: 支持struct查询中的offset子句 ([d2a43a99c](https://github.com/tencentmusic/supersonic/commit/d2a43a99c) by jerryjzhang)
- **向量召回优化**: 优化嵌入向量召回机制 ([8c6ae6252](https://github.com/tencentmusic/supersonic/commit/8c6ae6252) by lexluo09)

#### 权限管理系统 / Permission Management
- **Agent权限**: 支持agent级别的权限管理 ([b5aa6e046](https://github.com/tencentmusic/supersonic/commit/b5aa6e046) by jerryjzhang)
- **用户管理**: 支持用户删除功能 ([1c9cf788c](https://github.com/tencentmusic/supersonic/commit/1c9cf788c) by supersonicbi)
- **鉴权优化**: 全面优化鉴权与召回机制 ([1faf84e37](https://github.com/tencentmusic/supersonic/commit/1faf84e37), [7e6639df8](https://github.com/tencentmusic/supersonic/commit/7e6639df8) by guilinlewis)

### 架构升级 / Architecture Upgrades

#### 核心框架升级 / Core Framework Upgrades
- **SpringBoot 3升级**: 完成SpringBoot 3.x升级 ([07f6be51c](https://github.com/tencentmusic/supersonic/commit/07f6be51c) by mislayming)
- **依赖升级**: 升级依赖包并修复安全漏洞 ([232a20227](https://github.com/tencentmusic/supersonic/commit/232a20227) by beat4ocean)
- **LangChain4j更新**: 替换已废弃的LangChain4j APIs ([acffc03c7](https://github.com/tencentmusic/supersonic/commit/acffc03c7) by beat4ocean)
- **Swagger升级**: 使用SpringDoc支持Swagger在Spring 3.x ([758d170bb](https://github.com/tencentmusic/supersonic/commit/758d170bb) by jerryjzhang)

#### 许可证变更 / License Changes
- **Apache 2.0**: 从MIT更改为Apache 2.0许可证 ([0aa002882](https://github.com/tencentmusic/supersonic/commit/0aa002882) by jerryjzhang)

### 性能优化 / Performance Improvements

#### 系统性能 / System Performance
- **GC优化**: 实现Generational ZGC ([3fc1ec42b](https://github.com/tencentmusic/supersonic/commit/3fc1ec42b) by beat4ocean)
- **Docker优化**: 减少Docker镜像体积 ([614917ba7](https://github.com/tencentmusic/supersonic/commit/614917ba7) by kino)
- **并行处理**: 嵌入向量并行执行优化 ([8c6ae6252](https://github.com/tencentmusic/supersonic/commit/8c6ae6252) by lexluo09)
- **记忆评估**: 记忆评估性能优化 ([524ec38ed](https://github.com/tencentmusic/supersonic/commit/524ec38ed) by yudong)
- **多平台构建**: 支持Docker多平台构建 ([da6d28c18](https://github.com/tencentmusic/supersonic/commit/da6d28c18) by jerryjzhang)

#### 数据处理优化 / Data Processing Optimization
- **日期格式**: 支持更多日期字符串格式 ([2b13866c0](https://github.com/tencentmusic/supersonic/commit/2b13866c0) by supersonicbi)
- **SQL优化**: 优化SQL生成和执行性能 ([0ab764329](https://github.com/tencentmusic/supersonic/commit/0ab764329) by jerryjzhang)
- **模型关联**: 优化模型关联查询性能 ([47c2595fb](https://github.com/tencentmusic/supersonic/commit/47c2595fb) by Willy-J)

### 功能增强 / Feature Enhancements

#### 前端界面优化 / Frontend Improvements
- **图表导出**: 消息支持导出图表图片 ([ce9ae1c0c](https://github.com/tencentmusic/supersonic/commit/ce9ae1c0c) by pisces)
- **路由重构**: 重构语义建模路由交互 ([82c63a7f2](https://github.com/tencentmusic/supersonic/commit/82c63a7f2) by tristanliu)
- **权限界面**: 统一助理权限设置交互界面 ([46d64d78f](https://github.com/tencentmusic/supersonic/commit/46d64d78f) by tristanliu)
- **图表优化**: 优化ChatMsg图表条件 ([06fb6ba74](https://github.com/tencentmusic/supersonic/commit/06fb6ba74) by FredTsang)
- **数据格式**: 提取formatByDataFormatType()方法 ([9ffdba956](https://github.com/tencentmusic/supersonic/commit/9ffdba956) by FredTsang)

#### 开发体验 / Developer Experience
- **构建脚本**: 优化Web应用构建脚本 ([baae7f74b](https://github.com/tencentmusic/supersonic/commit/baae7f74b) by zyclove)
- **GitHub Actions**: 优化GitHub Actions镜像推送 ([6a4458a57](https://github.com/tencentmusic/supersonic/commit/6a4458a57) by lexluo09)
- **基准测试**: 改进基准测试，增加解析结果分析 ([97710a90c](https://github.com/tencentmusic/supersonic/commit/97710a90c) by Antgeek)

### Bug修复 / Bug Fixes

#### 核心功能修复 / Core Function Fixes
- **插件功能**: 修复插件功能无法调用/结果被NL2SQL覆盖问题 ([c75233e37](https://github.com/tencentmusic/supersonic/commit/c75233e37) by QJ_wonder)
- **维度别名**: 修复映射阶段维度值别名不生效问题 ([785bda6cd](https://github.com/tencentmusic/supersonic/commit/785bda6cd) by feelshana)
- **模型字段**: 修复模型字段更新问题 ([6bd897084](https://github.com/tencentmusic/supersonic/commit/6bd897084) by WDEP)
- **多轮对话**: 修复headless中字段查询及多轮对话使用问题 ([be0447ae1](https://github.com/tencentmusic/supersonic/commit/be0447ae1) by QJ_wonder)

#### NPE异常修复 / NPE Exception Fixes
- **聊天查询**: 修复EmbeddingMatchStrategy.detectByBatch() NPE异常 ([6d907b6ad](https://github.com/tencentmusic/supersonic/commit/6d907b6ad) by wangyong)
- **文件处理**: 修复FileHandlerImpl.convert2Resp() 维度值数据行首字符为空格异常 ([da172a030](https://github.com/tencentmusic/supersonic/commit/da172a030) by wangyong)
- **头部服务**: 修复多处headless NPE问题 ([79a44b27e](https://github.com/tencentmusic/supersonic/commit/79a44b27e) by jerryjzhang)
- **解析信息**: 修复getParseInfo中的NPE ([dce9a8a58](https://github.com/tencentmusic/supersonic/commit/dce9a8a58) by supersonicbi)

#### SQL兼容性修复 / SQL Compatibility Fixes
- **SQL处理**: 修复SQL前后换行符导致的语句结尾";"删除问题 ([55ac3d1aa](https://github.com/tencentmusic/supersonic/commit/55ac3d1aa) by wangyong)
- **查询别名**: DictUtils.constructQuerySqlReq针对sql query增加别名 ([042791762](https://github.com/tencentmusic/supersonic/commit/042791762) by andybj0228)
- **SQL变量**: 支持SQL脚本变量替换 ([0709575cd](https://github.com/tencentmusic/supersonic/commit/0709575cd) by wanglongqiang)

#### 前端Bug修复 / Frontend Bug Fixes
- **UI样式**: 修复问答对话右侧历史对话模块样式异常 ([c33a85b58](https://github.com/tencentmusic/supersonic/commit/c33a85b58) by wangyong)
- **推荐维度**: 修复页面不显示推荐下钻维度问题 ([62b9db679](https://github.com/tencentmusic/supersonic/commit/62b9db679) by WDEP)
- **图表显示**: 修复饼图显示条件问题 ([1b8cd7f0d](https://github.com/tencentmusic/supersonic/commit/1b8cd7f0d) by WDEP)
- **负数支持**: 支持负数显示 ([2552e2ae4](https://github.com/tencentmusic/supersonic/commit/2552e2ae4) by FredTsang)
- **百分比显示**: 支持bar图needMultiply100显示正确百分比值 ([8abfc923a](https://github.com/tencentmusic/supersonic/commit/8abfc923a) by coosir)
- **TypeScript错误**: 修复前端TypeScript错误 ([5585b9e22](https://github.com/tencentmusic/supersonic/commit/5585b9e22) by poncheen)

#### 系统兼容性修复 / System Compatibility Fixes
- **Windows脚本**: 修复Windows daemon.bat路径配置问题 ([e5a41765b](https://github.com/tencentmusic/supersonic/commit/e5a41765b) by 柯慕灵)
- **字符编码**: 将utf8编码修改为utf8mb4,解决字符问题 ([2e81b190a](https://github.com/tencentmusic/supersonic/commit/2e81b190a) by Kun Gu)
- **记忆缓存**: 修复记忆管理中因缓存无法存储的问题 ([81cd60d2d](https://github.com/tencentmusic/supersonic/commit/81cd60d2d) by guilinlewis)
- **Mac兼容**: 降级djl库以支持Mac Intel机器 ([bf3213e8f](https://github.com/tencentmusic/supersonic/commit/bf3213e8f) by jerryjzhang)

### 数据管理优化 / Data Management Improvements

#### 维度指标管理 / Dimension & Metric Management
- **维度检索**: 修复维度和指标检索及百分比显示问题 ([d8fe2ed2b](https://github.com/tencentmusic/supersonic/commit/d8fe2ed2b) by 木鱼和尚)
- **查询导出**: 基于queryColumns导出数据 ([11d1264d3](https://github.com/tencentmusic/supersonic/commit/11d1264d3) by FredTsang)
- **表格排序**: 移除表格defaultSortOrder ([32675387d](https://github.com/tencentmusic/supersonic/commit/32675387d) by FredTsang)
- **维度搜索**: 修复维度搜索带key查询范围超出问题 ([269f146c1](https://github.com/tencentmusic/supersonic/commit/269f146c1) by wangyong)

### 测试和质量保证 / Testing & Quality Assurance

#### 单元测试 / Unit Testing
- **测试修复**: 修复单元测试用例 ([91e4b51ef](https://github.com/tencentmusic/supersonic/commit/91e4b51ef) by jerryjzhang)
- **模型测试**: 修复ModelCreateForm.tsx错误 ([d2aa73b85](https://github.com/tencentmusic/supersonic/commit/d2aa73b85) by Antgeek)

### 重要变更说明 / Breaking Changes

#### 升级注意事项 / Upgrade Notes
1. **SpringBoot 3升级**: 可能需要更新依赖配置和代码适配
2. **许可证变更**: 从MIT变更为Apache 2.0，请注意法律合规
3. **API接口调整**: 部分API接口为支持新功能进行了调整
4. **数据库兼容**: 新增多种数据库支持，配置方式有所变化

### 完整提交统计 / Commit Statistics
- **总提交数**: 419个提交
- **主要贡献者**: 
  - jerryjzhang: 158次提交
  - supersonicbi: 22次提交
  - zyclove: 20次提交
  - beat4ocean: 15次提交
  - guilinlewis: 11次提交
  - wangyong: 11次提交
  - 其他贡献者: 182次提交
- **涉及模块**: headless, chat, auth, common, webapp, launcher, docker
- **时间跨度**: 2024年11月1日 - 2025年8月5日

### 致谢 / Acknowledgments

感谢所有为SuperSonic 1.0.0版本贡献代码、文档、测试和建议的开发者们！🎉

#### 核心贡献者 / Core Contributors
- **jerryjzhang** - 项目维护者，核心架构设计与实现
- **supersonicbi** - 核心功能开发，多数据库支持
- **beat4ocean** - 架构升级，依赖管理，安全优化
- **zyclove** - 数据库适配，构建优化
- **guilinlewis** - 鉴权系统，召回优化
- **wangyong** - Bug修复，NPE异常处理

#### 活跃贡献者 / Active Contributors
- **WDEP** - 前端优化，图表功能
- **FredTsang** - Chat SDK优化，数据导出
- **feelshana** - 记忆管理，向量召回
- **QJ_wonder** - 插件功能，多轮对话
- **Willy-J** - 模型关联，数据库兼容
- **iridescentpeo** - 查询优化，模型管理
- **tristanliu** - 前端路由，权限界面
- **mislayming** - SpringBoot 3升级
- **Antgeek** - 基准测试，模型修复
- **柯慕灵** - LLM纠错器，Windows脚本
- **superhero** - 项目管理，代码审查

#### 其他重要贡献者 / Other Important Contributors
- **木鱼和尚** - 维度指标检索优化
- **pisces** - 图表导出功能
- **lexluo09** - 并行处理，GitHub Actions
- **andybj0228** - SQL查询优化
- **wanglongqiang** - SQL变量支持
- **Hyman_bz** - StarRocks支持
- **wwsheng009** - SAP HANA适配
- **poncheen** - TypeScript错误修复
- **kino** - Docker镜像优化
- **coosir** - 前端百分比显示
- **Kun Gu** - 字符编码优化
- **chixiaopao** - NPE异常修复
- **naimehao** - 核心功能修复
- **yudong** - 记忆评估优化
- **mroldx** - 数据库脚本更新
- **ChPi** - 解析器性能优化
- **Hwting** - Docker配置优化

#### 特别感谢 / Special Thanks
感谢所有提交Issue、参与讨论、提供反馈的社区用户，你们的每一个建议都让SuperSonic变得更好！

#### 社区支持 / Community Support
SuperSonic是一个开源项目，我们欢迎更多开发者加入：
- 🔗 **GitHub**: https://github.com/tencentmusic/supersonic
- 📖 **文档**: 详见项目README和Wiki
- 🐛 **Issue报告**: 欢迎提交Bug和功能请求
- 🚀 **贡献代码**: 欢迎提交Pull Request
- 💬 **社区讨论**: 加入我们的技术交流群

#### 未来展望 / Future Vision
SuperSonic 1.0.0是一个重要的里程碑，但这只是开始。我们将继续：
- 🌟 **持续优化性能和稳定性**
- 🔧 **扩展更多数据库和AI模型支持**  
- 🎨 **改善用户体验和界面设计**
- 📚 **完善文档和最佳实践**
- 🤝 **建设更活跃的开源社区**

**让我们一起把SuperSonic做得更好！** ✨

---

*如果您在使用过程中遇到问题或有改进建议，欢迎随时与我们交流。每一份贡献都让SuperSonic更加强大！*


## SuperSonic [0.9.8] - 2024-11-01
- Add LLM management module to reuse connection across agents.
- Add ChatAPP configuration sub-module in Agent Management.
- Enhance dimension value management sub-module.
- Enhance memory management and term management sub-module.
- Enhance semantic translation of complex S2SQL.
- Enhance user experience in Chat UI.
- Introduce LLM-based semantic corrector and data interpreter.

## SuperSonic [0.9.2] - 2024-06-01

### Added
- support multiple rounds of dialogue
- add term configuration and identification to help LLM learn private domain knowledge
- support configuring LLM parameters in the agent
- metric market supports searching in natural language

### Updated
- introducing WorkFlow, Mapper, Parser, and Corrector support jump execution
- Introducing the concept of Model-Set to simplify Domain management
- overall optimization and upgrade of system pages
- optimize startup script

## SuperSonic [0.9.0] - 2024-04-03

### Added
- add tag abstraction and enhance tag marketplace management.
- headless-server provides Chat API interface.

### Updated
- migrate chat-core core component to headless-core.

## SuperSonic [0.8.6] - 2024-02-23

### Added
- support view abstraction to Headless.
- add the Metric API to Headless and optimizing the Headless API.
- add integration tests to Headless.
- add TimeCorrector to Chat.

## SuperSonic [0.8.4] - 2024-01-19

### Added
- support creating derived metrics.
  - Support creating metrics using three methods: by measure, metric, and field expressions.
- added support for postgresql data source.
- code adjustment and abstract optimization for chat and headless.

## SuperSonic [0.8.2] - 2023-12-18

### Added
- rewrite Python service with Java project, default to Java implementation.
- support setting the SQL generation method for large models in the interface.
- optimization of metric market experience.
- optimization of semantic modeling canvas experience.
- code structure adjustment and abstraction optimization for chat.

## SuperSonic [0.7.5] - 2023-10-13

### Added
- add SQL generation improvement optimization, support LLM SQL, Logic SQL, and Physical SQL display.
- add showcase functionality to support recommending similar questions.
- add frontend modification of filtering conditions and re-querying feature.
- support nested query functionality in semantic.
- support switching queries between multiple parsers in the frontend.

### Updated
- optimizing the build and deployment of the project.
- overall optimization of the SQL Corrector functionality.

### Fixed
- fix execute error on mysql <=5.7
  
## SuperSonic [0.7.4] - 2023-09-10
  
### Added
- add llm parser config
- add datasource agg_time option
- add function name adaptor in clickhouse
- add dimension and metric show in dsl
  
### Updated
- update user guide doc
- update query building of plugin in default model
- update some core API constructs to keep naming consistency
- update ConfigureDemo config
- update the association mechanism so that invisible dimensions and metrics will no longer be associated

### Fixed
- fix hasAggregateFunction logic in SqlParserSelectHelper

## SuperSonic [0.7.3] - 2023-08-29

### Added
- meet checkstyle code requirements
- save parseInfo after parsing
- add time statistics
- add agent

### Updated
- dsl where condition is used for front-end display
- dsl remove context inheritance

## SuperSonic [0.7.2] - 2023-08-12

### Added
- Support asynchronous query - return parse information to user before executing result
- Add Model as the basic data structure of the semantic definitions - this will repalce the old conception of subdomain

### Updated
- improve knowledge word similarity algorithm
- improve embedding plugin chooser
- improve DSLQuery field correction and parser


### Fixed
-  Fix mapper error that detectWord text is shorter than word
-  Fix MetricDomainQuery inherit context
  
## SuperSonic [0.7.0] - 2023-07-30

### Added

- Add function call parser and embedding recall parser
- Add plugin management
- Add web page query and web service query
- Metric filter query support querying metrics and comparing them in different dimensions
- Support dimension value mapping
- Support dimension/metric invisible, chat filter related data
- Add user guide docs


### Fixed

- Fix the data problem of getDomainList interface in standalone mode

## SuperSonic [0.6.0] - 2023-07-16

### Added

- Support llm parser and llm api server - users can query data through complex natural language.
- Support fuzzy query dimension and metric name - users can set the 'metric.dimension.threshold'
  parameter to control the fuzzy threshold.
- Support query filter and domain filter in query and search - users can specify domainId and query
  filter to filter the results in search and query.
- Support standalone mode - users can integrate semantic and chat services in one process for easy
  management and debugging.
- Support dsl query in semantic - users can specify DSL language to query data in Semantic. In the
  past, data querying was limited to struct language.
- Add unit and integration testing - add integration tests for single-turn and multi-turn
  conversations, to efficiently validate the code.
- Support dimension and metric alias - users can specify one or multiple aliases to expand search
  and query.
- Add scheduled semantic metadata update functionality in chat.
- Support create datasource by table name in the web page.
- Add the ability to set permissions for domain.
- Add a local/Remote implementation to the SemanticLayer interface.

### Updated

- Code architecture adjustment in chat.

1) Abstracting into three modules, namely api, core, and knowledge. Providing four core interfaces:
   SchemaMapper, SemanticLayer, SemanticParser, and SemanticQuery.
2) Add RuleSemanticQuery and LLMSemanticQuery implement to SemanticQuery.
3) Add all possible queries to the candidate queries, and then select the most suitable query from
   the candidate queries.

- Code architecture adjustment in semantic.

1) Refactor semantic layer SQL parsing code through Calcite.
2) Add QueryOptimizer interface.

- Chat config subdivided into detailed and metric scenarios - users can set different parameters in these two scenarios.

### Fixed

- Resolved last word not be recognized in SchemaMapper.
- Fix context inheritance problem.
- Fix the error of querying H2 database by month unit.
- Set faker user to context when authentication disable.

## SuperSonic [0.5.0] - 2023-06-15

### Added
- Add the search and query feature in chat according to rules in an extensible way.
- Add semantic/chat independent service for users.
- Add Modeling Interface - users can visually define and maintain semantic models in the web page.
- Add a unified semantic parsing layer - user can query data by struct language.

# Davinci Changelog

## Davinci [0.3.0] - 2023-06-15

### Added

- add data portal
- add metric trend chart
- add feedback component
- add tab component
- add page setting

### Updated

- modify permission process
- optimize css style
- optimize filter

### Removed

- delete view module

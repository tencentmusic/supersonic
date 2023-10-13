# SuperSonic Changelog

- All notable changes to this project will be documented in this file.
- "Breaking Changes" describes any changes that may break existing functionality or cause
  compatibility issues with previous versions.

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

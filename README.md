[中文介绍](README_CN.md) | [文档中心](https://github.com/tencentmusic/supersonic/wiki)

# SuperSonic (超音数)

**SuperSonic is an out-of-the-box yet highly extensible framework for building ChatBI**. SuperSonic provides a chat interface that empowers users to query data using natural language and visualize the results with suitable charts. To enable such experience, the only thing necessary is to build logical semantic models (definition of metrics/dimensions/entities, along with their meaning, context and relationships) on top of physical data models, and no data modification or copying is required. Meanwhile, SuperSonic is designed to be pluggable, allowing new functionalities to be added through plugins and core components to be integrated with other systems.

<img src="./docs/images/supersonic_demo.gif" height="100%" width="100%" align="center"/>

## Motivation

The emergence of Large Language Model (LLM) like ChatGPT is reshaping the way information is retrieved. In the field of data analytics, both academia and industry are primarily focused on leveraging LLM to convert natural language into SQL (so called text2sql or nl2sql). While some works exhibit promising results, their **reliability** is inadequate for real-world applications.  

From our perspective, the key to filling the real-world gap lies in three aspects: 
1. Introduce a semantic layer encapsulating underlying data context(joins, formulas, etc) to reduce **complexity**.
2. Augment the LLM with schema mappers(as a kind of preprocessor) and semantic correctors(as a kind of postprocessor) to mitigate **hallucination**.
3. Utilize heuristic rules when necessary to improve **efficiency**(in terms of latency and cost).

With these ideas in mind, we develop SuperSonic as a practical reference implementation and use it to power our real-world products. Additionally, to facilitate further development of ChatBI, we decide to open source SuperSonic as an extensible framework.

## Out-of-the-box Features

- Built-in CUI(Chat User Interface) for *business users* to enter data queries
- Built-in GUI(Graphical User Interface) for *analytics engineers* to build semantic models
- Built-in GUI for *system administrators* to manage chat agents and third-party plugins
- Support input auto-completion as well as query recommendation
- Support multi-turn conversation and history context management 
- Support four-level permission control: domain-level, model-level, column-level and row-level

## Extensible Components

The high-level architecture and main process flow is as follows:

<img src="./docs/images/supersonic_components.png" height="65%" width="65%" align="center"/> 

- **Knowledge Base:** extracts schema information periodically from the semantic models and build dictionary and index to facilitate schema mapping.

- **Schema Mapper:** identifies references to schema elements(metrics/dimensions/entities/values) in user queries. It matches the query text against the knowledge base.

- **Semantic Parser:** understands user queries and extracts semantic information. It consists of a combination of rule-based and model-based parsers, each of which deals with specific scenarios.

- **Semantic Corrector:** checks validity of extracted semantic information and performs correction and optimization if needed.

- **Semantic Interpreter:** performs execution according to extracted semantic information. It generates SQL statements and executes them against physical data models.

- **Chat Plugin:** extends functionality with third-party tools. The LLM is going to select the most suitable one, given all configured plugins with function description and sample questions.

## Quick Demo

SuperSonic comes with sample semantic models as well as chat conversations that can be used as a starting point. Please follow the steps: 

- Download the latest prebuilt binary from the [release page](https://github.com/tencentmusic/supersonic/releases)
- Run script "bin/supersonic-daemon.sh" to start services (one java process and one python process)
- Visit http://localhost:9080 in the browser to start exploration

## Build and Development

Please refer to project [wiki](https://github.com/tencentmusic/supersonic/wiki). 

## WeChat Contact

Please follow SuperSonic wechat official account:

<img src="./docs/images/supersonic_wechat_oa.png" height="50%" width="50%" align="center"/> 
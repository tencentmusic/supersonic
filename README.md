English | [中文](README_CN.md)

# SuperSonic (超音数)

**SuperSonic is an out-of-the-box yet highly extensible framework for building a data chatbot**. SuperSonic provides a chat interface that empowers users to query data using natural language and visualize the results with suitable charts. To enable such experience, the only thing necessary is to define logical semantic models (metrics, dimensions, aliases, relationships, etc) on top of physical data models, and no data modification or copying is required. Meanwhile SuperSonic is designed to be plugable, allowing new functionalities to be added through plugins and core components to be integrated into other systems.

<img src="./docs/images/supersonic_demo.gif" height="70%" width="70%" align="center"/>

## Motivation

The emergence of Large Language Models (LLMs) like ChatGPT is reshaping the way information is retrieved. In the field of data analytics, both academia and industry are primarily focused on leveraging deep learning models to convert natural language queries into SQL queries. While some works show promising results, they are not applicable to real-world scenarios. 

From our perspective, the key to filling the real-world gap lies in two aspects: 
1. Utilize a combination of rule-based and model-based semantic parsers to deal with different scenarios
2. Introduce a semantic model layer to encapsulate underlying complexity thus simplify the semantic parsers

With these ideas in mind, we developed SuperSonic as a practical reference implementation and used it to power our real-world products. Additionally, to encourage further development of data chatbots, we decided to open source SuperSonic as an extensible framework.

## Out-of-the-box Features

- Built-in graphical interface for business users to enter data queries 
- Built-in graphical interface for analytics engineers to manage semantic models
- Support input auto-completion as well as query recommendation
- Support multi-turn conversation and history context management 
- Support three-level permission control: domain-level, column-level and row-level 

## Extensible Components

SuperSonic is composed of two layers: supersonic-chat and supersonic-semantic. The chat layer is responsible for converting **natural language query** into semantic query (also known as DSL query), whereas the semantic layer is responsible for converting DSL query into **SQL query**. The high-level architecture and main process flow is shown in below diagram:

<img src="./docs/images/supersonic_components.png" height="70%" width="70%" align="center"/> 

### Chat Layer

The chat layer contains four core components:

- **Chat Interface:** accepts user queries and answer results with appropriate visualization charts. It supports input auto-completion as well as multi-turn conversation.

- **Schema Mapper Chain:** identifies references to semantic schema elements in user queries. It matches queries against the knowledage base which is constructed using the schema of semantic models.

- **Semantic Parser Chain:** resolves query mode based on mapped semantic models. It is composed of a group of rule-based and model-based parsers, each of which deals with specific scenarios.

- **Semantic Query:** performs execution according to the results of semantic parsing. The default semantic query would submit DSL to the semantic component, but new types of semantic query can be extended.

### Semantic Layer

The semantic layer contains four core components:

- **Modeling Interface:** empowers analytics engineers to visually define and maintain semantic models. The configurations related to access permission and chat conversation can also be set on the UI.

- **DSL Parser:** converts DSL expression to intermediate structures. To make it easily integratable with analytics applications, SQL (without joins and calculation formulas) is used as the DSL.

- **Query Planner:** builds and optimizes query plans according to various rules. 

- **SQL Generator:** generates final SQL expression (with joins and calculation formulas) based on the query plan.

## Quick Demo

SuperSonic comes with sample semantic models as well as chat conversations that can be used as a starting point. Please follow the steps: 

- Download the latest prebuilt binary from the [release page](https://github.com/tencentmusic/supersonic/releases)
- Run script "bin/start-standalone.sh" to start a standalone server
- Visit http://localhost:9080 in browser to start exploration

## How to Build

SuperSonic can be deployed in two modes: standalone (intended for quick demo) and distributed (intended for production). 

### Build for Standalone Mode 
 
Pull the source code and run script "assembly/bin/build-standalone.sh" to build a single packages.

### Build for Distributed Mode 

Pull the source code and run scripts "assembly/bin/build-chat.sh" and "assembly/bin/build-semantic.sh" separately to build packages.

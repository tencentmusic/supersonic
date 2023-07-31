English | [中文](README_CN.md)

# SuperSonic (超音数)

**SuperSonic is an out-of-the-box yet highly extensible framework for building a data chatbot**. SuperSonic provides a chat interface that empowers users to query data using natural language and visualize the results with suitable charts. To enable such experience, the only thing necessary is to build logical semantic models (definition of metrics/dimensions/entities, along with their meaning, context and relationships) on top of physical data models, and no data modification or copying is required. Meanwhile, SuperSonic is designed to be pluggable, allowing new functionalities to be added through plugins and core components to be integrated with other systems.

<img src="./docs/images/supersonic_demo.gif" height="100%" width="100%" align="center"/>

## Motivation

The emergence of Large Language Model (LLM) like ChatGPT is reshaping the way information is retrieved. In the field of data analytics, both academia and industry are primarily focused on leveraging LLM to convert natural language queries into SQL queries. While some works show promising results, they are still not applicable to real-world scenarios.

From our perspective, the key to filling the real-world gap lies in two aspects: 
1. Utilize a combination of rule-based and model-based semantic parsers to deal with different scenarios.
2. Introduce a semantic model layer encapsulating the underlying data complexity(joins, formulas, etc) to simplify semantic parsing.

With these ideas in mind, we develop SuperSonic as a practical reference implementation and use it to power our real-world products. Additionally, to facilitate further development of data chatbot, we decide to open source SuperSonic as an extensible framework.

## Out-of-the-box Features

- Built-in graphical interface for business users to enter data queries 
- Built-in graphical interface for analytics engineers to manage semantic models
- Support input auto-completion as well as query recommendation
- Support multi-turn conversation and history context management 
- Support three-level permission control: domain-level, column-level and row-level 

## Extensible Components

The high-level architecture and main process flow is shown in below diagram:

<img src="./docs/images/supersonic_components.png" height="100%" width="100%" align="center"/> 

- **Chat Interface:** accepts natural language queries and answer results with appropriate visualization charts. It supports input auto-completion as well as multi-turn conversation.

- **Modeling Interface:** empowers analytics engineers to visually define and maintain semantic models. The configurations related to access permission and chat conversation can also be set on the UI.

- **Schema Mapper Chain:** identifies references to schema elements(metrics/dimensions/entities/values) in user queries. It matches the query text against a knowledge base constructed from the semantic models.

- **Semantic Parser Chain:** understands user queries and extract semantic information. It consists of a combination of rule-based and model-based parsers, each of which deals with specific scenarios.

- **Semantic Query:** performs execution according to extracted semantic information. It generates SQL queries and executes them against physical data models.

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
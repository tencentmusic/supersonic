English | [中文](README_CN.md)

# SuperSonic (超音数)

**SuperSonic is an out-of-the-box yet highly extensible framework for building a data chatbot**. SuperSonic provides a chat interface that empowers users to query data using natural language and visualize the results with suitable charts. To enable such experience, the only thing necessary is to build a logical semantic model (definition of metrics, dimensions, relationships, etc) on top of the physical data stores, and no data modification or copying is required. Meanwhile SuperSonic is designed to be plug-and-play, allowing new functionalities to be added through plugins and core components to be integrated into other systems.

## Motivation

The emergence of Large Language Models (LLMs) like ChatGPT is reshaping the way information is retrieved. In the field of data analytics, both academia and industry are primarily focused on leveraging deep learning models to convert natural language queries into SQL queries. While some works show promising results, they are not applicable to real-world scenarios. 

From our perspective, the key to filling the real-world gap lies in two aspects: 
1. Utilize a combination of rule-based and model-based semantic parsers to deal with different scenarios
2. Introduce a semantic model layer to encapsulate underlying complexity thus simplify the semantic parsers

With these ideas in mind, we developed SuperSonic as a reference implementation and used it to power our real-world products. Additionally, to encourage further development of data chatbots, we decided to open source SuperSonic as an extensible framework.

## Out-of-the-box Features

- Built-in graphical interface for business users to enter data queries 
- Built-in graphical interface for analytics engineers to manage semantic models
- Support input auto-completion as well as query recommendation
- Support multi-turn conversation and switch context automatically 
- Support three-level permission control: domain-level, column-level and row-level 

## Extensible Components

SuperSonic contains four core components, each of which can be extended or integrated: 

<img src="./docs/images/supersonic_components.png" height="50%" width="50%" align="center"/> 

- **Chat interface:** accepts user queries and answer results with approriate visualization charts. It supports input auto-completion as well as multi-turn conversation.

- **Schema mapper:** identifies references to schema elements in natural language queries. It matches queries against the knowledage base which is constructed using the schema of semantic models.

- **Semantic parser chain:** resolves query mode and choose the most suitable semantic model. It is composed of a group of rule-based and model-based parsers, each of which deals with specific scenarios.

- **Semantic model layer:** manages semantic models and generate SQL statement given specific semantic model and related semantic items. It encapsulates technical concepts, calculation formulas and entity relationships of the underlying data.

## Quick Demo

SuperSonic comes with a sample semantic data model as well as sample chat that can be used as a starting point. Please follow the steps: 

- Download the latest prebuilt binary from the release page
- Run script "bin/start-all.sh" to start services
- Visit http://localhost:9080 in browser to explore chat interface
- Visit http://localhost:9081 in browser to explore modeling interface

## How to Build

Download the source code and run script "assembly/bin/build-all.sh" to build both front-end webapp and back-end services

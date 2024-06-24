[中文版](README_CN.md) | [日本語版](README_JP.md) | [Docs](https://supersonicbi.github.io/)

![Java CI](https://github.com/tencentmusic/supersonic/workflows/supersonic%20CI/badge.svg)

# SuperSonic

SuperSonic is the next-generation BI platform that integrates **Chat BI** (powered by LLM) and **Headless BI** (powered by semantic layer) paradigms. This integration ensures that Chat BI has access to the same curated and governed semantic data models as traditional BI. Furthermore, the implementation of both paradigms benefits from the integration: 

- Chat BI's Text2SQL gets augmented with context-retrieval from semantic models.
- Headless BI's query interface gets extended with natural language API.

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_ideas.png" height="75%" width="75%" />

SuperSonic provides a **Chat BI interface** that empowers users to query data using natural language and visualize the results with suitable charts. To enable such experience, the only thing necessary is to build logical semantic models (definition of metric/dimension/tag, along with their meaning and relationships) through a **Headless BI interface**. Meanwhile, SuperSonic is designed to be extensible and composable, allowing custom implementations to be added and configured with Java SPI.

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_demo.gif" height="100%" width="100%" />

## Motivation

The emergence of Large Language Model (LLM) like ChatGPT is reshaping the way information is retrieved, leading to a new paradigm in the field of data analytics known as Chat BI. To implement Chat BI, both academia and industry are primarily focused on harnessing the power of LLMs to convert natural language into SQL, commonly referred to as Text2SQL or NL2SQL. While some approaches show promising results, their **reliability** falls short for large-scale real-world applications.

Meanwhile, another emerging paradigm called Headless BI, which focuses on constructing unified semantic data models, has garnered significant attention. Headless BI is implemented through a universal semantic layer that exposes consistent data semantics via an open API.

From our perspective, the integration of Chat BI and Headless BI has the potential to enhance the Text2SQL generation in two dimensions:

1. Incorporate data semantics (such as business terms, column values, etc.) into the prompt, enabling LLM to better understand the semantics and **reduce hallucination**.
2. Offload the generation of advanced SQL syntax (such as join, formula, etc.) from LLM to the semantic layer to **reduce complexity**. 

With these ideas in mind, we develop SuperSonic as a practical reference implementation and use it to power our real-world products. Additionally, to facilitate further development we decide to open source SuperSonic as an extensible framework.

## Out-of-the-box Features

- Built-in Chat BI interface for *business users* to enter natural language queries
- Built-in Headless BI interface for *analytics engineers* to build semantic data models
- Built-in rule-based semantic parser to improve efficiency in certain scenarios (e.g. demonstration, integration testing)
- Built-in support for input auto-completion, multi-turn conversation as well as post-query recommendation
- Built-in support for three-level data access control: dataset-level, column-level and row-level

## Extensible Components

The high-level architecture and main process flow is as follows:

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_components.png" height="65%" width="65%" /> 

- **Knowledge Base:** extracts schema information periodically from the semantic models and build dictionary and index to facilitate schema mapping.

- **Schema Mapper:** identifies references to schema elements(metrics/dimensions/entities/values) in user queries. It matches the query text against the knowledge base.

- **Semantic Parser:** understands user queries and generates semantic query statement. It consists of a combination of rule-based and model-based parsers, each of which deals with specific scenarios.

- **Semantic Corrector:** checks validity of semantic query statement and performs correction and optimization if needed.

- **Semantic Translator:** converts semantic query statement into SQL statement that can be executed against physical data models.

- **Chat Plugin:** extends functionality with third-party tools. The LLM is going to select the most suitable one, given all configured plugins with function description and sample questions.

## Quick Demo
### Online playground
Visit http://117.72.46.148:9080 to register and experience as a new user. Please do not modify system configurations. We will restart to reset configurations regularly every weekend.

### Local build
SuperSonic comes with sample semantic models as well as chat conversations that can be used as a starting point. Please follow the steps: 

- Download the latest prebuilt binary from the [release page](https://github.com/tencentmusic/supersonic/releases)
- Run script "assembly/bin/supersonic-daemon.sh start" to start a standalone Java service
- Visit http://localhost:9080 in the browser to start exploration

## Build and Development

Please refer to project [Docs](https://supersonicbi.github.io/docs/%E7%B3%BB%E7%BB%9F%E9%83%A8%E7%BD%B2/%E7%BC%96%E8%AF%91%E6%9E%84%E5%BB%BA/). 

## WeChat Contact

Please follow SuperSonic wechat official account:

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_wechat_oa.png" height="50%" width="50%" /> 

Welcome to join the WeChat community:

<img src="https://github.com/supersonicbi/supersonic-website/blob/main/static/img/supersonic_wechat.png" height="50%" width="50%" /> 

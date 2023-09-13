# LLM模型配置

### **简介**

语言模型的使用是超音数的重要一环。能显著增强对用户的问题的理解能力，是通过对话形式与用户交互的基石之一。在本项目中对语言模型能力的应用主要在 LLM 和 Embedding 两方面;默认使用的模型中，LLM选用闭源模型 gpt-3.5-turbo-16k，Embedding模型选用开源模型 GanymedeNil/text2vec-large-chinese。用户可以根据自己实际需求进行配置更改。


### **配置方式**
<div align="left" >
    <img src=../images/nlp_config.png width="70%"/>
    <p>图1-1 LLM配置文件</p>
</div>

1. LLM模型相关的配置，在 supersonic/chat/core/src/main/python/llm/run_config.py 进行配置。
2. LLM采用OpenAI的闭源模型 gpt-3.5-turbo-16k，在使用时需要提供OpenAI的API-Key才能调用LLM模型，通过 OPENAI_API_KEY 变量进行配置。
3. Embedding模型采用开源模型 GanymedeNil/text2vec-large-chinese，通过 HF_TEXT2VEC_MODEL_NAME 变量进行位置，为了使用方便采用托管在HuggingFace的源，初次启动时自动下载模型文件。

### **FAQ**
1. 可以用开源的LLM模型替代OpenAI的GPT模型吗？
   - 暂时不能。我们测试过大部分主流的开源LLM，在实际使用中，在本项目需要LLM提供的逻辑推理和代码生成场景上，开源模型还不能满足需求。
   - 我们会持续跟进开源LLM的最新进展，在有满足要求的开源LLM后，在项目中集成私有化部署开源LLM的能力。
2. GPT4、GPT3.5、GPT3.5-16k 这几个模型用哪个比较好？
   - GPT3.5、GPT3.5-16k 均能基本满足要求，但会有输出结果不稳定的情况；GPT3.5的token长度限制为4k，在现有CoT策略下，容易出现超过长度限制的情况。
   - GPT4的输出更稳定，但费用成本远超GPT3.5，可以根据实际使用场景进行选择。
3. Embedding模型用其他的可以吗？
   - 可以。可以以该项目[text2vec]([URL](https://github.com/shibing624/text2vec))的榜单作为参考，然后在HuggingFace找到对应模型的model card，修改HF_TEXT2VEC_MODEL_NAME变量的取值。

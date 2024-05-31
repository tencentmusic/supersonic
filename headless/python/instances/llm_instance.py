# -*- coding:utf-8 -*-
from langchain import llms

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config.config_parse import LLM_PROVIDER_NAME, llm_config_dict


def get_llm(llm_config: dict):
    if LLM_PROVIDER_NAME in llms.type_to_cls_dict:
        llm_provider = llms.type_to_cls_dict[LLM_PROVIDER_NAME]
        if llm_config is None or llm_config["baseUrl"] is None or llm_config["baseUrl"] == '':
            llm = llm_provider(**llm_config_dict)
        else:
            openai_llm_config = {}
            openai_llm_config["model_name"] = llm_config["modelName"]
            openai_llm_config["openai_api_base"] = llm_config["baseUrl"]
            openai_llm_config["openai_api_key"] = llm_config["apiKey"]
            openai_llm_config["temperature"] = llm_config["temperature"]
            llm = llm_provider(**openai_llm_config)
        return llm
    else:
        raise Exception("llm_provider_name is not supported: {}".format(LLM_PROVIDER_NAME))
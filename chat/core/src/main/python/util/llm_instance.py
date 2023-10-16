# -*- coding:utf-8 -*-
from langchain import llms

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config.config_parse import LLM_PROVIDER_NAME, llm_config_dict


def get_llm_provider(llm_provider_name: str, llm_config_dict: dict):
    if llm_provider_name in llms.type_to_cls_dict:
        llm_provider = llms.type_to_cls_dict[llm_provider_name]
        llm = llm_provider(**llm_config_dict)
        return llm
    else:
        raise Exception("llm_provider_name is not supported: {}".format(llm_provider_name))


llm = get_llm_provider(LLM_PROVIDER_NAME, llm_config_dict)
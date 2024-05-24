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
        if llm_config is None:
            llm = llm_provider(**llm_config_dict)
        else:
            llm = llm_provider(**llm_config)
        return llm
    else:
        raise Exception("llm_provider_name is not supported: {}".format(LLM_PROVIDER_NAME))
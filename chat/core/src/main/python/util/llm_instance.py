# -*- coding:utf-8 -*-
from langchain.llms import OpenAI

from run_config import *
from util.stringutils import *

llm = OpenAI(
    model_name=MODEL_NAME,
    openai_api_key=OPENAI_API_KEY,
    openai_api_base=default_if_blank(OPENAI_API_BASE),
    temperature=TEMPERATURE,
)

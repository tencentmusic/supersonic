# -*- coding:utf-8 -*-
from langchain.llms import OpenAI

from run_config import MODEL_NAME, OPENAI_API_KEY, TEMPERATURE


llm = OpenAI(
    openai_api_key=OPENAI_API_KEY, model_name=MODEL_NAME, temperature=TEMPERATURE
)

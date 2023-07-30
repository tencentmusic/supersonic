# -*- coding:utf-8 -*-

from typing import List, Union
import logging
import json
import os
import sys
from langchain.llms import OpenAI

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from sql.prompt_maker import schema_linking_exampler, schema_link_parse, \
  sql_exampler

MODEL_NAME = "gpt-3.5-turbo-16k"
OPENAI_API_KEY = "YOUR_API_KEY"
TEMPERATURE = 0.0

llm = OpenAI(openai_api_key=OPENAI_API_KEY, model_name=MODEL_NAME,
             temperature=TEMPERATURE)


def query2sql(query_text: str, schema: dict):
  print("schema: ", schema)

  domain_name = schema['domainName']
  fields_list = schema['fieldNameList']

  schema_linking_prompt = schema_linking_exampler(query_text, domain_name,
                                                  fields_list)
  schema_link_output = llm(schema_linking_prompt)
  schema_link_str = schema_link_parse(schema_link_output)

  sql_prompt = sql_exampler(query_text, domain_name, schema_link_str)
  sql_output = llm(sql_prompt)

  resp = dict()
  resp['query'] = query_text
  resp['domain'] = domain_name
  resp['fields'] = fields_list

  resp['schemaLinkingOutput'] = schema_link_output
  resp['schemaLinkStr'] = schema_link_str

  resp['sqlOutput'] = sql_output

  return resp

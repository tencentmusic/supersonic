# -*- coding:utf-8 -*-

from typing import List, Union
import logging
import json
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from sql.prompt_maker import schema_linking_exampler, schema_link_parse, \
  sql_exampler

from util.llm_instance import llm

def query2sql(query_text: str, schema: dict):
  print("schema: ", schema)

  model_name = schema['modelName']
  fields_list = schema['fieldNameList']

  schema_linking_prompt = schema_linking_exampler(query_text, model_name,
                                                  fields_list)
  schema_link_output = llm(schema_linking_prompt)
  schema_link_str = schema_link_parse(schema_link_output)

  sql_prompt = sql_exampler(query_text, model_name, schema_link_str)
  sql_output = llm(sql_prompt)

  resp = dict()
  resp['query'] = query_text
  resp['model'] = model_name
  resp['fields'] = fields_list

  resp['schemaLinkingOutput'] = schema_link_output
  resp['schemaLinkStr'] = schema_link_str

  resp['sqlOutput'] = sql_output

  return resp

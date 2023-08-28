# -*- coding:utf-8 -*-
from typing import Any, List, Mapping, Optional, Union
import requests
import logging
import json
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from langchain.prompts import PromptTemplate
from langchain.prompts.few_shot import FewShotPromptTemplate
from langchain.llms import OpenAI

from few_shot_example.sql_exampler import examplars
from output_parser import schema_link_parse


def schema_linking_prompt_maker(user_query: str, model_name: str,
    fields_list: List[str],
    few_shots_example: str):
  instruction = "# 根据数据库的表结构,找出为每个问题生成SQL查询语句的schema_links\n"

  schema_linking_prompt = "Table {table_name}, columns = {fields_list}\n问题:{user_query}\n分析: 让我们一步一步地思考。".format(
    table_name=model_name,
    fields_list=fields_list,
    user_query=user_query)

  return instruction + few_shots_example + schema_linking_prompt


def schema_linking_exampler(user_query: str,
    model_name: str,
    fields_list: List[str]
) -> str:
  example_prompt_template = PromptTemplate(
    input_variables=["table_name", "fields_list", "question", "analysis",
                     "schema_links"],
    template="Table {table_name}, columns = {fields_list}\n问题:{question}\n分析:{analysis} 所以Schema_links是:\nSchema_links:{schema_links}")

  instruction = "# 根据数据库的表结构,找出为每个问题生成SQL查询语句的schema_links"

  schema_linking_prompt = "Table {table_name}, columns = {fields_list}\n问题:{question}\n分析: 让我们一步一步地思考。"

  schema_linking_example_prompt_template = FewShotPromptTemplate(
      examples=examplars,
      example_prompt=example_prompt_template,
      example_separator="\n\n",
      prefix=instruction,
      input_variables=["table_name", "fields_list", "question"],
      suffix=schema_linking_prompt
  )

  schema_linking_example_prompt = schema_linking_example_prompt_template.format(
    table_name=model_name,
    fields_list=fields_list,
    question=user_query)

  return schema_linking_example_prompt


def sql_exampler(user_query: str,
    model_name: str,
    schema_link_str: str
) -> str:
  instruction = "# 根据schema_links为每个问题生成SQL查询语句"

  sql_example_prompt_template = PromptTemplate(
    input_variables=["question", "table_name", "schema_links", "sql"],
    template="问题:{question}\nTable {table_name}\nSchema_links:{schema_links}\nSQL:{sql}")

  sql_prompt = "问题:{question}\nTable {table_name}\nSchema_links:{schema_links}\nSQL:"

  sql_example_prompt_template = FewShotPromptTemplate(
      examples=examplars,
      example_prompt=sql_example_prompt_template,
      example_separator="\n\n",
      prefix=instruction,
      input_variables=["question", "table_name", "schema_links"],
      suffix=sql_prompt
  )

  sql_example_prompt = sql_example_prompt_template.format(question=user_query,
                                                          table_name=model_name,
                                                          schema_links=schema_link_str)

  return sql_example_prompt

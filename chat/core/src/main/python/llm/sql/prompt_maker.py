# -*- coding:utf-8 -*-
from typing import Any, List, Mapping, Optional, Union
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from langchain.prompts import PromptTemplate
from langchain.prompts.few_shot import FewShotPromptTemplate
from langchain.prompts.example_selector import SemanticSimilarityExampleSelector


def schema_linking_exampler(user_query: str,
                            domain_name: str,
                            fields_list: List[str],
                            prior_schema_links: Mapping[str,str],
                            example_selector: SemanticSimilarityExampleSelector, 
                            ) -> str:

    prior_schema_links_str = '['+ ','.join(["""'{}'->{}""".format(k,v) for k,v in prior_schema_links.items()]) + ']'

    example_prompt_template = PromptTemplate(input_variables=["table_name", "fields_list", "prior_schema_links", "question", "analysis", "schema_links"],
                                template="Table {table_name}, columns = {fields_list}, prior_schema_links = {prior_schema_links}\n问题:{question}\n分析:{analysis} 所以Schema_links是:\nSchema_links:{schema_links}")

    instruction = "# 根据数据库的表结构,参考先验信息,找出为每个问题生成SQL查询语句的schema_links"

    schema_linking_prompt = "Table {table_name}, columns = {fields_list}, prior_schema_links = {prior_schema_links}\n问题:{question}\n分析: 让我们一步一步地思考。"

    schema_linking_example_prompt_template = FewShotPromptTemplate(
        example_selector=example_selector,
        example_prompt=example_prompt_template,
        example_separator="\n\n", 
        prefix=instruction,
        input_variables=["table_name", "fields_list", "prior_schema_links", "question"],
        suffix=schema_linking_prompt
        )

    schema_linking_example_prompt = schema_linking_example_prompt_template.format(table_name=domain_name,
                                                                                    fields_list=fields_list,
                                                                                    prior_schema_links=prior_schema_links_str,
                                                                                    question=user_query)

    return schema_linking_example_prompt


def sql_exampler(user_query: str,
                domain_name: str,
                schema_link_str: str,
                data_date: str,
                example_selector: SemanticSimilarityExampleSelector,
                ) -> str:
    
    instruction = "# 根据schema_links为每个问题生成SQL查询语句"

    sql_example_prompt_template = PromptTemplate(input_variables=["question", "current_date", "table_name", "schema_links", "sql"],
                                template="问题:{question}\nCurrent_date:{current_date}\nTable {table_name}\nSchema_links:{schema_links}\nSQL:{sql}")

    sql_prompt = "问题:{question}\nCurrent_date:{current_date}\nTable {table_name}\nSchema_links:{schema_links}\nSQL:"

    sql_example_prompt_template = FewShotPromptTemplate(
        example_selector=example_selector,
        example_prompt=sql_example_prompt_template,
        example_separator="\n\n", 
        prefix=instruction,
        input_variables=["question", "current_date", "table_name", "schema_links"],
        suffix=sql_prompt
        )

    sql_example_prompt = sql_example_prompt_template.format(question=user_query,
                                                            current_date=data_date,
                                                            table_name=domain_name,
                                                            schema_links=schema_link_str)

    return sql_example_prompt

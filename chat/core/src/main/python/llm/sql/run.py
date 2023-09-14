from typing import List, Union, Mapping
import logging
import json
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from sql.prompt_maker import schema_linking_exampler, sql_exampler
from sql.constructor import schema_linking_example_selector, sql_example_selector,sql_examples_vectorstore, reload_sql_example_collection
from sql.output_parser import schema_link_parse

from util.llm_instance import llm


class Text2DSLAgent(object):
        def __init__(self):
            self.schema_linking_exampler = schema_linking_exampler
            self.sql_exampler = sql_exampler

            self.sql_examples_vectorstore = sql_examples_vectorstore
            self.schema_linking_example_selector = schema_linking_example_selector
            self.sql_example_selector = sql_example_selector

            self.schema_link_parse = schema_link_parse

            self.llm = llm

        def update_examples(self, sql_examplars, example_nums):
            self.sql_examples_vectorstore, self.schema_linking_example_selector, self.sql_example_selector = reload_sql_example_collection(self.sql_examples_vectorstore, 
                                                                                                                                           sql_examplars, 
                                                                                                                                           self.schema_linking_example_selector, 
                                                                                                                                           self.sql_example_selector, 
                                                                                                                                           example_nums)

        def query2sql(self, query_text: str, 
            schema : Union[dict, None] = None,
            current_date: str = None,
            linking: Union[List[Mapping[str, str]], None] = None
            ):

            print("query_text: ", query_text)
            print("schema: ", schema)
            print("current_date: ", current_date)
            print("prior_schema_links: ", linking)

            if linking is not None:
                  prior_schema_links = {item['fieldValue']:item['fieldName'] for item in linking}
            else:
                  prior_schema_links = {}

            model_name = schema['modelName']
            fields_list = schema['fieldNameList']

            schema_linking_prompt = self.schema_linking_exampler(query_text, model_name, fields_list, prior_schema_links, self.schema_linking_example_selector)
            print("schema_linking_prompt->", schema_linking_prompt)
            schema_link_output = self.llm(schema_linking_prompt)
            schema_link_str = self.schema_link_parse(schema_link_output)
            
            sql_prompt = self.sql_exampler(query_text, model_name, schema_link_str, current_date, self.sql_example_selector)
            print("sql_prompt->", sql_prompt)
            sql_output = llm(sql_prompt)

            resp = dict()
            resp['query'] = query_text
            resp['model'] = model_name
            resp['fields'] = fields_list
            resp['priorSchemaLinking'] = linking
            resp['dataDate'] = current_date

            resp['schemaLinkingOutput'] = schema_link_output
            resp['schemaLinkStr'] = schema_link_str
            
            resp['sqlOutput'] = sql_output

            print("resp: ", resp)

            return resp

text2sql_agent = Text2DSLAgent()


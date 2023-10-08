import os
import sys
from typing import List, Union, Mapping

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from util.logging_utils import logger

from sql.prompt_maker import (
    schema_linking_exampler,
    sql_exampler,
    schema_linking_sql_combo_examplar,
)
from sql.constructor import (
    sql_examples_vectorstore,
    sql_example_selector,
    reload_sql_example_collection,
)
from sql.output_parser import (
    schema_link_parse,
    combo_schema_link_parse,
    combo_sql_parse,
)

from util.llm_instance import llm
from config.config_parse import TEXT2DSL_IS_SHORTCUT


class Text2DSLAgent(object):
    def __init__(self):
        self.schema_linking_exampler = schema_linking_exampler
        self.sql_exampler = sql_exampler

        self.schema_linking_sql_combo_exampler = schema_linking_sql_combo_examplar

        self.sql_examples_vectorstore = sql_examples_vectorstore
        self.sql_example_selector = sql_example_selector

        self.schema_link_parse = schema_link_parse
        self.combo_schema_link_parse = combo_schema_link_parse
        self.combo_sql_parse = combo_sql_parse

        self.llm = llm

        self.is_shortcut = TEXT2DSL_IS_SHORTCUT

    def update_examples(self, sql_examples, example_nums, is_shortcut):
        (
            self.sql_examples_vectorstore,
            self.sql_example_selector,
        ) = reload_sql_example_collection(
            self.sql_examples_vectorstore,
            sql_examples,
            self.sql_example_selector,
            example_nums,
        )
        self.is_shortcut = is_shortcut

    def query2sql(
        self,
        query_text: str,
        schema: Union[dict, None] = None,
        current_date: str = None,
        linking: Union[List[Mapping[str, str]], None] = None,
    ):

        logger.info("query_text: {}".format(query_text))
        logger.info("schema: {}".format(schema))
        logger.info("current_date: {}".format(current_date))
        logger.info("prior_schema_links: {}".format(linking))

        if linking is not None:
            prior_schema_links = {
                item["fieldValue"]: item["fieldName"] for item in linking
            }
        else:
            prior_schema_links = {}

        model_name = schema["modelName"]
        fields_list = schema["fieldNameList"]

        schema_linking_prompt = self.schema_linking_exampler(
            query_text,
            model_name,
            fields_list,
            prior_schema_links,
            self.sql_example_selector,
        )
        logger.info("schema_linking_prompt-> {}".format(schema_linking_prompt))
        schema_link_output = self.llm(schema_linking_prompt)
        schema_link_str = self.schema_link_parse(schema_link_output)

        sql_prompt = self.sql_exampler(
            query_text,
            model_name,
            schema_link_str,
            current_date,
            self.sql_example_selector,
        )
        logger.info("sql_prompt-> {}".format(sql_prompt))
        sql_output = self.llm(sql_prompt)

        resp = dict()
        resp["query"] = query_text
        resp["model"] = model_name
        resp["fields"] = fields_list
        resp["priorSchemaLinking"] = linking
        resp["dataDate"] = current_date

        resp["analysisOutput"] = schema_link_output
        resp["schemaLinkStr"] = schema_link_str

        resp["sqlOutput"] = sql_output

        logger.info("resp: {}".format(resp))

        return resp

    def query2sqlcombo(
        self,
        query_text: str,
        schema: Union[dict, None] = None,
        current_date: str = None,
        linking: Union[List[Mapping[str, str]], None] = None,
    ):

        logger.info("query_text: {}".format(query_text))
        logger.info("schema: {}".format(schema))
        logger.info("current_date: {}".format(current_date))
        logger.info("prior_schema_links: {}".format(linking))

        if linking is not None:
            prior_schema_links = {
                item["fieldValue"]: item["fieldName"] for item in linking
            }
        else:
            prior_schema_links = {}

        model_name = schema["modelName"]
        fields_list = schema["fieldNameList"]

        schema_linking_sql_combo_prompt = self.schema_linking_sql_combo_exampler(
            query_text,
            model_name,
            current_date,
            fields_list,
            prior_schema_links,
            self.sql_example_selector,
        )
        logger.info("schema_linking_sql_combo_prompt-> {}".format(schema_linking_sql_combo_prompt))
        schema_linking_sql_combo_output = self.llm(schema_linking_sql_combo_prompt)

        schema_linking_str = self.combo_schema_link_parse(
            schema_linking_sql_combo_output
        )
        sql_str = self.combo_sql_parse(schema_linking_sql_combo_output)

        resp = dict()
        resp["query"] = query_text
        resp["model"] = model_name
        resp["fields"] = fields_list
        resp["priorSchemaLinking"] = prior_schema_links
        resp["dataDate"] = current_date

        resp["analysisOutput"] = schema_linking_sql_combo_output
        resp["schemaLinkStr"] = schema_linking_str
        resp["sqlOutput"] = sql_str

        logger.info("resp: {}".format(resp))

        return resp

    def query2sql_run(
        self,
        query_text: str,
        schema: Union[dict, None] = None,
        current_date: str = None,
        linking: Union[List[Mapping[str, str]], None] = None,
    ):

        if self.is_shortcut:
            return self.query2sqlcombo(query_text, schema, current_date, linking)
        else:
            return self.query2sql(query_text, schema, current_date, linking)


text2sql_agent = Text2DSLAgent()

# -*- coding:utf-8 -*-
import re

import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from util.logging_utils import logger


def schema_link_parse(schema_link_output):
    try:
        schema_link_output = schema_link_output.strip()
        pattern = r"Schema_links:(.*)"
        schema_link_output = re.findall(pattern, schema_link_output, re.DOTALL)[
            0
        ].strip()
    except Exception as e:
        logger.exception(e)
        schema_link_output = None

    return schema_link_output


def combo_schema_link_parse(schema_linking_sql_combo_output: str):
    try:
        schema_linking_sql_combo_output = schema_linking_sql_combo_output.strip()
        pattern = r"Schema_links:(\[.*?\])"
        schema_links_match = re.search(pattern, schema_linking_sql_combo_output)

        if schema_links_match:
            schema_links = schema_links_match.group(1)
        else:
            schema_links = None
    except Exception as e:
        logger.info(e)
        schema_links = None

    return schema_links


def combo_sql_parse(schema_linking_sql_combo_output: str):
    try:
        schema_linking_sql_combo_output = schema_linking_sql_combo_output.strip()
        pattern = r"SQL:(.*)"
        sql_match = re.search(pattern, schema_linking_sql_combo_output)

        if sql_match:
            sql = sql_match.group(1)
        else:
            sql = None
    except Exception as e:
        logger.exception(e)
        sql = None

    return sql

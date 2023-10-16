# -*- coding:utf-8 -*-
import json
import os
import sys
from typing import List, Mapping

import requests

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config.config_parse import (TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM, TEXT2DSL_IS_SHORTCUT, 
                                LLMPARSER_HOST, LLMPARSER_PORT)
from few_shot_example.sql_exampler import examplars as sql_examplars
from util.logging_utils import logger


def text2dsl_setting_update(
    llm_parser_host: str,
    llm_parser_port: str,
    sql_examplars: List[Mapping[str, str]],
    example_nums: int,
    is_shortcut: bool,
):

    url = f"http://{llm_parser_host}:{llm_parser_port}/query2sql_setting_update/"
    logger.info("url: {}", url)
    payload = {
        "sqlExamplars": sql_examplars,
        "exampleNums": example_nums,
        "isShortcut": is_shortcut,
    }
    headers = {"content-type": "application/json"}
    response = requests.post(url, data=json.dumps(payload), headers=headers)
    logger.info(response.text)


if __name__ == "__main__":
    text2dsl_setting_update(
        LLMPARSER_HOST,
        LLMPARSER_PORT,
        sql_examplars,
        TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM,
        TEXT2DSL_IS_SHORTCUT,
    )

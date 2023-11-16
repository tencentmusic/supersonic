# -*- coding:utf-8 -*-
import json
import os
import sys
from typing import List, Mapping

import requests

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger

from config.config_parse import (TEXT2DSL_EXAMPLE_NUM, TEXT2DSL_FEWSHOTS_NUM, TEXT2DSL_SELF_CONSISTENCY_NUM,
                                LLMPARSER_HOST, LLMPARSER_PORT, TEXT2DSL_IS_SHORTCUT, TEXT2DSL_IS_SELF_CONSISTENCY)
from few_shot_example.sql_examplar import examplars as sql_examplars


def text2sql_agent_setting_update(llm_host:str, llm_port:str,
    sql_examplars:List[Mapping[str, str]], example_nums:int):

    url = f"http://{llm_host}:{llm_port}/text2sql_agent_setting_update/"
    payload = {"sqlExamplars":sql_examplars, "exampleNums":example_nums}
    headers = {'content-type': 'application/json'}
    response = requests.post(url, data=json.dumps(payload), headers=headers)
    logger.info(response.text)


def text2dsl_agent_cs_setting_update(llm_host:str, llm_port:str,
    sql_examplars:List[Mapping[str, str]], example_nums:int, fewshot_nums:int, self_consistency_nums:int):

    url = f"http://{llm_host}:{llm_port}/texg2sqt_cs_agent_setting_update/"
    payload = {"sqlExamplars":sql_examplars, 
               "exampleNums":example_nums, "fewshotNums":fewshot_nums, "selfConsistencyNums":self_consistency_nums}
    headers = {'content-type': 'application/json'}
    response = requests.post(url, data=json.dumps(payload), headers=headers)
    logger.info(response.text)


def text2dsl_agent_wrapper_setting_update(llm_host:str, llm_port:str,
    is_shortcut:bool, is_self_consistency:bool, 
    sql_examplars:List[Mapping[str, str]], example_nums:int, fewshot_nums:int, self_consistency_nums:int):

    sql_ids = [str(i) for i in range(0, len(sql_examplars))]

    url = f"http://{llm_host}:{llm_port}/query2sql_setting_update/"
    payload = {"isShortcut":is_shortcut, "isSelfConsistency":is_self_consistency, 
               "sqlExamplars":sql_examplars, "sqlIds": sql_ids,
               "exampleNums":example_nums, "fewshotNums":fewshot_nums, "selfConsistencyNums":self_consistency_nums}
    headers = {'content-type': 'application/json'}
    response = requests.post(url, data=json.dumps(payload), headers=headers)
    logger.info(response.text)

if __name__ == "__main__":
    text2dsl_agent_wrapper_setting_update(LLMPARSER_HOST,LLMPARSER_PORT,
        TEXT2DSL_IS_SHORTCUT, TEXT2DSL_IS_SELF_CONSISTENCY,
        sql_examplars, TEXT2DSL_EXAMPLE_NUM, TEXT2DSL_FEWSHOTS_NUM, TEXT2DSL_SELF_CONSISTENCY_NUM)
                                          
    

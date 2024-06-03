# -*- coding:utf-8 -*-

import os
import sys
from typing import Any, Dict, List, Union, Mapping

from git import Optional

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger

from auto_cot import auto_cot_run



def transform_sql_example(question:str, current_date:str, table_name:str, field_list: Union[str, List[str]], prior_linkings: Union[str, Mapping[str,str]], prior_exts:str, sql:str=None, terms_list: Optional[List[Dict]] = []):
    db_schema = f"Table: {table_name}, Columns = {field_list}\nForeign_keys: []"

    prior_linkings_pairs = []
    if isinstance(prior_linkings, str):
        prior_linkings = prior_linkings.strip('[]')
        if prior_linkings.strip() == '':
            prior_linkings = []
        else:
            prior_linkings = prior_linkings.split(',')   
        logger.debug(f'prior_linkings: {prior_linkings}') 
    
        for prior_linking in prior_linkings:
            logger.debug(f'prior_linking: {prior_linking}')
            entity_value, entity_type = prior_linking.split('->')
            entity_linking = """’{}‘是一个’{}‘""".format(entity_value, entity_type)
            prior_linkings_pairs.append(entity_linking)
    elif isinstance(prior_linkings, Mapping):
        for entity_value, entity_type in prior_linkings.items():
            entity_linking = """’{}‘是一个’{}‘""".format(entity_value, entity_type)
            prior_linkings_pairs.append(entity_linking)

    prior_linkings_str = '，'.join(prior_linkings_pairs)

    current_data_str = """当前的日期是{}""".format(current_date)

    terms_desc = ''

    if len(terms_list) > 0:
        terms_desc += "相关业务术语："
        for idx, term in enumerate(terms_list):
            
            if (term['description'] is not None and len(term['description']) > 0) and (term['alias'] is not None and len(term['alias']) > 0):
                terms_desc += f"""{idx+1}.<{term['name']}>是业务术语，它通常是指<{term['description']}>，类似的表达还有{term['alias']}；"""
            elif (term['description'] is None or len(term['description']) == 0) and (term['alias'] is not None and len(term['alias']) > 0):
                terms_desc += f"""{idx+1}.<{term['name']}>是业务术语，类似的表达还有{term['alias']}；"""
            elif (term['description'] is not None and len(term['description']) > 0) and (term['alias'] is None or len(term['alias']) == 0):
                terms_desc += f"""{idx+1}.<{term['name']}>是业务术语，它通常是指<{term['description']}>；"""
            else:
                terms_desc += f"""{idx+1}.<{term['name']}>是业务术语；"""
        
        if len(terms_desc) > 0:
            terms_desc = terms_desc[:-1] 
              


    question_augmented = """{question} (补充信息:{prior_linking}。{current_date}。{terms_desc}) (备注: {prior_exts})""".format(question=question, prior_linking=prior_linkings_str, prior_exts=prior_exts, current_date=current_data_str, terms_desc=terms_desc)

    return question_augmented, db_schema, sql


def transform_sql_example_autoCoT_run(examplar_list, min_window_size, max_window_size):
    transformed_sql_examplar_list = []

    for examplar in examplar_list:
        question = examplar['question']
        current_date = examplar['currentDate']
        table_name = examplar['tableName']
        field_list = examplar['fieldsList']
        prior_linkings = examplar['priorSchemaLinks']
        sql = examplar['sql']
        if 'priorExts' not in examplar:
            prior_exts = ''
        else:
            prior_exts = examplar['priorExts']

        question_augmented, db_schema, sql = transform_sql_example(question=question, current_date=current_date, table_name=table_name, field_list=field_list, prior_linkings=prior_linkings, prior_exts=prior_exts, sql=sql)
        logger.debug(f'question_augmented: {question_augmented}')
        logger.debug(f'db_schema: {db_schema}')
        logger.debug(f'sql: {sql}')

        generated_schema_linking_cot, generated_schema_linkings = auto_cot_run(question_augmented, sql, min_window_size, max_window_size)

        transformed_sql_examplar = dict()
        transformed_sql_examplar['question'] = question
        transformed_sql_examplar['questionAugmented'] = question_augmented
        transformed_sql_examplar['modelName'] = table_name
        transformed_sql_examplar['dbSchema'] = db_schema
        transformed_sql_examplar['sql'] = sql
        transformed_sql_examplar['generatedSchemaLinkingCoT'] = generated_schema_linking_cot
        transformed_sql_examplar['generatedSchemaLinkings'] = generated_schema_linkings

        logger.debug(f'transformed_sql_examplar: {transformed_sql_examplar}')

        transformed_sql_examplar_list.append(transformed_sql_examplar)

    return transformed_sql_examplar_list

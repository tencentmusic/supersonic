# -*- coding:utf-8 -*-
from typing import Any, List, Mapping, Optional, Union, Tuple

import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger
from instances.text2vec_instance import emb_func

from sqlglot import parse_one, exp
import numpy as np

def sql2schema_linking(sql: str):
    sql_ast = parse_one(sql)

    fields_raw = []
    table_alias_map = dict()

    literals = []
    fields = []

    for literal in sql_ast.find_all(exp.Literal):
        literals.append(literal.output_name)

    for column in sql_ast.find_all(exp.Column):
        fields_raw.append({
            'column_table_alias': column.table,
            'column_name': column.name,
        })

    for table in sql_ast.find_all(exp.Table):
        if table.alias not in table_alias_map:
            table_alias_map[table.alias] = table.name
    
    logger.debug(f'literals: {literals}')
    logger.debug(f'fields_raw: {fields_raw}')
    logger.debug(f'table_alias_map: {table_alias_map}')

    for field in fields_raw:
        column_table_alias = field['column_table_alias']
        column_name = field['column_name']

        if column_table_alias.strip() == '':
            column_table = ''
            fields.append((column_table, column_name))
        elif column_table_alias in table_alias_map:
            column_table = table_alias_map[column_table_alias]
            fields.append((column_table, column_name))
        elif column_table_alias in table_alias_map.values():
            column_table = column_table_alias
            fields.append((column_table, column_name))
        else:
            logger.error(f'column_table_alias: {column_table_alias} not in table_alias_map: {table_alias_map}')
            raise Exception(f'column_table_alias: {column_table_alias} not in table_alias_map: {table_alias_map}')
        
    return {
        'fields': list(set(fields)),
        'literals': literals
    }


def get_question_slices(question: str, min_window_size: int, max_window_size: int):
    assert min_window_size <= max_window_size
    assert min_window_size > 1
    assert max_window_size < len(question)+1

    question_slices = []
    for i in range(len(question)):
        for j in range(i+1, len(question)+1):
            if j-i >= min_window_size and j-i <= max_window_size:
                question_slices.append(question[i:j])

    return question_slices


def schema_linking_match(fields: List[Tuple[str,str]], question: str, min_window_size: int, max_window_size: int):
    question_slices = get_question_slices(question, min_window_size, max_window_size)
    assert len(question_slices) > 0 
    logger.debug('question_slices_len:{}'.format(len(question_slices)))
    logger.debug(f'question_slices: {question_slices}')

    question_slices_embeddings = emb_func(question_slices)
    fields_embeddings = emb_func([field[1] for field in fields])

    fields_embeddings = np.array(fields_embeddings) # (n_fields, 768)
    question_slices_embeddings = np.array(question_slices_embeddings) # (n_question_slices, 768)
    
    question_slices_embeddings_norm = question_slices_embeddings / np.linalg.norm(question_slices_embeddings, axis=1, keepdims=True) # (n_question_slices, 768)
    question_slices_embeddings_norm_transpose = question_slices_embeddings_norm.T # (768, n_question_slices)

    if len(fields) > 0:
        fields_embeddings_norm = fields_embeddings / np.linalg.norm(fields_embeddings, axis=1, keepdims=True) # (n_fields, 768)
        fields_question_slices_similarity = np.matmul(fields_embeddings_norm, question_slices_embeddings_norm_transpose) # (n_fields, n_question_slices)
        logger.debug('fields_question_slices_similarity_max:{}'.format(np.max(fields_question_slices_similarity, axis=1)))
        fields_question_slices_argmax = np.argmax(fields_question_slices_similarity, axis=1) # (n_fields, )
        logger.debug('fields_question_slices_argmax:{}'.format(fields_question_slices_argmax))

        fields_question_slices_pair = []
        for i in range(len(fields)):
            if fields[i][0]!="":
                fields_question_slices_pair.append((fields[i][0]+'.'+fields[i][1], question_slices[fields_question_slices_argmax[i]]))
            else:
                fields_question_slices_pair.append((fields[i][1], question_slices[fields_question_slices_argmax[i]]))

        logger.debug(f'fields_question_slices_pair: {fields_question_slices_pair}')
    else:
        fields_question_slices_pair = []

    return fields_question_slices_pair


def construct_schema_linking_cot(question:str, fields_question_slices_pair:List[Tuple[str,str]], literals_list:List[str]):    
    cot_intro= """Let’s think step by step. In the question "{question}", we are asked:""".format(question=question)

    schema_linkings_list = []

    fields_cot_template = """"{question_slice}" so we need column = [{field}]"""
    fields_cot_list = []
    for field, question_slice in fields_question_slices_pair:
        fields_cot_list.append(fields_cot_template.format(question_slice=question_slice, field=field))
        schema_linkings_list.append(field)
    fields_cot = '\n'.join(fields_cot_list)

    literals_cot_template = """Based on the tables, columns, and Foreign_keys, The set of possible cell values are = [{literals}]. So the Schema_links are:"""
    literals_cot = literals_cot_template.format(literals=",".join(literals_list))
    
    schema_linkings_list += literals_list
    schema_linking_str = '[' + ",".join(schema_linkings_list) + ']'
    schema_linkings = 'Schema_links: '+ schema_linking_str

    cot = """{cot_intro}""".format(cot_intro=cot_intro)
    if len(fields_cot_list) > 0:
        cot += '\n' + fields_cot

    cot += '\n' + literals_cot
    cot += '\n' + schema_linkings

    return cot, schema_linking_str

def auto_cot_run(question, sql, min_window_size, max_window_size):
    sql_entity = sql2schema_linking(sql)
    logger.debug(f'sql_entity: {sql_entity}')

    fields = sql_entity['fields']
    literals = sql_entity['literals']

    field_linked_pairs = schema_linking_match(fields, question, min_window_size, max_window_size)
    logger.debug(f'field_linked_pairs: {field_linked_pairs}')

    auto_schema_linking_cot, auto_schema_linkings = construct_schema_linking_cot(question, field_linked_pairs, literals)
    logger.debug(f'auto_schema_linking_cot: {auto_schema_linking_cot}')
    logger.debug(f'auto_schema_linkings: {auto_schema_linkings}')

    return auto_schema_linking_cot, auto_schema_linkings


if __name__ == '__main__':
    question = "没有获得过奖项的高校有哪几所？"
    sql = "select 名称 from 高校 where 词条id not in ( select 高校id from 奖项 )"
    min_window_size = 6
    max_window_size = 10

    generated_schema_linking_cot, generated_schema_linkings = auto_cot_run(question, sql, min_window_size, max_window_size)
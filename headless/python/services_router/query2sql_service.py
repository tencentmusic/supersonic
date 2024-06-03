# -*- coding:utf-8 -*-
import os
import sys
import ast
from typing import Any, Mapping

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import APIRouter, HTTPException

from services.s2sql.run import text2sql_agent_router

router = APIRouter()


@router.post("/query2sql")
async def query2sql(query_body: Mapping[str, Any]):
    if 'queryText' not in query_body:
        raise HTTPException(status_code=400, detail="query_text is not in query_body")
    else:
        query_text = query_body['queryText']

    if 'schema' not in query_body:
        raise HTTPException(status_code=400, detail="schema is not in query_body")
    else:
        schema = query_body['schema']
        
    if 'currentDate' not in query_body:
        raise HTTPException(status_code=400, detail="currentDate is not in query_body")
    else:
        current_date = query_body['currentDate']

    if 'linking' not in query_body:
        raise HTTPException(status_code=400, detail="linking is not in query_body")
    else:
        linking = query_body['linking']

    if 'priorExts' not in query_body:
        raise HTTPException(status_code=400, detail="prior_exts is not in query_body")
    else:
        prior_exts = query_body['priorExts']

    if 'filterCondition' not in query_body:
        raise HTTPException(status_code=400, detail="filterCondition is not in query_body")
    else:
        filter_condition = query_body['filterCondition']

    if 'sqlGenType' not in query_body:
        raise HTTPException(status_code=400, detail="sqlGenType is not in query_body")
    else:
        sqlGenType = query_body['sqlGenType']

    if 'llmConfig' in query_body:
        llm_config = ast.literal_eval(str(query_body['llmConfig']))
    else:
        llm_config = None

    dataset_name = schema['dataSetName']
    fields_list = schema['fieldNameList']
    prior_schema_links = {item['fieldValue']:item['fieldName'] for item in linking}
    terms_list = schema['terms']

    resp = await text2sql_agent_router.async_query2sql(question=query_text, filter_condition=filter_condition, 
                                            model_name=dataset_name, fields_list=fields_list,
                                            data_date=current_date, prior_schema_links=prior_schema_links, 
                                            prior_exts=prior_exts, sql_generation_mode=sqlGenType,
                                            llm_config=llm_config, terms_list=terms_list)

    return resp


@router.post("/query2sql_setting_update")
def query2sql_setting_update(query_body: Mapping[str, Any]):
    if 'sqlExamplars' not in query_body:
        raise HTTPException(status_code=400, detail="sqlExamplars is not in query_body")
    else:
        sql_examplars = query_body['sqlExamplars']

    if 'sqlIds' not in query_body:
        raise HTTPException(status_code=400, detail="sqlIds is not in query_body")
    else:
        sql_ids = query_body['sqlIds']    

    if 'exampleNums' not in query_body:
        raise HTTPException(status_code=400, detail="exampleNums is not in query_body")
    else:
        example_nums = query_body['exampleNums']

    if 'fewshotNums' not in query_body:
        raise HTTPException(status_code=400, detail="fewshotNums is not in query_body")
    else:
        fewshot_nums = query_body['fewshotNums']

    if 'selfConsistencyNums' not in query_body:
        raise HTTPException(status_code=400, detail="selfConsistencyNums is not in query_body")
    else:   
        self_consistency_nums = query_body['selfConsistencyNums']

    text2sql_agent_router.update_configs(sql_example_ids=sql_ids, sql_example_units=sql_examplars,
                                        num_examples=example_nums, num_fewshots=fewshot_nums, num_self_consistency=self_consistency_nums)

    return "success"


@router.post("/query2sql_add_examples")
def query2sql_add_examples(query_body: Mapping[str, Any]):
    if 'sqlIds' not in query_body:
        raise HTTPException(status_code=400, detail="sqlIds is not in query_body")
    else:
        sql_ids = query_body['sqlIds']

    if 'sqlExamplars' not in query_body:
        raise HTTPException(status_code=400,
                        detail="sqlExamplars is not in query_body")
    else:
        sql_examplars = query_body['sqlExamplars']

    text2sql_agent_router.add_examples(sql_example_ids=sql_ids, sql_example_units=sql_examplars)

    return "success"


@router.post("/query2sql_update_examples")
def query2sql_update_examples(query_body: Mapping[str, Any]):
    if 'sqlIds' not in query_body:
        raise HTTPException(status_code=400, detail="sqlIds is not in query_body")
    else:
        sql_ids = query_body['sqlIds']

    if 'sqlExamplars' not in query_body:
        raise HTTPException(status_code=400,
                        detail="sqlExamplars is not in query_body")
    else:
        sql_examplars = query_body['sqlExamplars']

    text2sql_agent_router.update_examples(sql_example_ids=sql_ids, sql_example_units=sql_examplars)

    return "success"


@router.post("/query2sql_delete_examples")
def query2sql_delete_examples(query_body: Mapping[str, Any]):
    if 'sqlIds' not in query_body:
        raise HTTPException(status_code=400, detail="sqlIds is not in query_body")
    else:
        sql_ids = query_body['sqlIds']

    text2sql_agent_router.delete_examples(sql_example_ids=sql_ids)

    return "success"

@router.post("/query2sql_get_examples")
def query2sql_get_examples(query_body: Mapping[str, Any]):
    if 'sqlIds' not in query_body:
        raise HTTPException(status_code=400, detail="sqlIds is not in query_body")
    else:
        sql_ids = query_body['sqlIds']

    examples = text2sql_agent_router.get_examples(sql_example_ids=sql_ids)

    return examples

@router.get("/query2sql_count_examples")
def query2sql_count_examples():
    examples_cnt = text2sql_agent_router.count_examples()

    return examples_cnt


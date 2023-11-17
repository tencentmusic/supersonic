# -*- coding:utf-8 -*-
import os
import sys
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import APIRouter, Depends, HTTPException

from services.sql.run import text2sql_agent_router

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

    model_name = schema['modelName']
    fields_list = schema['fieldNameList']
    prior_schema_links = {item['fieldValue']:item['fieldName'] for item in linking}

    resp = await text2sql_agent_router.async_query2sql(query_text=query_text, filter_condition=filter_condition, model_name=model_name, fields_list=fields_list,
                                            data_date=current_date, prior_schema_links=prior_schema_links, prior_exts=prior_exts)

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

    if 'isShortcut' not in query_body:
        raise HTTPException(status_code=400, detail="isShortcut is not in query_body")
    else:
        is_shortcut = query_body['isShortcut']

    if 'isSelfConsistency' not in query_body:
        raise HTTPException(status_code=400, detail="isSelfConsistency is not in query_body")
    else:
        is_self_consistency = query_body['isSelfConsistency']

    text2sql_agent_router.update_configs(is_shortcut=is_shortcut, is_self_consistency=is_self_consistency, 
                                        sql_example_ids=sql_ids, sql_example_units=sql_examplars,
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

    text2sql_agent_router.sql_agent.add_examples(sql_example_ids=sql_ids, sql_example_units=sql_examplars)
    text2sql_agent_router.sql_agent_cs.add_examples(sql_example_ids=sql_ids, sql_example_units=sql_examplars)

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

    text2sql_agent_router.sql_agent.update_examples(sql_example_ids=sql_ids, sql_example_units=sql_examplars)
    text2sql_agent_router.sql_agent_cs.update_examples(sql_example_ids=sql_ids, sql_example_units=sql_examplars)

    return "success"


@router.post("/query2sql_delete_examples")
def query2sql_delete_examples(query_body: Mapping[str, Any]):
    if 'sqlIds' not in query_body:
        raise HTTPException(status_code=400, detail="sqlIds is not in query_body")
    else:
        sql_ids = query_body['sqlIds']

    text2sql_agent_router.sql_agent.delete_examples(sql_example_ids=sql_ids)
    text2sql_agent_router.sql_agent_cs.delete_examples(sql_example_ids=sql_ids)

    return "success"


@router.get("/query2sql_count_examples")
def query2sql_count_examples():
    sql_agent_examples_cnt = text2sql_agent_router.sql_agent.count_examples()
    sql_agent_cs_examples_cnt = text2sql_agent_router.sql_agent_cs.count_examples()

    assert sql_agent_examples_cnt == sql_agent_cs_examples_cnt

    return sql_agent_examples_cnt


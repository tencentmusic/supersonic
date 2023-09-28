# -*- coding:utf-8 -*-
import os
import sys
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import APIRouter, Depends, HTTPException

from services.sql.run import text2sql_agent

router = APIRouter()


@router.post("/query2sql/")
def din_query2sql(query_body: Mapping[str, Any]):
    if "queryText" not in query_body:
        raise HTTPException(status_code=400, detail="query_text is not in query_body")
    else:
        query_text = query_body["queryText"]

    if "schema" not in query_body:
        raise HTTPException(status_code=400, detail="schema is not in query_body")
    else:
        schema = query_body["schema"]

    if "currentDate" not in query_body:
        raise HTTPException(status_code=400, detail="currentDate is not in query_body")
    else:
        current_date = query_body["currentDate"]

    if "linking" not in query_body:
        linking = None
    else:
        linking = query_body["linking"]

    resp = text2sql_agent.query2sql_run(
        query_text=query_text, schema=schema, current_date=current_date, linking=linking
    )

    return resp


@router.post("/query2sql_setting_update/")
def query2sql_setting_update(query_body: Mapping[str, Any]):
    if "sqlExamplars" not in query_body:
        raise HTTPException(status_code=400, detail="sqlExamplars is not in query_body")
    else:
        sql_examplars = query_body["sqlExamplars"]

    if "exampleNums" not in query_body:
        raise HTTPException(status_code=400, detail="exampleNums is not in query_body")
    else:
        example_nums = query_body["exampleNums"]

    if "isShortcut" not in query_body:
        raise HTTPException(status_code=400, detail="isShortcut is not in query_body")
    else:
        is_shortcut = query_body["isShortcut"]

    text2sql_agent.update_examples(
        sql_examples=sql_examplars, example_nums=example_nums, is_shortcut=is_shortcut
    )

    return "success"

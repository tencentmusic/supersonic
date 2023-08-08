# -*- coding:utf-8 -*-
import os
import logging
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from typing import Any, List, Mapping, Optional, Union

from fastapi import FastAPI, HTTPException

from sql.run import query2sql

from preset_retrieval.run import preset_query_retrieval_run, collection as preset_query_collection
from preset_retrieval.preset_query_db import (add2preset_query_collection, update_preset_query_collection, 
                                              empty_preset_query_collection, delete_preset_query_by_ids, 
                                              update_preset_query_collection, get_preset_query_by_ids,
                                              preset_query_collection_size)

from plugin_call.run import plugin_selection_run

app = FastAPI()


@app.post("/query2sql/")
async def din_query2sql(query_body: Mapping[str, Any]):
  if 'queryText' not in query_body:
    raise HTTPException(status_code=400,
                        detail="query_text is not in query_body")
  else:
    query_text = query_body['queryText']

  if 'schema' not in query_body:
    raise HTTPException(status_code=400, detail="schema is not in query_body")
  else:
    schema = query_body['schema']

  resp = query2sql(query_text=query_text, schema=schema)

  return resp


@app.post("/preset_query_retrival/")
async def preset_query_retrival(query_text_list: List[str], n_results: int = 5):
    parsed_retrieval_res_format = preset_query_retrieval_run(preset_query_collection, query_text_list, n_results)

    return parsed_retrieval_res_format


@app.post("/preset_query_add/")
async def preset_query_add(preset_info_list: List[Mapping[str, str]]):
    preset_queries = []
    preset_query_ids = []

    for preset_info in preset_info_list:
        preset_queries.append(preset_info['preset_query'])
        preset_query_ids.append(preset_info['preset_query_id'])

    add2preset_query_collection(collection=preset_query_collection,
                              preset_queries=preset_queries,
                              preset_query_ids=preset_query_ids)

    return "success"

@app.post("/preset_query_update/")
async def preset_query_update(preset_info_list: List[Mapping[str, str]]):
    preset_queries = []
    preset_query_ids = []

    for preset_info in preset_info_list:
        preset_queries.append(preset_info['preset_query'])
        preset_query_ids.append(preset_info['preset_query_id'])

    update_preset_query_collection(collection=preset_query_collection,
                              preset_queries=preset_queries,
                              preset_query_ids=preset_query_ids)

    return "success"


@app.get("/preset_query_empty/")
async def preset_query_empty():
    empty_preset_query_collection(collection=preset_query_collection)

    return "success"

@app.post("/preset_delete_by_ids/")
async def preset_delete_by_ids(preset_query_ids: List[str]):
    delete_preset_query_by_ids(collection=preset_query_collection, preset_query_ids=preset_query_ids)

    return "success"

@app.post("/preset_get_by_ids/")
async def preset_get_by_ids(preset_query_ids: List[str]):
    preset_queries = get_preset_query_by_ids(collection=preset_query_collection, preset_query_ids=preset_query_ids)

    return preset_queries

@app.get("/preset_query_size/")
async def preset_query_size():
    size = preset_query_collection_size(collection=preset_query_collection)

    return size

@app.post("/plugin_selection/")
async def tool_selection(query_body: Mapping[str, Any]):
    if 'queryText' not in query_body:
        raise HTTPException(status_code=400, detail="query_text is not in query_body")
    else:
        query_text = query_body['queryText']

    if 'pluginConfigs' not in query_body:
        raise HTTPException(status_code=400, detail="pluginConfigs is not in query_body")
    else:
        plugin_configs = query_body['pluginConfigs']

    resp = plugin_selection_run(query_text=query_text, plugin_configs=plugin_configs)

    return resp

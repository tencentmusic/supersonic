# -*- coding:utf-8 -*-
import os
import sys
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import APIRouter, Depends, HTTPException

from services.query_retrieval.run import preset_query_retriever

router = APIRouter()

@router.post("/preset_query_retrival")
def preset_query_retrival(query_text_list: List[str], n_results: int = 5):
    parsed_retrieval_res_format = preset_query_retriever.retrieval_query_run(query_texts_list=query_text_list, filter_condition=None, n_results=n_results)

    return parsed_retrieval_res_format


@router.post("/preset_query_add")
def preset_query_add(preset_info_list: List[Mapping[str, str]]):
    preset_queries = []
    preset_query_ids = []

    for preset_info in preset_info_list:
        preset_queries.append(preset_info['preset_query'])
        preset_query_ids.append(preset_info['preset_query_id'])

    preset_query_retriever.add_queries(query_text_list=preset_queries, query_id_list=preset_query_ids, metadatas=None)

    return "success"

@router.post("/preset_query_update")
def preset_query_update(preset_info_list: List[Mapping[str, str]]):
    preset_queries = []
    preset_query_ids = []

    for preset_info in preset_info_list:
        preset_queries.append(preset_info['preset_query'])
        preset_query_ids.append(preset_info['preset_query_id'])

    preset_query_retriever.update_queries(query_text_list=preset_queries, query_id_list=preset_query_ids, metadatas=None)

    return "success"


@router.get("/preset_query_empty")
def preset_query_empty():
    preset_query_retriever.empty_query_collection()

    return "success"

@router.post("/preset_delete_by_ids")
def preset_delete_by_ids(preset_query_ids: List[str]):
    preset_query_retriever.delete_queries_by_ids(preset_query_ids)

    return "success"

@router.post("/preset_get_by_ids")
def preset_get_by_ids(preset_query_ids: List[str]):
    preset_queries = preset_query_retriever.get_query_by_ids(preset_query_ids)

    return preset_queries

@router.get("/preset_query_size")
def preset_query_size():
    size = preset_query_retriever.get_query_size()

    return size

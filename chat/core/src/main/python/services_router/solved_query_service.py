# -*- coding:utf-8 -*-
import os
import sys
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import APIRouter, Depends, HTTPException

from services.query_retrieval.run import solved_query_retriever

router = APIRouter()

@router.post("/solved_query_retrival")
def solved_query_retrival(query_info: Mapping[str, Any], n_results: int = 5):
    query_texts_list = query_info['queryTextsList']
    filter_condition = query_info['filterCondition']

    parsed_retrieval_res_format = solved_query_retriever.retrieval_query_run(query_texts_list=query_texts_list, 
                                                                                filter_condition=filter_condition, 
                                                                                n_results=n_results)

    return parsed_retrieval_res_format


@router.post("/solved_query_add")
def add_solved_queries(sovled_query_info_list: List[Mapping[str, Any]]):
    queries = []
    query_ids = []
    metadatas = []

    for sovled_query_info in sovled_query_info_list:
        queries.append(sovled_query_info['query'])
        query_ids.append(sovled_query_info['query_id'])
        metadatas.append(sovled_query_info['metadata'])

    solved_query_retriever.add_queries(query_text_list=queries, query_id_list=query_ids, metadatas=metadatas)

    return "success"

@router.post("/solved_query_update")
def solved_query_update(sovled_query_info_list: List[Mapping[str, Any]]):
    queries = []
    query_ids = []
    metadatas = []

    for sovled_query_info in sovled_query_info_list:
        queries.append(sovled_query_info['query'])
        query_ids.append(sovled_query_info['query_id'])
        metadatas.append(sovled_query_info['metadata'])

    solved_query_retriever.update_queries(query_text_list=queries, query_id_list=query_ids, metadatas=metadatas)

    return "success"


@router.get("/solved_query_empty")
def solved_query_empty():
    solved_query_retriever.empty_query_collection()

    return "success"

@router.post("/solved_query_delete_by_ids")
def solved_query_delete_by_ids(query_ids: List[str]):
    solved_query_retriever.delete_queries_by_ids(query_ids=query_ids)

    return "success"

@router.post("/solved_query_get_by_ids")
def solved_query_get_by_ids(query_ids: List[str]):
    queries = solved_query_retriever.get_query_by_ids(query_ids=query_ids)

    return queries

@router.get("/solved_query_size")
def solved_query_size():
    size = solved_query_retriever.get_query_size()

    return size

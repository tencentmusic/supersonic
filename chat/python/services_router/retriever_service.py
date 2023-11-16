# -*- coding:utf-8 -*-
import os
import sys
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import APIRouter, Depends, HTTPException

from services.query_retrieval.run import collection_manager
from services.query_retrieval.retriever import ChromaCollectionRetriever

router = APIRouter()

@router.get("/list_collections")
def list_collections():
    collections = collection_manager.list_collections()

    return collections

@router.get("/create_collection")
def create_collection(collection_name: str):
    collection_manager.create_collection(collection_name)

    return "success"

@router.get("/delete_collection")
def delete_collection(collection_name: str):
    collection_manager.delete_collection(collection_name)

    return "success"

@router.get("/get_collection")
def get_collection(collection_name: str):
    collection = collection_manager.get_collection(collection_name)

    return collection

@router.get("/get_or_create_collection")
def get_or_create_collection(collection_name: str):
    collection = collection_manager.get_or_create_collection(collection_name)

    return collection

@router.post("/add_query")
def query_add(collection_name:str, query_info_list: List[Mapping[str, Any]]):
    queries = []
    query_ids = []
    metadatas = []
    embeddings = []

    for query_info in query_info_list:
        queries.append(query_info['query'])
        query_ids.append(query_info['queryId'])
        metadatas.append(query_info['metadata'])
        embeddings.append(query_info['queryEmbedding']) 

    if None in embeddings:
        embeddings = None
    if None in queries:
        queries = None

    if embeddings is None and queries is None:
        raise HTTPException(status_code=400, detail="query and queryEmbedding are None")
    if embeddings is not None and queries is not None:
        raise HTTPException(status_code=400, detail="query and queryEmbedding are not None")

    query_collection = collection_manager.get_or_create_collection(collection_name=collection_name)
    query_retriever = ChromaCollectionRetriever(collection=query_collection)
    query_retriever.add_queries(query_text_list=queries, query_id_list=query_ids, metadatas=metadatas, embeddings=embeddings)

    return "success"

@router.post("/update_query")
def update_query(collection_name:str, query_info_list: List[Mapping[str, Any]]):
    queries = []
    query_ids = []
    metadatas = []
    embeddings = []

    for query_info in query_info_list:
        queries.append(query_info['query'])
        query_ids.append(query_info['queryId'])
        metadatas.append(query_info['metadata'])
        embeddings.append(query_info['queryEmbedding']) 

    if None in embeddings:
        embeddings = None
    if None in queries:
        queries = None

    if embeddings is None and queries is None:
        raise HTTPException(status_code=400, detail="query and queryEmbedding are None")
    if embeddings is not None and queries is not None:
        raise HTTPException(status_code=400, detail="query and queryEmbedding are not None")

    query_collection = collection_manager.get_or_create_collection(collection_name=collection_name)
    query_retriever = ChromaCollectionRetriever(collection=query_collection)
    query_retriever.update_queries(query_text_list=queries, query_id_list=query_ids, metadatas=metadatas, embeddings=embeddings)

    return "success"

@router.get("/empty_query")
def empty_query(collection_name:str):
    query_collection = collection_manager.get_or_create_collection(collection_name=collection_name)
    query_retriever = ChromaCollectionRetriever(collection=query_collection)
    query_retriever.empty_query_collection()

    return "success"


@router.post("/delete_query_by_ids")
def delete_query_by_ids(collection_name:str, query_ids: List[str]):
    query_collection = collection_manager.get_or_create_collection(collection_name=collection_name)
    query_retriever = ChromaCollectionRetriever(collection=query_collection)
    query_retriever.delete_queries_by_ids(query_ids=query_ids)

    return "success"

@router.post("/get_query_by_ids")
def get_query_by_ids(collection_name:str, query_ids: List[str]):
    query_collection = collection_manager.get_or_create_collection(collection_name=collection_name)
    query_retriever = ChromaCollectionRetriever(collection=query_collection)
    queries = query_retriever.get_query_by_ids(query_ids=query_ids)

    return queries

@router.get("/query_size")
def query_size(collection_name:str):
    query_collection = collection_manager.get_or_create_collection(collection_name=collection_name)
    query_retriever = ChromaCollectionRetriever(collection=query_collection)
    size = query_retriever.get_query_size()

    return size

@router.post("/retrieve_query")
def retrieve_query(collection_name:str, query_info: Mapping[str, Any], n_results:int=10):
    query_collection = collection_manager.get_or_create_collection(collection_name=collection_name)
    query_retriever = ChromaCollectionRetriever(collection=query_collection)

    query_texts_list = query_info['queryTextsList']
    qeuery_embeddings = query_info['queryEmbeddings']
    filter_condition = query_info['filterCondition']

    if query_texts_list is None and qeuery_embeddings is None:
        raise HTTPException(status_code=400, detail="query and queryEmbedding are None")
    if query_texts_list is not None and qeuery_embeddings is not None:
        raise HTTPException(status_code=400, detail="query and queryEmbedding are not None")

    parsed_retrieval_res_format = query_retriever.retrieval_query_run(query_texts_list=query_texts_list,
                                                        query_embeddings=qeuery_embeddings,
                                                        filter_condition=filter_condition,
                                                        n_results=n_results)

    return parsed_retrieval_res_format
# -*- coding:utf-8 -*-

import os
import sys
from typing import List

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from util.logging_utils import logger
from chromadb.api import Collection

from preset_query_db import (
    query2preset_query_collection,
    parse_retrieval_preset_query,
    preset_query_retrieval_format,
    preset_query_collection_size,
)

from util.text2vec import Text2VecEmbeddingFunction

from config.config_parse import PRESET_QUERY_COLLECTION_NAME
from util.chromadb_instance import client


emb_func = Text2VecEmbeddingFunction()

collection = client.get_or_create_collection(
    name=PRESET_QUERY_COLLECTION_NAME,
    embedding_function=emb_func,
    metadata={"hnsw:space": "cosine"},
)  # Get a collection object from an existing collection, by name. If it doesn't exist, create it.

logger.info("init_preset_query_collection_size: {}", preset_query_collection_size(collection))


def preset_query_retrieval_run(
    collection: Collection, query_texts_list: List[str], n_results: int = 5
):
    retrieval_res = query2preset_query_collection(
        collection=collection, query_texts=query_texts_list, n_results=n_results
    )

    parsed_retrieval_res = parse_retrieval_preset_query(retrieval_res)
    parsed_retrieval_res_format = preset_query_retrieval_format(
        query_texts_list, parsed_retrieval_res
    )

    logger.info("parsed_retrieval_res_format: {}", parsed_retrieval_res_format)

    return parsed_retrieval_res_format

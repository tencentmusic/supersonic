# -*- coding:utf-8 -*-

import os
import sys
import uuid
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))


from chromadb.api import Collection, Documents, Embeddings

from langchain.llms import OpenAI

from preset_query_db import (get_ids, add2preset_query_collection, 
                            query2preset_query_collection, parse_retrieval_preset_query, 
                            preset_query_retrieval_format, empty_preset_query_collection, preset_query_collection_size)

from util.text2vec import Text2VecEmbeddingFunction

from run_config import CHROMA_DB_PERSIST_PATH, PRESET_QUERY_COLLECTION_NAME
from util.chromadb_instance import client 


emb_func = Text2VecEmbeddingFunction()

collection = client.get_or_create_collection(name=PRESET_QUERY_COLLECTION_NAME, 
                                            embedding_function=emb_func,
                                            metadata={"hnsw:space": "cosine"}
                                            ) # Get a collection object from an existing collection, by name. If it doesn't exist, create it.

print("init_preset_query_collection_size: ", preset_query_collection_size(collection))


def preset_query_retrieval_run(collection:Collection, query_texts_list:List[str], n_results:int=5):
    retrieval_res = query2preset_query_collection(collection=collection, 
                              query_texts=query_texts_list,
                              n_results=n_results)

    parsed_retrieval_res = parse_retrieval_preset_query(retrieval_res)
    parsed_retrieval_res_format = preset_query_retrieval_format(query_texts_list, parsed_retrieval_res)

    print('parsed_retrieval_res_format: ', parsed_retrieval_res_format)

    return parsed_retrieval_res_format

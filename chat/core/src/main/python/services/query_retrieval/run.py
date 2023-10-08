# -*- coding:utf-8 -*-

import os
import sys
import uuid
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from util.logging_utils import logger

import chromadb
from chromadb.config import Settings
from chromadb.api import Collection, Documents, Embeddings

from util.text2vec import Text2VecEmbeddingFunction

from config.config_parse import SOLVED_QUERY_COLLECTION_NAME, PRESET_QUERY_COLLECTION_NAME
from util.chromadb_instance import (client, 
                                    get_chroma_collection_size, query_chroma_collection, 
                                    parse_retrieval_chroma_collection_query, chroma_collection_query_retrieval_format,
                                    get_chroma_collection_by_ids, get_chroma_collection_size,
                                    add_chroma_collection, update_chroma_collection, delete_chroma_collection_by_ids,
                                    empty_chroma_collection_2)

emb_func = Text2VecEmbeddingFunction()

solved_query_collection = client.get_or_create_collection(name=SOLVED_QUERY_COLLECTION_NAME, 
                                            embedding_function=emb_func,
                                            metadata={"hnsw:space": "cosine"}
                                            ) # Get a collection object from an existing collection, by name. If it doesn't exist, create it.
logger.info("init_solved_query_collection_size: {}", get_chroma_collection_size(solved_query_collection))


preset_query_collection = client.get_or_create_collection(name=PRESET_QUERY_COLLECTION_NAME,
                                            embedding_function=emb_func,
                                            metadata={"hnsw:space": "cosine"}
                                            )
logger.info("init_preset_query_collection_size: {}", get_chroma_collection_size(preset_query_collection))

class ChromaCollectionRetriever(object):
    def __init__(self, collection:Collection):
        self.collection = collection

    def retrieval_query_run(self, query_texts_list:List[str], 
                                   filter_condition:Mapping[str,str]=None, n_results:int=5):
        
        retrieval_res = query_chroma_collection(self.collection, query_texts_list,
                                                filter_condition, n_results)

        parsed_retrieval_res = parse_retrieval_chroma_collection_query(retrieval_res)
        parsed_retrieval_res_format = chroma_collection_query_retrieval_format(query_texts_list, parsed_retrieval_res)

        logger.info('parsed_retrieval_res_format: {}', parsed_retrieval_res_format)

        return parsed_retrieval_res_format

    def get_query_by_ids(self, query_ids:List[str]):
        queries = get_chroma_collection_by_ids(self.collection, query_ids)
        return queries
    
    def get_query_size(self):
        return get_chroma_collection_size(self.collection)
    
    def add_queries(self, query_text_list:List[str], 
                           query_id_list:List[str], metadatas:List[Mapping[str, str]]=None):
        add_chroma_collection(self.collection, query_text_list, query_id_list, metadatas)
        return True
    
    def update_queries(self, query_text_list:List[str], 
                              query_id_list:List[str], metadatas:List[Mapping[str, str]]=None):
        update_chroma_collection(self.collection, query_text_list, query_id_list, metadatas)
        return True
    
    def delete_queries_by_ids(self, query_ids:List[str]):
        delete_chroma_collection_by_ids(self.collection, query_ids)
        return True
    
    def empty_query_collection(self):
        self.collection = empty_chroma_collection_2(self.collection)

        return True
    

solved_query_retriever = ChromaCollectionRetriever(solved_query_collection)
preset_query_retriever = ChromaCollectionRetriever(preset_query_collection)

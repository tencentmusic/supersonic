# -*- coding:utf-8 -*-

import os
import sys
import uuid
from typing import Any, List, Mapping, Optional, Union

import chromadb
from chromadb import Client
from chromadb.config import Settings
from chromadb.api import Collection, Documents, Embeddings
from chromadb.api.types import CollectionMetadata

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger
from utils.chromadb_utils import (get_chroma_collection_size, query_chroma_collection, 
                                            parse_retrieval_chroma_collection_query, chroma_collection_query_retrieval_format,
                                            get_chroma_collection_by_ids, get_chroma_collection_size,
                                            add_chroma_collection, update_chroma_collection, delete_chroma_collection_by_ids,
                                            empty_chroma_collection_2)

from utils.text2vec import Text2VecEmbeddingFunction

class ChromaCollectionRetriever(object):
    def __init__(self, collection:Collection):
        self.collection = collection

    def retrieval_query_run(self, query_texts_list:List[str]=None, query_embeddings:Embeddings=None,
                                   filter_condition:Mapping[str,str]=None, n_results:int=5):
        
        retrieval_res = query_chroma_collection(self.collection, query_texts_list, query_embeddings,
                                                filter_condition, n_results)

        parsed_retrieval_res = parse_retrieval_chroma_collection_query(retrieval_res)
        logger.debug('parsed_retrieval_res: {}', parsed_retrieval_res)
        parsed_retrieval_res_format = chroma_collection_query_retrieval_format(query_texts_list, query_embeddings, parsed_retrieval_res)
        logger.debug('parsed_retrieval_res_format: {}', parsed_retrieval_res_format)

        return parsed_retrieval_res_format

    def get_query_by_ids(self, query_ids:List[str]):
        queries = get_chroma_collection_by_ids(self.collection, query_ids)
        return queries
    
    def get_query_size(self):
        return get_chroma_collection_size(self.collection)
    
    def add_queries(self, query_text_list:List[str], 
                           query_id_list:List[str], 
                           metadatas:List[Mapping[str, str]]=None, 
                           embeddings:Embeddings=None):
        add_chroma_collection(self.collection, query_text_list, query_id_list, metadatas, embeddings)
        return True
    
    def update_queries(self, query_text_list:List[str], 
                              query_id_list:List[str], 
                              metadatas:List[Mapping[str, str]]=None, 
                              embeddings:Embeddings=None):
        update_chroma_collection(self.collection, query_text_list, query_id_list, metadatas, embeddings)
        return True
    
    def delete_queries_by_ids(self, query_ids:List[str]):
        delete_chroma_collection_by_ids(self.collection, query_ids)
        return True
    
    def empty_query_collection(self):
        self.collection = empty_chroma_collection_2(self.collection)

        return True

class CollectionManager(object):
    def __init__(self, chroma_client:Client, embedding_func: Text2VecEmbeddingFunction, collection_meta: Optional[CollectionMetadata] = None):
        self.chroma_client = chroma_client
        self.embedding_func = embedding_func
        self.collection_meta = collection_meta

    def list_collections(self):
        collection_list = self.chroma_client.list_collections()
        return collection_list

    def get_collection(self, collection_name:str):
        collection = self.chroma_client.get_collection(name=collection_name, embedding_function=self.embedding_func)
        return collection
    
    def create_collection(self, collection_name:str):
        collection = self.chroma_client.create_collection(name=collection_name, embedding_function=self.embedding_func, metadata=self.collection_meta)
        return collection
    
    def get_or_create_collection(self, collection_name:str):
        collection = self.chroma_client.get_or_create_collection(name=collection_name, embedding_function=self.embedding_func, metadata=self.collection_meta)
        return collection

    def delete_collection(self, collection_name:str):
        self.chroma_client.delete_collection(collection_name)
        return True
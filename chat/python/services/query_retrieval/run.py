# -*- coding:utf-8 -*-

import os
import sys
import uuid
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger

import chromadb
from chromadb.config import Settings
from chromadb.api import Collection, Documents, Embeddings

from utils.text2vec import Text2VecEmbeddingFunction
from instances.chromadb_instance import client

from config.config_parse import SOLVED_QUERY_COLLECTION_NAME, PRESET_QUERY_COLLECTION_NAME
from retriever import ChromaCollectionRetriever, CollectionManager


emb_func = Text2VecEmbeddingFunction()

collection_manager = CollectionManager(chroma_client=client, embedding_func=emb_func
                                        ,collection_meta={"hnsw:space": "cosine"})

solved_query_collection = collection_manager.get_or_create_collection(collection_name=SOLVED_QUERY_COLLECTION_NAME)
preset_query_collection = collection_manager.get_or_create_collection(collection_name=PRESET_QUERY_COLLECTION_NAME)


solved_query_retriever = ChromaCollectionRetriever(solved_query_collection)
preset_query_retriever = ChromaCollectionRetriever(preset_query_collection)

logger.info("init_solved_query_collection_size: {}".format(solved_query_retriever.get_query_size()))
logger.info("init_preset_query_collection_size: {}".format(preset_query_retriever.get_query_size()))

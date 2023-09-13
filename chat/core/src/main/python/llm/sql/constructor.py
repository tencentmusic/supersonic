# -*- coding:utf-8 -*-
from typing import Any, List, Mapping, Optional, Union
import os
import sys
import time

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from langchain.prompts.few_shot import FewShotPromptTemplate
from langchain.prompts import PromptTemplate
from langchain.vectorstores import Chroma
from langchain.embeddings import OpenAIEmbeddings, HuggingFaceEmbeddings
from langchain.prompts.example_selector import SemanticSimilarityExampleSelector

import chromadb
from chromadb.config import Settings
from chromadb.api import Collection, Documents, Embeddings

from few_shot_example.sql_exampler import examplars as sql_examplars
from util.text2vec import Text2VecEmbeddingFunction, hg_embedding
from util.chromadb_instance import client as chromadb_client, empty_chroma_collection_2
from run_config import TEXT2DSL_COLLECTION_NAME, TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM


def reload_sql_example_collection(vectorstore:Chroma, 
                                sql_examplars:List[Mapping[str, str]],
                                schema_linking_example_selector:SemanticSimilarityExampleSelector,
                                sql_example_selector:SemanticSimilarityExampleSelector, 
                                example_nums:int
                                ):
    print("original sql_examples_collection size:", vectorstore._collection.count())
    new_collection = empty_chroma_collection_2(collection=vectorstore._collection)
    vectorstore._collection = new_collection

    print("emptied sql_examples_collection size:", vectorstore._collection.count())

    schema_linking_example_selector = SemanticSimilarityExampleSelector(vectorstore=sql_examples_vectorstore, k=example_nums,
                                                        input_keys=["question"], 
                                                        example_keys=["table_name", "fields_list", "prior_schema_links", "question", "analysis", "schema_links"])

    sql_example_selector = SemanticSimilarityExampleSelector(vectorstore=sql_examples_vectorstore, k=example_nums,
                                                        input_keys=["question"],
                                                        example_keys=["question", "current_date", "table_name", "schema_links", "sql"])

    for example in sql_examplars:
        schema_linking_example_selector.add_example(example)

    print("reloaded sql_examples_collection size:", vectorstore._collection.count())

    return vectorstore, schema_linking_example_selector, sql_example_selector


sql_examples_vectorstore = Chroma(collection_name=TEXT2DSL_COLLECTION_NAME, 
                    embedding_function=hg_embedding,
                    client=chromadb_client)

example_nums = TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM

schema_linking_example_selector = SemanticSimilarityExampleSelector(vectorstore=sql_examples_vectorstore, k=example_nums,
                                                        input_keys=["question"], 
                                                        example_keys=["table_name", "fields_list", "prior_schema_links", "question", "analysis", "schema_links"])

sql_example_selector = SemanticSimilarityExampleSelector(vectorstore=sql_examples_vectorstore, k=example_nums,
                                                        input_keys=["question"],
                                                        example_keys=["question", "current_date", "table_name", "schema_links", "sql"])

if sql_examples_vectorstore._collection.count() > 0:
    print("examples already in sql_vectorstore")
    print("init sql_vectorstore size:", sql_examples_vectorstore._collection.count()) 
    if sql_examples_vectorstore._collection.count() < len(sql_examplars):
        print("sql_examplars size:", len(sql_examplars))
        sql_examples_vectorstore, schema_linking_example_selector, sql_example_selector = reload_sql_example_collection(sql_examples_vectorstore, sql_examplars, schema_linking_example_selector, sql_example_selector, example_nums)
        print("added sql_vectorstore size:", sql_examples_vectorstore._collection.count())
else:
    sql_examples_vectorstore, schema_linking_example_selector, sql_example_selector = reload_sql_example_collection(sql_examples_vectorstore, sql_examplars, schema_linking_example_selector, sql_example_selector, example_nums)
    print("added sql_vectorstore size:", sql_examples_vectorstore._collection.count())


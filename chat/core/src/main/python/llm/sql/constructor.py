# -*- coding:utf-8 -*-
import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from langchain.prompts.few_shot import FewShotPromptTemplate
from langchain.prompts import PromptTemplate
from langchain.vectorstores import Chroma
from langchain.embeddings import OpenAIEmbeddings, HuggingFaceEmbeddings
from langchain.prompts.example_selector import SemanticSimilarityExampleSelector

import chromadb
from chromadb.config import Settings

from few_shot_example.sql_exampler import examplars as din_sql_examplars
from util.text2vec import Text2VecEmbeddingFunction, hg_embedding
from util.chromadb_instance import client as chromadb_client


from run_config import TEXT2DSL_COLLECTION_NAME


vectorstore = Chroma(collection_name=TEXT2DSL_COLLECTION_NAME, 
                    embedding_function=hg_embedding,
                    client=chromadb_client)

example_nums = 15

schema_linking_example_selector = SemanticSimilarityExampleSelector(vectorstore=vectorstore, k=example_nums,
                                                        input_keys=["question"], 
                                                        example_keys=["table_name", "fields_list", "prior_schema_links", "question", "analysis", "schema_links"])

sql_example_selector = SemanticSimilarityExampleSelector(vectorstore=vectorstore, k=example_nums,
                                                        input_keys=["question"],
                                                        example_keys=["question", "current_date", "table_name", "schema_links", "sql"])

if vectorstore._collection.count() > 0:
    print("examples already in din_sql_vectorstore")
    print("init din_sql_vectorstore size:", vectorstore._collection.count()) 
    if vectorstore._collection.count() < len(din_sql_examplars):
        print("din_sql_examplars size:", len(din_sql_examplars))
        vectorstore._collection.delete()
        print("empty din_sql_vectorstore")
        for example in din_sql_examplars:
            schema_linking_example_selector.add_example(example)
        print("added din_sql_vectorstore size:", vectorstore._collection.count())
else:
    for example in din_sql_examplars:
        schema_linking_example_selector.add_example(example)

    print("added din_sql_vectorstore size:", vectorstore._collection.count())

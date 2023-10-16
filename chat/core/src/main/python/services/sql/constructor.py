# -*- coding:utf-8 -*-
import os
import sys
from typing import List, Mapping

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from util.logging_utils import logger

from langchain.vectorstores import Chroma
from langchain.prompts.example_selector import SemanticSimilarityExampleSelector

from few_shot_example.sql_exampler import examplars as sql_examplars
from util.text2vec import hg_embedding
from util.chromadb_instance import client as chromadb_client, empty_chroma_collection_2
from config.config_parse import TEXT2DSL_COLLECTION_NAME, TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM


def reload_sql_example_collection(
    vectorstore: Chroma,
    sql_examplars: List[Mapping[str, str]],
    sql_example_selector: SemanticSimilarityExampleSelector,
    example_nums: int,
):
    logger.info("original sql_examples_collection size: {}", vectorstore._collection.count())
    new_collection = empty_chroma_collection_2(collection=vectorstore._collection)
    vectorstore._collection = new_collection

    logger.info("emptied sql_examples_collection size: {}", vectorstore._collection.count())

    sql_example_selector = SemanticSimilarityExampleSelector(
        vectorstore=sql_examples_vectorstore,
        k=example_nums,
        input_keys=["question"],
        example_keys=[
            "table_name",
            "fields_list",
            "prior_schema_links",
            "question",
            "analysis",
            "schema_links",
            "current_date",
            "sql",
        ],
    )

    for example in sql_examplars:
        sql_example_selector.add_example(example)

    logger.info("reloaded sql_examples_collection size: {}", vectorstore._collection.count())

    return vectorstore, sql_example_selector


sql_examples_vectorstore = Chroma(
    collection_name=TEXT2DSL_COLLECTION_NAME,
    embedding_function=hg_embedding,
    client=chromadb_client,
)

example_nums = TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM

sql_example_selector = SemanticSimilarityExampleSelector(
    vectorstore=sql_examples_vectorstore,
    k=example_nums,
    input_keys=["question"],
    example_keys=[
        "table_name",
        "fields_list",
        "prior_schema_links",
        "question",
        "analysis",
        "schema_links",
        "current_date",
        "sql",
    ],
)

if sql_examples_vectorstore._collection.count() > 0:
    logger.info("examples already in sql_vectorstore")
    logger.info("init sql_vectorstore size: {}", sql_examples_vectorstore._collection.count())

logger.info("sql_examplars size: {}", len(sql_examplars))
sql_examples_vectorstore, sql_example_selector = reload_sql_example_collection(
    sql_examples_vectorstore, sql_examplars, sql_example_selector, example_nums
)
logger.info("added sql_vectorstore size: {}", sql_examples_vectorstore._collection.count())

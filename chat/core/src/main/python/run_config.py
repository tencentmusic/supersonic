# -*- coding:utf-8 -*-
import os

from config import (
    configs,
    llmparser_configs,
    vector_store_configs,
    text2dsl_configs,
    llm_configs,
    embedding_configs,
)

conf = configs

PROJECT_DIR_PATH = os.path.dirname(os.path.abspath(__file__))

LLMPARSER_HOST = llmparser_configs.get("host", "127.0.0.1")
LLMPARSER_PORT = conf.get("llmparser.host", 9092)

MODEL_NAME = llm_configs.get("model_name", "gpt-3.5-turbo-16k")
OPENAI_API_KEY = llm_configs.get("openai_api_key")
OPENAI_API_BASE = llm_configs.get("openai_api_base")

TEMPERATURE = conf["llm.temperature"]

CHROMA_DB_PERSIST_DIR = vector_store_configs.get("chroma_db_persist_dir")
PRESET_QUERY_COLLECTION_NAME = conf["query.preset_query_collection_name"]
TEXT2DSL_COLLECTION_NAME = text2dsl_configs.get("collection_name")
TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM = text2dsl_configs.get("few_shots_example_num")
TEXT2DSL_IS_SHORTCUT = text2dsl_configs.get("is_shortcut")

CHROMA_DB_PERSIST_PATH = os.path.join(PROJECT_DIR_PATH, CHROMA_DB_PERSIST_DIR)

HF_TEXT2VEC_MODEL_NAME = embedding_configs.get(
    "hf_text2vec_model_name", "GanymedeNil/text2vec-large-chinese"
)

if __name__ == "__main__":
    print("PROJECT_DIR_PATH: ", PROJECT_DIR_PATH)
    print("EMB_MODEL_PATH: ", HF_TEXT2VEC_MODEL_NAME)
    print("CHROMA_DB_PERSIST_PATH: ", CHROMA_DB_PERSIST_PATH)
    print("LLMPARSER_HOST: ", LLMPARSER_HOST)
    print("LLMPARSER_PORT: ", LLMPARSER_PORT)

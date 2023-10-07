# -*- coding:utf-8 -*-
import os

PROJECT_DIR_PATH = os.path.dirname(os.path.abspath(__file__))

LLMPARSER_HOST = "127.0.0.1"
LLMPARSER_PORT = 9092

MODEL_NAME = "gpt-3.5-turbo-16k"
OPENAI_API_KEY = "YOUR_API_KEY"
OPENAI_API_BASE = ""

TEMPERATURE = 0.0

CHROMA_DB_PERSIST_DIR = "chm_db"
PRESET_QUERY_COLLECTION_NAME = "preset_query_collection"
SOLVED_QUERY_COLLECTION_NAME = "solved_query_collection"
TEXT2DSL_COLLECTION_NAME = "text2dsl_collection"
TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM = 15
TEXT2DSL_IS_SHORTCUT = False

CHROMA_DB_PERSIST_PATH = os.path.join(PROJECT_DIR_PATH, CHROMA_DB_PERSIST_DIR)

HF_TEXT2VEC_MODEL_NAME = "GanymedeNil/text2vec-large-chinese"

if __name__ == "__main__":
    logger.info("PROJECT_DIR_PATH: {}", PROJECT_DIR_PATH)
    logger.info("EMB_MODEL_PATH: {}", HF_TEXT2VEC_MODEL_NAME)
    logger.info("CHROMA_DB_PERSIST_PATH: {}", CHROMA_DB_PERSIST_PATH)
    logger.info("LLMPARSER_HOST: {}", LLMPARSER_HOST)
    logger.info("LLMPARSER_PORT: {}", LLMPARSER_PORT)

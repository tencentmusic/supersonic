# -*- coding:utf-8 -*-
import os
import configparser
from util.logging_utils import logger


def type_convert(input_str: str):
    try:
        return eval(input_str)
    except:
        return input_str


PROJECT_DIR_PATH = os.path.dirname(os.path.abspath(__file__))

config_file = "run_config.ini"
config_path = os.path.join(PROJECT_DIR_PATH, config_file)
config = configparser.ConfigParser()
config.read(config_path)

llm_parser_section_name = "LLMParser"
LLMPARSER_HOST = config.get(llm_parser_section_name, 'LLMPARSER_HOST')
LLMPARSER_PORT = int(config.get(llm_parser_section_name, 'LLMPARSER_PORT')) 

chroma_db_section_name = "ChromaDB"
CHROMA_DB_PERSIST_DIR = config.get(chroma_db_section_name, 'CHROMA_DB_PERSIST_DIR')
PRESET_QUERY_COLLECTION_NAME = config.get(chroma_db_section_name, 'PRESET_QUERY_COLLECTION_NAME')
SOLVED_QUERY_COLLECTION_NAME = config.get(chroma_db_section_name, 'SOLVED_QUERY_COLLECTION_NAME')
TEXT2DSL_COLLECTION_NAME = config.get(chroma_db_section_name, 'TEXT2DSL_COLLECTION_NAME')
TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM = int(config.get(chroma_db_section_name, 'TEXT2DSL_FEW_SHOTS_EXAMPLE_NUM')) 
TEXT2DSL_IS_SHORTCUT = eval(config.get(chroma_db_section_name, 'TEXT2DSL_IS_SHORTCUT')) 
CHROMA_DB_PERSIST_PATH = os.path.join(PROJECT_DIR_PATH, CHROMA_DB_PERSIST_DIR)

text2vec_section_name = "Text2Vec"
HF_TEXT2VEC_MODEL_NAME = config.get(text2vec_section_name, 'HF_TEXT2VEC_MODEL_NAME')

llm_provider_section_name = "LLMProvider"
LLM_PROVIDER_NAME = config.get(llm_provider_section_name, 'LLM_PROVIDER_NAME')

llm_model_section_name = "LLMModel"
llm_config_dict = {}
for option in config.options(llm_model_section_name):
    llm_config_dict[option] = type_convert(config.get(llm_model_section_name, option)) 


if __name__ == "__main__":
    logger.info("PROJECT_DIR_PATH: ", PROJECT_DIR_PATH)
    logger.info("EMB_MODEL_PATH: ", HF_TEXT2VEC_MODEL_NAME)
    logger.info("CHROMA_DB_PERSIST_PATH: ", CHROMA_DB_PERSIST_PATH)
    logger.info("LLMPARSER_HOST: ", LLMPARSER_HOST)
    logger.info("LLMPARSER_PORT: ", LLMPARSER_PORT)
    logger.info("llm_config_dict: ", llm_config_dict)
    logger.info("is_shortcut: ", TEXT2DSL_IS_SHORTCUT)

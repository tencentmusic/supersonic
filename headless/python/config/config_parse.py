# -*- coding:utf-8 -*-
import configparser

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

class EnvInterpolation(configparser.BasicInterpolation):
    """Interpolation which expands environment variables in values."""

    def before_get(self, parser, section, option, value, defaults):
        value = super().before_get(parser, section, option, value, defaults)
        return os.path.expandvars(value)

def type_convert(input_str: str):
    try:
        return eval(input_str)
    except:
        return input_str


PROJECT_DIR_PATH = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
config_dir = "config"
CONFIG_DIR_PATH = os.path.join(PROJECT_DIR_PATH, config_dir)
config_file = "run_config.ini"
config_path = os.path.join(CONFIG_DIR_PATH, config_file)

config = configparser.ConfigParser(interpolation=EnvInterpolation())
config.read(config_path)

log_dir = "log"
LOG_DIR_PATH = os.path.join(PROJECT_DIR_PATH, log_dir)
log_file = "run.log"
LOG_FILE_PATH = os.path.join(LOG_DIR_PATH, log_file)

llm_parser_section_name = "LLMParser"
LLMPARSER_HOST = config.get(llm_parser_section_name, 'LLMPARSER_HOST')
LLMPARSER_PORT = int(config.get(llm_parser_section_name, 'LLMPARSER_PORT')) 

chroma_db_section_name = "ChromaDB"
CHROMA_DB_PERSIST_DIR = config.get(chroma_db_section_name, 'CHROMA_DB_PERSIST_DIR')
PRESET_QUERY_COLLECTION_NAME = config.get(chroma_db_section_name, 'PRESET_QUERY_COLLECTION_NAME')
SOLVED_QUERY_COLLECTION_NAME = config.get(chroma_db_section_name, 'SOLVED_QUERY_COLLECTION_NAME')
TEXT2DSLAGENT_COLLECTION_NAME = config.get(chroma_db_section_name, 'TEXT2DSLAGENT_COLLECTION_NAME')
TEXT2DSLAGENTACT_COLLECTION_NAME = config.get(chroma_db_section_name, 'TEXT2DSLAGENTACT_COLLECTION_NAME')
TEXT2DSL_EXAMPLE_NUM = int(config.get(chroma_db_section_name, 'TEXT2DSL_EXAMPLE_NUM')) 
TEXT2DSL_FEWSHOTS_NUM = int(config.get(chroma_db_section_name, 'TEXT2DSL_FEWSHOTS_NUM')) 
TEXT2DSL_SELF_CONSISTENCY_NUM = int(config.get(chroma_db_section_name, 'TEXT2DSL_SELF_CONSISTENCY_NUM')) 
ACT_MIN_WINDOWN_SIZE = int(config.get(chroma_db_section_name, 'ACT_MIN_WINDOWN_SIZE'))
ACT_MAX_WINDOWN_SIZE = int(config.get(chroma_db_section_name, 'ACT_MAX_WINDOWN_SIZE'))
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
    print(f"PROJECT_DIR_PATH: {PROJECT_DIR_PATH}")
    print(f"EMB_MODEL_PATH: {HF_TEXT2VEC_MODEL_NAME}")
    print(f"CHROMA_DB_PERSIST_PATH: {CHROMA_DB_PERSIST_PATH}")
    print(f"LLMPARSER_HOST: {LLMPARSER_HOST}")
    print(f"LLMPARSER_PORT: {LLMPARSER_PORT}")
    print(f"llm_config_dict: {llm_config_dict}")
    print(f"LLM_PROVIDER_NAME: {LLM_PROVIDER_NAME}")
    print(f"PRESET_QUERY_COLLECTION_NAME: {PRESET_QUERY_COLLECTION_NAME}")
    print(f"SOLVED_QUERY_COLLECTION_NAME: {SOLVED_QUERY_COLLECTION_NAME}")
    print(f"TEXT2DSLAGENT_COLLECTION_NAME: {TEXT2DSLAGENT_COLLECTION_NAME}")
    print(f"TEXT2DSLAGENTACT_COLLECTION_NAME: {TEXT2DSLAGENTACT_COLLECTION_NAME}")
    print(f"TEXT2DSL_EXAMPLE_NUM: {TEXT2DSL_EXAMPLE_NUM}")
    print(f"TEXT2DSL_FEWSHOTS_NUM: {TEXT2DSL_FEWSHOTS_NUM}")
    print(f"TEXT2DSL_SELF_CONSISTENCY_NUM: {TEXT2DSL_SELF_CONSISTENCY_NUM}")
    print(f"ACT_MIN_WINDOWN_SIZE: {ACT_MIN_WINDOWN_SIZE}")
    print(f"ACT_MAX_WINDOWN_SIZE: {ACT_MAX_WINDOWN_SIZE}")
    print(f"LOG_FILE_PATH: {LOG_FILE_PATH}")

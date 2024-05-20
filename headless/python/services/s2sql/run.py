# -*- coding:utf-8 -*-

import asyncio

import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

import json

from s2sql.constructor import FewShotPromptTemplate2
from s2sql.sql_agent import Text2DSLAgent, Text2DSLAgentAutoCoT, Text2DSLAgentWrapper

from instances.chromadb_instance import client as chromadb_client
from instances.logging_instance import logger
from instances.text2vec_instance import emb_func

from few_shot_example.s2sql_exemplar import exemplars as sql_exemplars
from config.config_parse import (TEXT2DSLAGENT_COLLECTION_NAME, TEXT2DSLAGENTACT_COLLECTION_NAME,
                    TEXT2DSL_EXAMPLE_NUM, TEXT2DSL_FEWSHOTS_NUM, TEXT2DSL_SELF_CONSISTENCY_NUM,
                    ACT_MIN_WINDOWN_SIZE, ACT_MAX_WINDOWN_SIZE)


text2dsl_agent_collection = chromadb_client.get_or_create_collection(name=TEXT2DSLAGENT_COLLECTION_NAME,
                                            embedding_function=emb_func,
                                            metadata={"hnsw:space": "cosine"})
text2dsl_agent_act_collection = chromadb_client.get_or_create_collection(name=TEXT2DSLAGENTACT_COLLECTION_NAME,
                                            embedding_function=emb_func,
                                            metadata={"hnsw:space": "cosine"})

text2dsl_agent_example_prompter = FewShotPromptTemplate2(collection=text2dsl_agent_collection,
                                            retrieval_key="question",
                                            few_shot_seperator='\n\n')
text2dsl_agent_act_example_prompter = FewShotPromptTemplate2(collection=text2dsl_agent_act_collection,
                                            retrieval_key="question",
                                            few_shot_seperator='\n\n')

text2sql_agent = Text2DSLAgent(num_fewshots=TEXT2DSL_FEWSHOTS_NUM, num_examples=TEXT2DSL_EXAMPLE_NUM, num_self_consistency=TEXT2DSL_SELF_CONSISTENCY_NUM,
                               sql_example_prompter=text2dsl_agent_example_prompter)
text2sql_agent_autoCoT = Text2DSLAgentAutoCoT(num_fewshots=TEXT2DSL_FEWSHOTS_NUM, num_examples=TEXT2DSL_EXAMPLE_NUM, num_self_consistency=TEXT2DSL_SELF_CONSISTENCY_NUM,
                                            sql_example_prompter=text2dsl_agent_act_example_prompter,
                                            auto_cot_min_window_size=ACT_MIN_WINDOWN_SIZE, auto_cot_max_window_size=ACT_MAX_WINDOWN_SIZE)

sql_ids = [str(i) for i in range(0, len(sql_exemplars))]
text2sql_agent.reload_setting(sql_ids, sql_exemplars, TEXT2DSL_EXAMPLE_NUM, TEXT2DSL_FEWSHOTS_NUM, TEXT2DSL_SELF_CONSISTENCY_NUM)

if text2sql_agent_autoCoT.count_examples()==0:
    source_dir_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))) 
    example_dir_path = os.path.join(source_dir_path, 'few_shot_example')
    example_json_file = os.path.join(example_dir_path, 's2sql_exemplar3_transformed.json')
    with open(example_json_file, 'r', encoding='utf-8') as f:
        transformed_sql_examplar_list = json.load(f)

    transformed_sql_examplar_ids = [str(i) for i in range(0, len(transformed_sql_examplar_list))]
    text2sql_agent_autoCoT.reload_setting_autoCoT(transformed_sql_examplar_ids, transformed_sql_examplar_list, TEXT2DSL_EXAMPLE_NUM, TEXT2DSL_FEWSHOTS_NUM, TEXT2DSL_SELF_CONSISTENCY_NUM)


text2sql_agent_router = Text2DSLAgentWrapper(sql_agent_act=text2sql_agent_autoCoT)


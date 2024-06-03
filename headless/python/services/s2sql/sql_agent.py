import os
import sys
from typing import Dict, List, Optional, Union, Mapping, Any
from collections import Counter
import random
import asyncio
from enum import Enum

from langchain.llms.base import BaseLLM

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger

from s2sql.constructor import FewShotPromptTemplate2
from s2sql.output_parser import  schema_link_parse, combo_schema_link_parse, combo_sql_parse
from s2sql.auto_cot_run import transform_sql_example, transform_sql_example_autoCoT_run
from instances.llm_instance import get_llm


class Text2DSLAgentBase(object):
    def __init__(self, num_fewshots:int, num_examples:int, num_self_consistency:int,
            sql_example_prompter:FewShotPromptTemplate2) -> None:
        self.num_fewshots = num_fewshots
        self.num_examples = num_examples
        assert self.num_fewshots <= self.num_examples
        self.num_self_consistency = num_self_consistency

        self.sql_example_prompter = sql_example_prompter

    def get_examples_candidates(self, question: str, filter_condition: Mapping[str, str], num_examples: int)->List[Mapping[str, str]]:
        few_shot_example_meta_list = self.sql_example_prompter.retrieve_few_shot_example(question, num_examples, filter_condition)

        if len(few_shot_example_meta_list) == num_examples:
            return few_shot_example_meta_list
        elif len(few_shot_example_meta_list) < num_examples:
            logger.info(f"few_shot_example_meta_list size: {len(few_shot_example_meta_list)} < num_examples: {num_examples}")
            existed_id_set = set([item['id'] for item in few_shot_example_meta_list])
            extra_few_shot_example_meta_list = self.sql_example_prompter.retrieve_few_shot_example(query_text=question, retrieval_num=num_examples, filter_condition=None)

            for item in extra_few_shot_example_meta_list:
                if item['id'] not in existed_id_set:
                    few_shot_example_meta_list.append(item)
                    existed_id_set.add(item['id'])
                if len(few_shot_example_meta_list) == num_examples:
                        break
            
            logger.info(f"few_shot_example_meta_list size: {len(few_shot_example_meta_list)} = num_examples: {num_examples}")
            return few_shot_example_meta_list
        else:
            logger.info(f"few_shot_example_meta_list size: {len(few_shot_example_meta_list)} > num_examples: {num_examples}")
            few_shot_example_meta_list = few_shot_example_meta_list[:num_examples]
            return few_shot_example_meta_list
        
    def get_fewshot_example_combos(self, example_meta_list:List[Mapping[str, str]], num_fewshots:int)-> List[List[Mapping[str, str]]]:
        fewshot_example_list = []
        for i in range(0, self.num_self_consistency):
            random.shuffle(example_meta_list)
            fewshot_example_list.append(example_meta_list[:num_fewshots])

        return fewshot_example_list
     
    def self_consistency_vote(self, output_res_pool:List[str]):
        output_res_counts = Counter(output_res_pool)
        output_res_max = output_res_counts.most_common(1)[0][0]
        total_output_num = len(output_res_pool)

        vote_percentage = {k: (v/total_output_num) for k,v in output_res_counts.items()}

        return output_res_max, vote_percentage
    
    def schema_linking_list_str_unify(self, schema_linking_list: List[str])-> List[str]:
        schema_linking_list_unify = []
        for schema_linking_str in schema_linking_list:
            schema_linking_str_unify = ','.join(sorted([item.strip() for item in schema_linking_str.strip('[]').split(',')]))
            schema_linking_str_unify = f'[{schema_linking_str_unify}]'
            schema_linking_list_unify.append(schema_linking_str_unify)

        return schema_linking_list_unify

class Text2DSLAgentAutoCoT(Text2DSLAgentBase):
        def __init__(self, num_fewshots:int, num_examples:int, num_self_consistency:int,
            sql_example_prompter:FewShotPromptTemplate2,
            auto_cot_min_window_size: int, auto_cot_max_window_size: int):
            super().__init__(num_fewshots, num_examples, num_self_consistency, sql_example_prompter)

            assert auto_cot_min_window_size <= auto_cot_max_window_size 
            self.auto_cot_min_window_size = auto_cot_min_window_size
            self.auto_cot_max_window_size = auto_cot_max_window_size

        def reload_setting(self, sql_example_ids: List[str], sql_example_units: List[Mapping[str,str]], num_examples:int, num_fewshots:int, num_self_consistency:int):
            self.num_fewshots = num_fewshots
            self.num_examples = num_examples
            assert self.num_fewshots <= self.num_examples
            self.num_self_consistency = num_self_consistency
            assert self.num_self_consistency >= 1

            new_sql_example_unit_list = transform_sql_example_autoCoT_run(sql_example_units, self.auto_cot_min_window_size, self.auto_cot_max_window_size)
            self.sql_example_prompter.reload_few_shot_example(sql_example_ids, new_sql_example_unit_list)

        def reload_setting_autoCoT(self, sql_example_ids: List[str], auto_cot_sql_example_units: List[Mapping[str,str]], num_examples:int, num_fewshots:int, num_self_consistency:int):
            self.num_fewshots = num_fewshots
            self.num_examples = num_examples
            assert self.num_fewshots <= self.num_examples
            self.num_self_consistency = num_self_consistency
            assert self.num_self_consistency >= 1

            self.sql_example_prompter.reload_few_shot_example(sql_example_ids, auto_cot_sql_example_units)

        def add_examples(self, sql_example_ids: List[str], sql_example_units: List[Mapping[str,str]]):
            new_sql_example_unit_list = transform_sql_example_autoCoT_run(sql_example_units, self.auto_cot_min_window_size, self.auto_cot_max_window_size)
            self.sql_example_prompter.add_few_shot_example(sql_example_ids, new_sql_example_unit_list)

        def update_examples(self, sql_example_ids: List[str], sql_example_units: List[Mapping[str,str]]):
            new_sql_example_unit_list = transform_sql_example_autoCoT_run(sql_example_units, self.auto_cot_min_window_size, self.auto_cot_max_window_size)
            self.sql_example_prompter.update_few_shot_example(sql_example_ids, new_sql_example_unit_list)

        def delete_examples(self, sql_example_ids: List[str]):
            self.sql_example_prompter.delete_few_shot_example(sql_example_ids)

        def count_examples(self):
            return self.sql_example_prompter.count_few_shot_example()
        
        def get_examples(self, sql_example_ids: List[str]):
            return self.sql_example_prompter.get_few_shot_example(sql_example_ids)
        
        def generate_schema_linking_prompt(self, question: str, current_date:str, domain_name: str, fields_list: List[str],
                        prior_schema_links: Mapping[str,str], prior_exts:str, fewshot_example_list:List[Mapping[str, str]])-> str:
            
            instruction = "# Find the schema_links for generating SQL queries for each question based on the database schema and Foreign keys."
            
            schema_linking_example_keys = ["questionAugmented", "dbSchema", "generatedSchemaLinkingCoT"]
            schema_linking_example_template = "{dbSchema}\nQ: {questionAugmented}\nA: {generatedSchemaLinkingCoT}"
            schema_linking_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=schema_linking_example_template, 
                                                                                            example_keys=schema_linking_example_keys,
                                                                                            few_shot_example_meta_list=fewshot_example_list)

            question_augmented, db_schema, _ = transform_sql_example(question, current_date, domain_name, fields_list, prior_schema_links, prior_exts)
            new_case_template = """{dbSchema}\nQ: {questionAugmented1}\nA: Let’s think step by step. In the question "{questionAugmented2}", we are asked:"""
            new_case_prompt = new_case_template.format(dbSchema=db_schema, questionAugmented1=question_augmented, questionAugmented2=question_augmented)

            schema_linking_prompt = instruction + '\n\n' + schema_linking_fewshot_prompt + '\n\n' + new_case_prompt

            logger.info(f'schema_linking_prompt: {schema_linking_prompt}')
            return schema_linking_prompt


        def generate_schema_linking_prompt_pool(self, question: str, current_date:str, domain_name: str, fields_list: List[str],
                                                prior_schema_links: Mapping[str,str], prior_exts:str, fewshot_example_list_pool:List[List[Mapping[str, str]]])-> List[str]:
            schema_linking_prompt_pool = []
            for fewshot_example_list in fewshot_example_list_pool:
                schema_linking_prompt = self.generate_schema_linking_prompt(question, current_date, domain_name, fields_list, prior_schema_links, prior_exts, fewshot_example_list)
                schema_linking_prompt_pool.append(schema_linking_prompt)

            return schema_linking_prompt_pool

        def generate_sql_prompt(self, question: str, domain_name: str,fields_list: List[str],
                            schema_link_str: str, current_date: str, prior_schema_links: Mapping[str,str], prior_exts:str,
                            fewshot_example_list:List[Mapping[str, str]], terms_list: Optional[List[Dict]] = [])-> str:
            
            instruction = "# Use the the schema links to generate the SQL queries for each of the questions."
            sql_example_keys = ["questionAugmented", "dbSchema", "generatedSchemaLinkings", "sql"]
            sql_example_template = "{dbSchema}\nQ: {questionAugmented}\nSchema_links: {generatedSchemaLinkings}\nSQL: {sql}"

            sql_example_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=sql_example_template, 
                                                                                            example_keys=sql_example_keys,
                                                                                            few_shot_example_meta_list=fewshot_example_list)

            question_augmented, db_schema, _ = transform_sql_example(question, current_date, domain_name, fields_list, prior_schema_links, prior_exts, terms_list=terms_list)
            new_case_template = "{dbSchema}\nQ: {questionAugmented}\nSchema_links: {schemaLinkings}\nSQL: "
            new_case_prompt = new_case_template.format(dbSchema=db_schema, questionAugmented=question_augmented, schemaLinkings=schema_link_str)

            sql_example_prompt = instruction + '\n\n' + sql_example_fewshot_prompt + '\n\n' + new_case_prompt
            
            logger.info(f'sql_example_prompt: {sql_example_prompt}')
            return sql_example_prompt
        
        def generate_sql_prompt_pool(self, question: str, domain_name: str,fields_list: List[str],
                            schema_link_str_pool: List[str], current_date: str, prior_schema_links: Mapping[str,str], prior_exts:str,
                            fewshot_example_list_pool:List[List[Mapping[str, str]]], terms_list: Optional[List[Dict]] = [])-> List[str]:
            sql_prompt_pool = []
            for schema_link_str, fewshot_example_list in zip(schema_link_str_pool, fewshot_example_list_pool):
                sql_prompt = self.generate_sql_prompt(question, domain_name, fields_list, schema_link_str, current_date, prior_schema_links, prior_exts, fewshot_example_list, terms_list=terms_list)
                sql_prompt_pool.append(sql_prompt)

            return sql_prompt_pool

        def generate_schema_linking_sql_prompt(self, question: str, current_date:str, domain_name: str, fields_list: List[str],
                        prior_schema_links: Mapping[str,str], prior_exts:str, fewshot_example_list:List[Mapping[str, str]], terms_list: Optional[List[Dict]] = []):
            
            instruction = "# Find the schema_links for generating SQL queries for each question based on the database schema and Foreign keys. Then use the the schema links to generate the SQL queries for each of the questions."

            example_keys = ["questionAugmented", "dbSchema", "generatedSchemaLinkingCoT","sql"]
            example_template = "{dbSchema}\nQ: {questionAugmented}\nA: {generatedSchemaLinkingCoT}\nSQL: {sql}\n"
            fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=example_template, 
                                                                                    example_keys=example_keys,
                                                                                    few_shot_example_meta_list=fewshot_example_list)
            

            question_augmented, db_schema, _ = transform_sql_example(question, current_date, domain_name, fields_list, prior_schema_links, prior_exts, terms_list)
            new_case_template = """{dbSchema}\nQ: {questionAugmented1}\nA: Let’s think step by step. In the question "{questionAugmented2}", we are asked:"""
            new_case_prompt = new_case_template.format(dbSchema=db_schema, questionAugmented1=question_augmented, questionAugmented2=question_augmented)

            prompt = instruction + '\n\n' + fewshot_prompt + '\n\n' + new_case_prompt

            logger.info(f'schema_linking_sql_prompt: {prompt}')
            return prompt

        def generate_schema_linking_sql_prompt_pool(self, question: str, current_date:str, domain_name: str, fields_list: List[str],
                                                    prior_schema_links: Mapping[str,str], prior_exts:str, fewshot_example_list_pool:List[List[Mapping[str, str]]], terms_list: Optional[List[Dict]] = [])-> List[str]:
            schema_linking_sql_prompt_pool = []
            for fewshot_example_list in fewshot_example_list_pool:
                schema_linking_sql_prompt = self.generate_schema_linking_sql_prompt(question, current_date, domain_name, fields_list, prior_schema_links, prior_exts, fewshot_example_list, terms_list=terms_list)
                schema_linking_sql_prompt_pool.append(schema_linking_sql_prompt)

            return schema_linking_sql_prompt_pool

        async def async_query2sql(self, question: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    current_date: str, prior_schema_links: Mapping[str,str], prior_exts: str,
                    llm_config:dict, terms_list: Optional[List[Dict]] = []):
            logger.info("question: {}".format(question))
            logger.info("filter_condition: {}".format(filter_condition))
            logger.info("model_name: {}".format(model_name))
            logger.info("fields_list: {}".format(fields_list))
            logger.info("current_date: {}".format(current_date))
            logger.info("prior_schema_links: {}".format(prior_schema_links))
            logger.info("prior_exts: {}".format(prior_exts))
            logger.info("terms_list: {}".format(terms_list))


            fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
            schema_linking_prompt = self.generate_schema_linking_prompt(question, current_date, model_name, fields_list, prior_schema_links, prior_exts, fewshot_example_meta_list)
            logger.debug("schema_linking_prompt->{}".format(schema_linking_prompt))
            llm = get_llm(llm_config)
            schema_link_output = await llm._call_async(schema_linking_prompt)
            logger.debug("schema_link_output->{}".format(schema_link_output))

            schema_link_str = schema_link_parse(schema_link_output)
            logger.debug("schema_link_str->{}".format(schema_link_str))
            
            sql_prompt = self.generate_sql_prompt(question, model_name, fields_list, schema_link_str, current_date, prior_schema_links, prior_exts, fewshot_example_meta_list, terms_list=terms_list)
            logger.debug("sql_prompt->{}".format(sql_prompt))
            sql_output = await llm._call_async(sql_prompt)

            resp = dict()
            resp['question'] = question
            resp['model'] = model_name
            resp['fields'] = fields_list
            resp['priorSchemaLinking'] = prior_schema_links
            resp['priorExts'] = prior_exts
            resp['currentDate'] = current_date

            resp['prompt'] = [schema_linking_prompt+'\n\n'+sql_prompt]

            resp['schemaLinkingOutput'] = schema_link_output
            resp['schemaLinkStr'] = schema_link_str
            
            resp['sqlOutput'] = sql_output

            logger.info("resp: {}".format(resp))

            return resp

        async def async_query2sql_shortcut(self, question: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    current_date: str, prior_schema_links: Mapping[str,str], prior_exts: str,
                    llm_config:dict, terms_list: Optional[List[Dict]] = []):
            logger.info("question: {}".format(question))
            logger.info("filter_condition: {}".format(filter_condition))
            logger.info("model_name: {}".format(model_name))
            logger.info("fields_list: {}".format(fields_list))
            logger.info("current_date: {}".format(current_date))
            logger.info("prior_schema_links: {}".format(prior_schema_links))
            logger.info("prior_exts: {}".format(prior_exts))
            logger.info("terms_list: {}".format(terms_list))
            
            fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
            schema_linking_sql_shortcut_prompt = self.generate_schema_linking_sql_prompt(question, current_date, model_name, fields_list, prior_schema_links, prior_exts, fewshot_example_meta_list, terms_list)
            logger.debug("schema_linking_sql_shortcut_prompt->{}".format(schema_linking_sql_shortcut_prompt))
            llm = get_llm(llm_config)
            schema_linking_sql_shortcut_output = await llm._call_async(schema_linking_sql_shortcut_prompt)
            logger.debug("schema_linking_sql_shortcut_output->{}".format(schema_linking_sql_shortcut_output))

            schema_linking_str = combo_schema_link_parse(schema_linking_sql_shortcut_output)
            sql_str = combo_sql_parse(schema_linking_sql_shortcut_output)

            resp = dict()
            resp['question'] = question
            resp['model'] = model_name
            resp['fields'] = fields_list
            resp['priorSchemaLinking'] = prior_schema_links
            resp['priorExts'] = prior_exts
            resp['currentDate'] = current_date

            resp['prompt'] = [schema_linking_sql_shortcut_prompt]

            resp['schemaLinkingComboOutput'] = schema_linking_sql_shortcut_output
            resp['schemaLinkStr'] = schema_linking_str
            resp['sqlOutput'] = sql_str

            logger.info("resp: {}".format(resp))

            return resp

        async def generate_schema_linking_tasks(self, question: str, model_name: str, fields_list: List[str],
                    current_date: str, prior_schema_links: Mapping[str,str], prior_exts: str, 
                    fewshot_example_list_combo:List[List[Mapping[str, str]]], llm_config: dict):

            schema_linking_prompt_pool = self.generate_schema_linking_prompt_pool(question, current_date, model_name, fields_list, prior_schema_links, prior_exts, fewshot_example_list_combo)
            logger.debug("schema_linking_prompt_pool->{}".format(schema_linking_prompt_pool))
            llm = get_llm(llm_config)
            schema_linking_output_pool = await asyncio.gather(*[llm._call_async(schema_linking_prompt) for schema_linking_prompt in schema_linking_prompt_pool])
            logger.debug("schema_linking_output_pool->{}".format(schema_linking_output_pool))

            schema_linking_str_pool = [schema_link_parse(schema_linking_output) for schema_linking_output in schema_linking_output_pool]

            return schema_linking_str_pool, schema_linking_output_pool, schema_linking_prompt_pool
        
        async def generate_sql_tasks(self, question: str, model_name: str, fields_list: List[str], schema_link_str_pool: List[str],
                    current_date: str, prior_schema_links: Mapping[str,str], prior_exts: str, fewshot_example_list_combo:List[List[Mapping[str, str]]], llm_config: dict, terms_list: Optional[List[Dict]] = []):

            sql_prompt_pool = self.generate_sql_prompt_pool(question, model_name, fields_list, schema_link_str_pool, current_date, prior_schema_links, prior_exts, fewshot_example_list_combo, terms_list=terms_list)
            logger.debug("sql_prompt_pool->{}".format(sql_prompt_pool))
            llm = get_llm(llm_config)
            sql_output_pool = await asyncio.gather(*[llm._call_async(sql_prompt) for sql_prompt in sql_prompt_pool])
            logger.debug("sql_output_pool->{}".format(sql_output_pool))

            return sql_output_pool, sql_prompt_pool
        
        async def generate_schema_linking_sql_tasks(self, question: str, model_name: str, fields_list: List[str],
                                                    current_date: str, prior_schema_links: Mapping[str,str], prior_exts: str, 
                                                    fewshot_example_list_combo:List[List[Mapping[str, str]]],llm_config: dict, terms_list: Optional[List[Dict]] = []):
            schema_linking_sql_prompt_pool = self.generate_schema_linking_sql_prompt_pool(question, current_date, model_name, fields_list, prior_schema_links, prior_exts, fewshot_example_list_combo, terms_list=terms_list)
            llm = get_llm(llm_config)
            schema_linking_sql_output_task_pool = [llm._call_async(schema_linking_sql_prompt) for schema_linking_sql_prompt in schema_linking_sql_prompt_pool]
            schema_linking_sql_output_res_pool = await asyncio.gather(*schema_linking_sql_output_task_pool)
            logger.debug("schema_linking_sql_output_res_pool->{}".format(schema_linking_sql_output_res_pool))

            return schema_linking_sql_output_res_pool, schema_linking_sql_prompt_pool, schema_linking_sql_output_task_pool
   
        async def tasks_run(self, question: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    current_date: str, prior_schema_links: Mapping[str,str], prior_exts: str, llm_config: dict, terms_list: Optional[List[Dict]] = []):
            logger.info("question: {}".format(question))
            logger.info("filter_condition: {}".format(filter_condition))
            logger.info("model_name: {}".format(model_name))
            logger.info("fields_list: {}".format(fields_list))
            logger.info("current_date: {}".format(current_date))
            logger.info("prior_schema_links: {}".format(prior_schema_links))
            logger.info("prior_exts: {}".format(prior_exts))
            logger.info("terms_list: {}".format(terms_list))

            
            fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
            fewshot_example_list_combo = self.get_fewshot_example_combos(fewshot_example_meta_list, self.num_fewshots)

            schema_linking_candidate_list, _, schema_linking_prompt_list = await self.generate_schema_linking_tasks(question, model_name, fields_list, current_date, prior_schema_links, prior_exts, fewshot_example_list_combo, llm_config,)
            logger.debug(f'schema_linking_candidate_list:{schema_linking_candidate_list}')
            schema_linking_candidate_sorted_list = self.schema_linking_list_str_unify(schema_linking_candidate_list)
            logger.debug(f'schema_linking_candidate_sorted_list:{schema_linking_candidate_sorted_list}')

            schema_linking_output_max, schema_linking_output_vote_percentage = self.self_consistency_vote(schema_linking_candidate_sorted_list)

            sql_output_candicates, sql_output_prompt_list = await self.generate_sql_tasks(question, model_name, fields_list, schema_linking_candidate_list, current_date, prior_schema_links, prior_exts, fewshot_example_list_combo, llm_config, terms_list=terms_list)
            logger.debug(f'sql_output_candicates:{sql_output_candicates}')
            sql_output_max, sql_output_vote_percentage = self.self_consistency_vote(sql_output_candicates)

            resp = dict()
            resp['question'] = question
            resp['model'] = model_name
            resp['fields'] = fields_list
            resp['priorSchemaLinking'] = prior_schema_links
            resp['priorExts'] = prior_exts
            resp['currentDate'] = current_date

            resp['prompt'] = [schema_linking_prompt+'\n\n'+sql_prompt for schema_linking_prompt, sql_prompt in zip(schema_linking_prompt_list, sql_output_prompt_list)]

            resp['schemaLinkStr'] = schema_linking_output_max
            resp['schemaLinkingWeight'] = schema_linking_output_vote_percentage

            resp['sqlOutput'] = sql_output_max
            resp['sqlWeight'] = sql_output_vote_percentage

            logger.info("resp: {}".format(resp))

            return resp

        async def tasks_run_shortcut(self, question: str, filter_condition: Mapping[str,str], model_name: str, fields_list: List[str],
                    current_date: str, prior_schema_links: Mapping[str,str], prior_exts: str, llm_config: dict, terms_list: Optional[List[Dict]] = []):
            logger.info("question: {}".format(question))
            logger.info("filter_condition: {}".format(filter_condition))
            logger.info("model_name: {}".format(model_name))
            logger.info("fields_list: {}".format(fields_list))
            logger.info("current_date: {}".format(current_date))
            logger.info("prior_schema_links: {}".format(prior_schema_links))
            logger.info("prior_exts: {}".format(prior_exts))
            logger.info("terms_list: {}".format(terms_list))

            fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
            fewshot_example_list_combo = self.get_fewshot_example_combos(fewshot_example_meta_list, self.num_fewshots)

            schema_linking_sql_output_candidates, schema_linking_sql_prompt_list, _ = await self.generate_schema_linking_sql_tasks(question, model_name, fields_list, current_date, prior_schema_links, prior_exts, fewshot_example_list_combo, llm_config=llm_config, terms_list=terms_list)
            logger.debug(f'schema_linking_sql_output_candidates:{schema_linking_sql_output_candidates}')
            schema_linking_output_candidate_list = [combo_schema_link_parse(schema_linking_sql_output_candidate) for schema_linking_sql_output_candidate in schema_linking_sql_output_candidates]
            logger.debug(f'schema_linking_sql_output_candidate_list:{schema_linking_output_candidate_list}')
            schema_linking_output_candidate_sorted_list = self.schema_linking_list_str_unify(schema_linking_output_candidate_list)

            schema_linking_output_max, schema_linking_output_vote_percentage = self.self_consistency_vote(schema_linking_output_candidate_sorted_list)

            sql_output_candidate_list = [combo_sql_parse(schema_linking_sql_output_candidate) for schema_linking_sql_output_candidate in schema_linking_sql_output_candidates]
            logger.debug(f'sql_output_candidate_list:{sql_output_candidate_list}')
            sql_output_max, sql_output_vote_percentage = self.self_consistency_vote(sql_output_candidate_list)

            resp = dict()
            resp['question'] = question
            resp['model'] = model_name
            resp['fields'] = fields_list
            resp['priorSchemaLinking'] = prior_schema_links
            resp['priorExts'] = prior_exts
            resp['currentDate'] = current_date

            resp['prompt'] = schema_linking_sql_prompt_list

            resp['schemaLinkStr'] = schema_linking_output_max
            resp['schemaLinkingWeight'] = schema_linking_output_vote_percentage

            resp['sqlOutput'] = sql_output_max
            resp['sqlWeight'] = sql_output_vote_percentage

            logger.info("resp: {}".format(resp))

            return resp
                                     
class Text2DSLAgent(Text2DSLAgentBase):
    def __init__(self, num_fewshots:int, num_examples:int, num_self_consistency:int,
                sql_example_prompter:FewShotPromptTemplate2) -> None:
        super().__init__(num_fewshots, num_examples, num_self_consistency, sql_example_prompter)

    def reload_setting(self, sql_example_ids:List[str], sql_example_units: List[Mapping[str, str]], num_examples:int, num_fewshots:int, num_self_consistency:int):
        self.num_fewshots = num_fewshots
        self.num_examples = num_examples
        assert self.num_fewshots <= self.num_examples
        self.num_self_consistency = num_self_consistency
        assert self.num_self_consistency >= 1
        self.sql_example_prompter.reload_few_shot_example(sql_example_ids, sql_example_units)

    def add_examples(self, sql_example_ids:List[str], sql_example_units: List[Mapping[str, str]]):
        self.sql_example_prompter.add_few_shot_example(sql_example_ids, sql_example_units)

    def update_examples(self, sql_example_ids:List[str], sql_example_units: List[Mapping[str, str]]):
        self.sql_example_prompter.update_few_shot_example(sql_example_ids, sql_example_units)

    def delete_examples(self, sql_example_ids:List[str]):
        self.sql_example_prompter.delete_few_shot_example(sql_example_ids)

    def get_examples(self, sql_example_ids: List[str]):
        return self.sql_example_prompter.get_few_shot_example(sql_example_ids)
        
    def count_examples(self):
        return self.sql_example_prompter.count_few_shot_example()
    
    def generate_schema_linking_prompt(self, question: str, domain_name: str, fields_list: List[str],
                        prior_schema_links: Mapping[str,str], fewshot_example_list:List[Mapping[str, str]])-> str:
            
        prior_schema_links_str = '['+ ','.join(["""'{}'->{}""".format(k,v) for k,v in prior_schema_links.items()]) + ']'

        instruction = "# 根据数据库的表结构,参考先验信息,找出为每个问题生成SQL查询语句的schema_links"
        
        schema_linking_example_keys = ["tableName", "fieldsList", "priorSchemaLinks", "question", "analysis", "schemaLinks"]
        schema_linking_example_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n问题:{question}\n分析:{analysis} 所以Schema_links是:\nSchema_links:{schemaLinks}"
        schema_linking_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=schema_linking_example_template, 
                                                                                        example_keys=schema_linking_example_keys,
                                                                                        few_shot_example_meta_list=fewshot_example_list)

        new_case_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n问题:{question}\n分析: 让我们一步一步地思考。"
        new_case_prompt = new_case_template.format(tableName=domain_name, fieldsList=fields_list, priorSchemaLinks=prior_schema_links_str, question=question)

        schema_linking_prompt = instruction + '\n\n' + schema_linking_fewshot_prompt + '\n\n' + new_case_prompt
        return schema_linking_prompt

    def generate_schema_linking_prompt_pool(self, question: str, domain_name: str, fields_list: List[str],
                                            prior_schema_links: Mapping[str,str], fewshot_example_list_pool:List[List[Mapping[str, str]]])-> List[str]:
        schema_linking_prompt_pool = []
        for fewshot_example_list in fewshot_example_list_pool:
            schema_linking_prompt = self.generate_schema_linking_prompt(question, domain_name, fields_list, prior_schema_links, fewshot_example_list)
            schema_linking_prompt_pool.append(schema_linking_prompt)

        return schema_linking_prompt_pool

    def generate_sql_prompt(self, question: str, domain_name: str,
                            schema_link_str: str, data_date: str, 
                            fewshot_example_list:List[Mapping[str, str]])-> str:
        instruction = "# 根据schema_links为每个问题生成SQL查询语句"
        sql_example_keys = ["question", "currentDate", "tableName", "schemaLinks", "sql"]
        sql_example_template = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\nSchema_links:{schemaLinks}\nSQL:{sql}"

        sql_example_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=sql_example_template, 
                                                                                        example_keys=sql_example_keys,
                                                                                        few_shot_example_meta_list=fewshot_example_list)

        new_case_template = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\nSchema_links:{schemaLinks}\nSQL:"
        new_case_prompt = new_case_template.format(question=question, currentDate=data_date, tableName=domain_name, schemaLinks=schema_link_str)

        sql_example_prompt = instruction + '\n\n' + sql_example_fewshot_prompt + '\n\n' + new_case_prompt

        return sql_example_prompt

    def generate_sql_prompt_pool(self, question: str, domain_name: str, data_date: str, 
                                schema_link_str_pool: List[str], fewshot_example_list_pool:List[List[Mapping[str, str]]])-> List[str]:
        sql_prompt_pool = []
        for schema_link_str, fewshot_example_list in zip(schema_link_str_pool, fewshot_example_list_pool):
            sql_prompt = self.generate_sql_prompt(question, domain_name, schema_link_str, data_date, fewshot_example_list)
            sql_prompt_pool.append(sql_prompt)

        return sql_prompt_pool
    
    def generate_schema_linking_sql_prompt(self, question: str,
                                                domain_name: str,
                                                data_date : str,
                                                fields_list: List[str],
                                                prior_schema_links: Mapping[str,str],
                                                fewshot_example_list:List[Mapping[str, str]]):
        
        prior_schema_links_str = '['+ ','.join(["""'{}'->{}""".format(k,v) for k,v in prior_schema_links.items()]) + ']'

        instruction = "# 根据数据库的表结构,参考先验信息,找出为每个问题生成SQL查询语句的schema_links,再根据schema_links为每个问题生成SQL查询语句"

        example_keys = ["tableName", "fieldsList", "priorSchemaLinks", "currentDate", "question", "analysis", "schemaLinks", "sql"]
        example_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\nCurrent_date:{currentDate}\n问题:{question}\n分析:{analysis} 所以Schema_links是:\nSchema_links:{schemaLinks}\nSQL:{sql}"
        fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=example_template, 
                                                                                example_keys=example_keys,
                                                                                few_shot_example_meta_list=fewshot_example_list)

        new_case_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\nCurrent_date:{currentDate}\n问题:{question}\n分析: 让我们一步一步地思考。"
        new_case_prompt = new_case_template.format(tableName=domain_name, fieldsList=fields_list, priorSchemaLinks=prior_schema_links_str, currentDate=data_date, question=question)

        prompt = instruction + '\n\n' + fewshot_prompt + '\n\n' + new_case_prompt

        return prompt

    def generate_schema_linking_sql_prompt_pool(self, question: str, domain_name: str, fields_list: List[str], data_date: str,
                                            prior_schema_links: Mapping[str,str], fewshot_example_list_pool:List[List[Mapping[str, str]]])-> List[str]:
        schema_linking_sql_prompt_pool = []
        for fewshot_example_list in fewshot_example_list_pool:
            schema_linking_sql_prompt = self.generate_schema_linking_sql_prompt(question, domain_name, data_date, fields_list, prior_schema_links, fewshot_example_list)
            schema_linking_sql_prompt_pool.append(schema_linking_sql_prompt)

        return schema_linking_sql_prompt_pool

    def self_consistency_vote(self, output_res_pool:List[str]):
        output_res_counts = Counter(output_res_pool)
        output_res_max = output_res_counts.most_common(1)[0][0]
        total_output_num = len(output_res_pool)

        vote_percentage = {k: (v/total_output_num) for k,v in output_res_counts.items()}

        return output_res_max, vote_percentage
    
    def schema_linking_list_str_unify(self, schema_linking_list: List[str])-> List[str]:
        schema_linking_list_unify = []
        for schema_linking_str in schema_linking_list:
            schema_linking_str_unify = ','.join(sorted([item.strip() for item in schema_linking_str.strip('[]').split(',')]))
            schema_linking_str_unify = f'[{schema_linking_str_unify}]'
            schema_linking_list_unify.append(schema_linking_str_unify)

        return schema_linking_list_unify

    async def generate_schema_linking_tasks(self, question: str, domain_name: str, 
                                fields_list: List[str], prior_schema_links: Mapping[str,str], 
                                fewshot_example_list_combo:List[List[Mapping[str, str]]], llm_config: dict):

        schema_linking_prompt_pool = self.generate_schema_linking_prompt_pool(question, domain_name, 
                                                                            fields_list, prior_schema_links, 
                                                                            fewshot_example_list_combo)
        llm = get_llm(llm_config)
        schema_linking_output_task_pool = [llm._call_async(schema_linking_prompt) for schema_linking_prompt in schema_linking_prompt_pool]
        schema_linking_output_pool = await asyncio.gather(*schema_linking_output_task_pool)
        logger.debug(f'schema_linking_output_pool:{schema_linking_output_pool}')

        schema_linking_str_pool = [schema_link_parse(schema_linking_output) for schema_linking_output in schema_linking_output_pool]

        return schema_linking_str_pool

    async def generate_sql_tasks(self, question: str, domain_name: str, data_date: str,
                                schema_link_str_pool: List[str], fewshot_example_list_combo:List[List[Mapping[str, str]]],
                                llm_config: dict):    
        
        sql_prompt_pool = self.generate_sql_prompt_pool(question, domain_name, schema_link_str_pool, data_date, fewshot_example_list_combo)
        llm = get_llm(llm_config)
        sql_output_task_pool = [llm._call_async(sql_prompt) for sql_prompt in sql_prompt_pool]
        sql_output_res_pool = await asyncio.gather(*sql_output_task_pool)
        logger.debug(f'sql_output_res_pool:{sql_output_res_pool}')

        return sql_output_res_pool
    
    async def generate_schema_linking_sql_tasks(self, question: str, domain_name: str, fields_list: List[str], data_date: str,
                                            prior_schema_links: Mapping[str,str], fewshot_example_list_combo:List[List[Mapping[str, str]]],
                                            llm_config: dict):
        schema_linking_sql_prompt_pool = self.generate_schema_linking_sql_prompt_pool(question, domain_name, fields_list, data_date, prior_schema_links, fewshot_example_list_combo)
        llm = get_llm(llm_config)
        schema_linking_sql_output_task_pool = [llm._call_async(schema_linking_sql_prompt) for schema_linking_sql_prompt in schema_linking_sql_prompt_pool]
        schema_linking_sql_output_res_pool = await asyncio.gather(*schema_linking_sql_output_task_pool)
        logger.debug(f'schema_linking_sql_output_res_pool:{schema_linking_sql_output_res_pool}')

        return schema_linking_sql_output_res_pool
    
    async def tasks_run(self, question: str, filter_condition: Mapping[str, str], domain_name: str, fields_list: List[str], prior_schema_links: Mapping[str,str], data_date: str, prior_exts: str, llm_config: dict):
        logger.info("question: {}".format(question))
        logger.info("domain_name: {}".format(domain_name))
        logger.info("fields_list: {}".format(fields_list))
        logger.info("current_date: {}".format(data_date))
        logger.info("prior_schema_links: {}".format(prior_schema_links))
        logger.info("prior_exts: {}".format(prior_exts))
        
        if prior_exts != '':
            question = question + ' 备注:'+prior_exts
        logger.info("question_prior_exts: {}".format(question))

        fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
        fewshot_example_list_combo = self.get_fewshot_example_combos(fewshot_example_meta_list, self.num_fewshots)

        schema_linking_candidate_list = await self.generate_schema_linking_tasks(question, domain_name, fields_list, prior_schema_links, fewshot_example_list_combo, llm_config)
        logger.debug(f'schema_linking_candidate_list:{schema_linking_candidate_list}')
        schema_linking_candidate_sorted_list = self.schema_linking_list_str_unify(schema_linking_candidate_list)
        logger.debug(f'schema_linking_candidate_sorted_list:{schema_linking_candidate_sorted_list}')

        schema_linking_output_max, schema_linking_output_vote_percentage = self.self_consistency_vote(schema_linking_candidate_sorted_list)

        sql_output_candicates = await self.generate_sql_tasks(question, domain_name, data_date, schema_linking_candidate_list,fewshot_example_list_combo)
        logger.debug(f'sql_output_candicates:{sql_output_candicates}')
        sql_output_max, sql_output_vote_percentage = self.self_consistency_vote(sql_output_candicates)

        resp = dict()
        resp['question'] = question
        resp['model'] = domain_name
        resp['fields'] = fields_list
        resp['priorSchemaLinking'] = prior_schema_links
        resp['dataDate'] = data_date

        resp['schemaLinkStr'] = schema_linking_output_max
        resp['schemaLinkingWeight'] = schema_linking_output_vote_percentage

        resp['sqlOutput'] = sql_output_max
        resp['sqlWeight'] = sql_output_vote_percentage

        logger.info("resp: {}".format(resp))

        return resp
    
    async def tasks_run_shortcut(self, question: str, filter_condition: Mapping[str, str], domain_name: str, fields_list: List[str], prior_schema_links: Mapping[str,str], data_date: str, prior_exts: str):
        logger.info("question: {}".format(question))
        logger.info("domain_name: {}".format(domain_name))
        logger.info("fields_list: {}".format(fields_list))
        logger.info("current_date: {}".format(data_date))
        logger.info("prior_schema_links: {}".format(prior_schema_links))
        logger.info("prior_exts: {}".format(prior_exts))
        
        if prior_exts != '':
            question = question + ' 备注:'+prior_exts
        logger.info("question_prior_exts: {}".format(question))

        fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
        fewshot_example_list_combo = self.get_fewshot_example_combos(fewshot_example_meta_list, self.num_fewshots)

        schema_linking_sql_output_candidates = await self.generate_schema_linking_sql_tasks(question, domain_name, fields_list, data_date, prior_schema_links, fewshot_example_list_combo)
        logger.debug(f'schema_linking_sql_output_candidates:{schema_linking_sql_output_candidates}')
        schema_linking_output_candidate_list = [combo_schema_link_parse(schema_linking_sql_output_candidate) for schema_linking_sql_output_candidate in schema_linking_sql_output_candidates]
        logger.debug(f'schema_linking_sql_output_candidate_list:{schema_linking_output_candidate_list}')
        schema_linking_output_candidate_sorted_list = self.schema_linking_list_str_unify(schema_linking_output_candidate_list)

        schema_linking_output_max, schema_linking_output_vote_percentage = self.self_consistency_vote(schema_linking_output_candidate_sorted_list)

        sql_output_candidate_list = [combo_sql_parse(schema_linking_sql_output_candidate) for schema_linking_sql_output_candidate in schema_linking_sql_output_candidates]
        logger.debug(f'sql_output_candidate_list:{sql_output_candidate_list}')
        sql_output_max, sql_output_vote_percentage = self.self_consistency_vote(sql_output_candidate_list)

        resp = dict()
        resp['question'] = question
        resp['model'] = domain_name
        resp['fields'] = fields_list
        resp['priorSchemaLinking'] = prior_schema_links
        resp['dataDate'] = data_date

        resp['schemaLinkStr'] = schema_linking_output_max
        resp['schemaLinkingWeight'] = schema_linking_output_vote_percentage

        resp['sqlOutput'] = sql_output_max
        resp['sqlWeight'] = sql_output_vote_percentage

        logger.info("resp: {}".format(resp))

        return resp

    async def async_query2sql(self, question: str, filter_condition: Mapping[str,str],
                model_name: str, fields_list: List[str],
                data_date: str, prior_schema_links: Mapping[str,str], prior_exts: str, llm_config: dict):
        logger.info("question: {}".format(question))
        logger.info("model_name: {}".format(model_name))
        logger.info("fields_list: {}".format(fields_list))
        logger.info("data_date: {}".format(data_date))
        logger.info("prior_schema_links: {}".format(prior_schema_links))
        logger.info("prior_exts: {}".format(prior_exts))

        if prior_exts != '':
            question = question + ' 备注:'+prior_exts
        logger.info("question_prior_exts: {}".format(question))

        fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
        schema_linking_prompt = self.generate_schema_linking_prompt(question, model_name, fields_list, prior_schema_links, fewshot_example_meta_list)
        logger.debug("schema_linking_prompt->{}".format(schema_linking_prompt))
        llm = get_llm(llm_config)
        schema_link_output = await llm._call_async(schema_linking_prompt)

        schema_link_str = schema_link_parse(schema_link_output)
        
        sql_prompt = self.generate_sql_prompt(question, model_name, schema_link_str, data_date, fewshot_example_meta_list)
        logger.debug("sql_prompt->{}".format(sql_prompt))
        sql_output = await llm._call_async(sql_prompt)

        resp = dict()
        resp['question'] = question
        resp['model'] = model_name
        resp['fields'] = fields_list
        resp['priorSchemaLinking'] = prior_schema_links
        resp['dataDate'] = data_date

        resp['schemaLinkingOutput'] = schema_link_output
        resp['schemaLinkStr'] = schema_link_str
        
        resp['sqlOutput'] = sql_output

        logger.info("resp: {}".format(resp))

        return resp

    async def async_query2sql_shortcut(self, question: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    data_date: str, prior_schema_links: Mapping[str,str], prior_exts: str,
                    llm_config: dict):

        logger.info("question: {}".format(question))
        logger.info("model_name: {}".format(model_name))
        logger.info("fields_list: {}".format(fields_list))
        logger.info("data_date: {}".format(data_date))
        logger.info("prior_schema_links: {}".format(prior_schema_links))
        logger.info("prior_exts: {}".format(prior_exts))

        if prior_exts != '':
            question = question + ' 备注:'+prior_exts
        logger.info("question_prior_exts: {}".format(question))
        
        fewshot_example_meta_list = self.get_examples_candidates(question, filter_condition, self.num_examples)
        schema_linking_sql_shortcut_prompt = self.generate_schema_linking_sql_prompt(question, model_name, data_date, fields_list, prior_schema_links, fewshot_example_meta_list)
        logger.debug("schema_linking_sql_shortcut_prompt->{}".format(schema_linking_sql_shortcut_prompt))
        llm = get_llm(llm_config)
        schema_linking_sql_shortcut_output = await llm._call_async(schema_linking_sql_shortcut_prompt)

        schema_linking_str = combo_schema_link_parse(schema_linking_sql_shortcut_output)
        sql_str = combo_sql_parse(schema_linking_sql_shortcut_output)

        resp = dict()
        resp['question'] = question
        resp['model'] = model_name
        resp['fields'] = fields_list
        resp['priorSchemaLinking'] = prior_schema_links
        resp['dataDate'] = data_date

        resp['schemaLinkingComboOutput'] = schema_linking_sql_shortcut_output
        resp['schemaLinkStr'] = schema_linking_str
        resp['sqlOutput'] = sql_str

        logger.info("resp: {}".format(resp))

        return resp

class SqlModeEnum(Enum):
    VALUE5 = '1_pass_auto_cot'
    VALUE6 = '1_pass_auto_cot_self_consistency'
    VALUE7 = '2_pass_auto_cot'
    VALUE8 = '2_pass_auto_cot_self_consistency'

class Text2DSLAgentWrapper(object):
    def __init__(self, sql_agent_act:Text2DSLAgentAutoCoT):
        self.sql_agent_act = sql_agent_act

    async def async_query2sql(self, question: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    data_date: str, prior_schema_links: Mapping[str,str], prior_exts: str, sql_generation_mode: str, llm_config: dict, terms_list: Optional[List[Dict]] = []):

        if sql_generation_mode not in (sql_mode.value for sql_mode in SqlModeEnum):
            raise ValueError(f"sql_generation_mode: {sql_generation_mode} is not in SqlModeEnum")

        if sql_generation_mode == '1_pass_auto_cot':
            logger.info(f"sql wrapper: {sql_generation_mode}")
            resp = await self.sql_agent_act.async_query2sql_shortcut(question=question, filter_condition=filter_condition, model_name=model_name, fields_list=fields_list, current_date=data_date, prior_schema_links=prior_schema_links, prior_exts=prior_exts, llm_config=llm_config, terms_list=terms_list)
            return resp
        elif sql_generation_mode == '1_pass_auto_cot_self_consistency':
            logger.info(f"sql wrapper: {sql_generation_mode}")
            resp = await self.sql_agent_act.tasks_run_shortcut(question=question, filter_condition=filter_condition, model_name=model_name, fields_list=fields_list, current_date=data_date, prior_schema_links=prior_schema_links, prior_exts=prior_exts, llm_config=llm_config, terms_list=terms_list)
            return resp
        elif sql_generation_mode == '2_pass_auto_cot':
            logger.info(f"sql wrapper: {sql_generation_mode}")
            resp = await self.sql_agent_act.async_query2sql(question=question, filter_condition=filter_condition, model_name=model_name, fields_list=fields_list, current_date=data_date, prior_schema_links=prior_schema_links, prior_exts=prior_exts, llm_config=llm_config, terms_list=terms_list)
            return resp
        elif sql_generation_mode == '2_pass_auto_cot_self_consistency':
            logger.info(f"sql wrapper: {sql_generation_mode}")
            resp = await self.sql_agent_act.tasks_run(question=question, filter_condition=filter_condition, model_name=model_name, fields_list=fields_list, current_date=data_date, prior_schema_links=prior_schema_links, prior_exts=prior_exts, llm_config=llm_config, terms_list=terms_list)
            return resp
        else:
            raise ValueError(f'sql_generation_mode:{sql_generation_mode} is not in SqlModeEnum')

    def update_configs(self, sql_example_ids:List[str], sql_example_units: List[Mapping[str, str]],
                    num_examples: int, num_fewshots: int, num_self_consistency: int):
        self.sql_agent_act.reload_setting(sql_example_ids=sql_example_ids, sql_example_units=sql_example_units, num_examples=num_examples, num_fewshots=num_fewshots, num_self_consistency=num_self_consistency)

    def add_examples(self, sql_example_ids:List[str], sql_example_units: List[Mapping[str, str]]):
        self.sql_agent_act.add_examples(sql_example_ids=sql_example_ids, sql_example_units=sql_example_units)

    def update_examples(self, sql_example_ids:List[str], sql_example_units: List[Mapping[str, str]]):
        self.sql_agent_act.update_examples(sql_example_ids=sql_example_ids, sql_example_units=sql_example_units)

    def delete_examples(self, sql_example_ids:List[str]):
        self.sql_agent_act.delete_examples(sql_example_ids=sql_example_ids)

    def get_examples(self, sql_example_ids: List[str]):
        sql_agent_act_examples = self.sql_agent_act.get_examples(sql_example_ids=sql_example_ids)

        return sql_agent_act_examples
    
    def count_examples(self):
        sql_agent_examples_act_cnt = self.sql_agent_act.count_examples()

        return sql_agent_examples_act_cnt
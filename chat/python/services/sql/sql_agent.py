import os
import sys
from typing import List, Union, Mapping, Any
from collections import Counter
import random
import asyncio
from langchain.llms.base import BaseLLM
 
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger

from sql.constructor import FewShotPromptTemplate2
from sql.output_parser import  schema_link_parse, combo_schema_link_parse, combo_sql_parse


class Text2DSLAgent(object):
        def __init__(self, num_fewshots:int, 
                    sql_example_prompter:FewShotPromptTemplate2,
                    llm: BaseLLM):
            self.num_fewshots = num_fewshots
            self.sql_example_prompter = sql_example_prompter
            self.llm = llm

        def reload_setting(self, sql_example_ids: List[str], sql_example_units: List[Mapping[str,str]], num_fewshots: int):
            self.num_fewshots = num_fewshots
            
            self.sql_example_prompter.reload_few_shot_example(sql_example_ids, sql_example_units)

        def add_examples(self, sql_example_ids: List[str], sql_example_units: List[Mapping[str,str]]):
                self.sql_example_prompter.add_few_shot_example(sql_example_ids, sql_example_units)

        def update_examples(self, sql_example_ids: List[str], sql_example_units: List[Mapping[str,str]]):
            self.sql_example_prompter.update_few_shot_example(sql_example_ids, sql_example_units)

        def delete_examples(self, sql_example_ids: List[str]):
            self.sql_example_prompter.delete_few_shot_example(sql_example_ids)

        def count_examples(self):
            return self.sql_example_prompter.count_few_shot_example()

        def get_fewshot_examples(self, query_text: str, filter_condition: Mapping[str,str])->List[Mapping[str, str]]:
            few_shot_example_meta_list = self.sql_example_prompter.retrieve_few_shot_example(query_text, self.num_fewshots, filter_condition)

            return few_shot_example_meta_list

        def generate_schema_linking_prompt(self, user_query: str, domain_name: str, fields_list: List[str],
                        prior_schema_links: Mapping[str,str], fewshot_example_list:List[Mapping[str, str]])-> str:
            
            prior_schema_links_str = '['+ ','.join(["""'{}'->{}""".format(k,v) for k,v in prior_schema_links.items()]) + ']'

            instruction = "# 根据数据库的表结构,参考先验信息,找出为每个问题生成SQL查询语句的schema_links"
            
            schema_linking_example_keys = ["tableName", "fieldsList", "priorSchemaLinks", "question", "analysis", "schemaLinks"]
            schema_linking_example_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n问题:{question}\n分析:{analysis} 所以Schema_links是:\nSchema_links:{schemaLinks}"
            schema_linking_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=schema_linking_example_template, 
                                                                                            example_keys=schema_linking_example_keys,
                                                                                            few_shot_example_meta_list=fewshot_example_list)

            new_case_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n问题:{question}\n分析: 让我们一步一步地思考。"
            new_case_prompt = new_case_template.format(tableName=domain_name, fieldsList=fields_list, priorSchemaLinks=prior_schema_links_str, question=user_query)

            schema_linking_prompt = instruction + '\n\n' + schema_linking_fewshot_prompt + '\n\n' + new_case_prompt
            return schema_linking_prompt

        def generate_sql_prompt(self, user_query: str, domain_name: str,
                            schema_link_str: str, data_date: str, 
                            fewshot_example_list:List[Mapping[str, str]])-> str:
            instruction = "# 根据schema_links为每个问题生成SQL查询语句"
            sql_example_keys = ["question", "currentDate", "tableName", "schemaLinks", "sql"]
            sql_example_template = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\nSchema_links:{schemaLinks}\nSQL:{sql}"

            sql_example_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=sql_example_template, 
                                                                                            example_keys=sql_example_keys,
                                                                                            few_shot_example_meta_list=fewshot_example_list)

            new_case_template = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\nSchema_links:{schemaLinks}\nSQL:"
            new_case_prompt = new_case_template.format(question=user_query, currentDate=data_date, tableName=domain_name, schemaLinks=schema_link_str)

            sql_example_prompt = instruction + '\n\n' + sql_example_fewshot_prompt + '\n\n' + new_case_prompt

            return sql_example_prompt

        def generate_schema_linking_sql_prompt(self, user_query: str,
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
            new_case_prompt = new_case_template.format(tableName=domain_name, fieldsList=fields_list, priorSchemaLinks=prior_schema_links_str, currentDate=data_date, question=user_query)

            prompt = instruction + '\n\n' + fewshot_prompt + '\n\n' + new_case_prompt

            return prompt

        async def async_query2sql(self, query_text: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    data_date: str, prior_schema_links: Mapping[str,str], prior_exts: str):
            logger.info("query_text: {}".format(query_text))
            logger.info("model_name: {}".format(model_name))
            logger.info("fields_list: {}".format(fields_list))
            logger.info("data_date: {}".format(data_date))
            logger.info("prior_schema_links: {}".format(prior_schema_links))
            logger.info("prior_exts: {}".format(prior_exts))

            if prior_exts != '':
                query_text = query_text + ' 备注:'+prior_exts
            logger.info("query_text_prior_exts: {}".format(query_text))

            fewshot_example_meta_list = self.get_fewshot_examples(query_text, filter_condition)
            schema_linking_prompt = self.generate_schema_linking_prompt(query_text, model_name, fields_list, prior_schema_links, fewshot_example_meta_list)
            logger.debug("schema_linking_prompt->{}".format(schema_linking_prompt))
            schema_link_output = await self.llm._call_async(schema_linking_prompt)

            schema_link_str = schema_link_parse(schema_link_output)
            
            sql_prompt = self.generate_sql_prompt(query_text, model_name, schema_link_str, data_date, fewshot_example_meta_list)
            logger.debug("sql_prompt->{}".format(sql_prompt))
            sql_output = await self.llm._call_async(sql_prompt)

            resp = dict()
            resp['query'] = query_text
            resp['model'] = model_name
            resp['fields'] = fields_list
            resp['priorSchemaLinking'] = prior_schema_links
            resp['dataDate'] = data_date

            resp['schemaLinkingOutput'] = schema_link_output
            resp['schemaLinkStr'] = schema_link_str
            
            resp['sqlOutput'] = sql_output

            logger.info("resp: {}".format(resp))

            return resp

        async def async_query2sql_shortcut(self, query_text: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    data_date: str, prior_schema_links: Mapping[str,str], prior_exts: str):
            logger.info("query_text: {}".format(query_text))
            logger.info("model_name: {}".format(model_name))
            logger.info("fields_list: {}".format(fields_list))
            logger.info("data_date: {}".format(data_date))
            logger.info("prior_schema_links: {}".format(prior_schema_links))
            logger.info("prior_exts: {}".format(prior_exts))

            if prior_exts != '':
                query_text = query_text + ' 备注:'+prior_exts
            logger.info("query_text_prior_exts: {}".format(query_text))
            
            fewshot_example_meta_list = self.get_fewshot_examples(query_text, filter_condition)
            schema_linking_sql_shortcut_prompt = self.generate_schema_linking_sql_prompt(query_text, model_name, data_date, fields_list, prior_schema_links, fewshot_example_meta_list)
            logger.debug("schema_linking_sql_shortcut_prompt->{}".format(schema_linking_sql_shortcut_prompt))
            schema_linking_sql_shortcut_output = await self.llm._call_async(schema_linking_sql_shortcut_prompt)

            schema_linking_str = combo_schema_link_parse(schema_linking_sql_shortcut_output)
            sql_str = combo_sql_parse(schema_linking_sql_shortcut_output)

            resp = dict()
            resp['query'] = query_text
            resp['model'] = model_name
            resp['fields'] = fields_list
            resp['priorSchemaLinking'] = prior_schema_links
            resp['dataDate'] = data_date

            resp['schemaLinkingComboOutput'] = schema_linking_sql_shortcut_output
            resp['schemaLinkStr'] = schema_linking_str
            resp['sqlOutput'] = sql_str

            logger.info("resp: {}".format(resp))

            return resp

class Text2DSLAgentConsistency(object):
    def __init__(self, num_fewshots:int, num_examples:int, num_self_consistency:int,
                sql_example_prompter:FewShotPromptTemplate2, llm: BaseLLM) -> None:
        self.num_fewshots = num_fewshots
        self.num_examples = num_examples
        assert self.num_fewshots <= self.num_examples
        self.num_self_consistency = num_self_consistency

        self.llm = llm
        self.sql_example_prompter = sql_example_prompter

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

    def count_examples(self):
        return self.sql_example_prompter.count_few_shot_example()

    def get_examples_candidates(self, query_text: str, filter_condition: Mapping[str, str])->List[Mapping[str, str]]:
        few_shot_example_meta_list = self.sql_example_prompter.retrieve_few_shot_example(query_text, self.num_examples, filter_condition)

        return few_shot_example_meta_list
        
    def get_fewshot_example_combos(self, example_meta_list:List[Mapping[str, str]])-> List[List[Mapping[str, str]]]:
        fewshot_example_list = []
        for i in range(0, self.num_self_consistency):
            random.shuffle(example_meta_list)
            fewshot_example_list.append(example_meta_list[:self.num_fewshots])

        return fewshot_example_list
        
    def generate_schema_linking_prompt(self, user_query: str, domain_name: str, fields_list: List[str],
                        prior_schema_links: Mapping[str,str], fewshot_example_list:List[Mapping[str, str]])-> str:
            
        prior_schema_links_str = '['+ ','.join(["""'{}'->{}""".format(k,v) for k,v in prior_schema_links.items()]) + ']'

        instruction = "# 根据数据库的表结构,参考先验信息,找出为每个问题生成SQL查询语句的schema_links"
        
        schema_linking_example_keys = ["tableName", "fieldsList", "priorSchemaLinks", "question", "analysis", "schemaLinks"]
        schema_linking_example_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n问题:{question}\n分析:{analysis} 所以Schema_links是:\nSchema_links:{schemaLinks}"
        schema_linking_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=schema_linking_example_template, 
                                                                                        example_keys=schema_linking_example_keys,
                                                                                        few_shot_example_meta_list=fewshot_example_list)

        new_case_template = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n问题:{question}\n分析: 让我们一步一步地思考。"
        new_case_prompt = new_case_template.format(tableName=domain_name, fieldsList=fields_list, priorSchemaLinks=prior_schema_links_str, question=user_query)

        schema_linking_prompt = instruction + '\n\n' + schema_linking_fewshot_prompt + '\n\n' + new_case_prompt
        return schema_linking_prompt

    def generate_schema_linking_prompt_pool(self, user_query: str, domain_name: str, fields_list: List[str],
                                            prior_schema_links: Mapping[str,str], fewshot_example_list_pool:List[List[Mapping[str, str]]])-> List[str]:
        schema_linking_prompt_pool = []
        for fewshot_example_list in fewshot_example_list_pool:
            schema_linking_prompt = self.generate_schema_linking_prompt(user_query, domain_name, fields_list, prior_schema_links, fewshot_example_list)
            schema_linking_prompt_pool.append(schema_linking_prompt)

        return schema_linking_prompt_pool

    def generate_sql_prompt(self, user_query: str, domain_name: str,
                            schema_link_str: str, data_date: str, 
                            fewshot_example_list:List[Mapping[str, str]])-> str:
        instruction = "# 根据schema_links为每个问题生成SQL查询语句"
        sql_example_keys = ["question", "currentDate", "tableName", "schemaLinks", "sql"]
        sql_example_template = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\nSchema_links:{schemaLinks}\nSQL:{sql}"

        sql_example_fewshot_prompt = self.sql_example_prompter.make_few_shot_example_prompt(few_shot_template=sql_example_template, 
                                                                                        example_keys=sql_example_keys,
                                                                                        few_shot_example_meta_list=fewshot_example_list)

        new_case_template = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\nSchema_links:{schemaLinks}\nSQL:"
        new_case_prompt = new_case_template.format(question=user_query, currentDate=data_date, tableName=domain_name, schemaLinks=schema_link_str)

        sql_example_prompt = instruction + '\n\n' + sql_example_fewshot_prompt + '\n\n' + new_case_prompt

        return sql_example_prompt

    def generate_sql_prompt_pool(self, user_query: str, domain_name: str, data_date: str, 
                                schema_link_str_pool: List[str], fewshot_example_list_pool:List[List[Mapping[str, str]]])-> List[str]:
        sql_prompt_pool = []
        for schema_link_str, fewshot_example_list in zip(schema_link_str_pool, fewshot_example_list_pool):
            sql_prompt = self.generate_sql_prompt(user_query, domain_name, schema_link_str, data_date, fewshot_example_list)
            sql_prompt_pool.append(sql_prompt)

        return sql_prompt_pool

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


    async def generate_schema_linking_tasks(self, user_query: str, domain_name: str, 
                                fields_list: List[str], prior_schema_links: Mapping[str,str], 
                                fewshot_example_list_combo:List[List[Mapping[str, str]]]):

        schema_linking_prompt_pool = self.generate_schema_linking_prompt_pool(user_query, domain_name, 
                                                                            fields_list, prior_schema_links, 
                                                                            fewshot_example_list_combo)
        schema_linking_output_task_pool = [self.llm._call_async(schema_linking_prompt) for schema_linking_prompt in schema_linking_prompt_pool]
        schema_linking_output_res_pool = await asyncio.gather(*schema_linking_output_task_pool)
        logger.debug(f'schema_linking_output_res_pool:{schema_linking_output_res_pool}')

        return schema_linking_output_res_pool

    async def generate_sql_tasks(self, user_query: str, domain_name: str, data_date: str,
                                schema_link_str_pool: List[str], fewshot_example_list_combo:List[List[Mapping[str, str]]]):    
        
        sql_prompt_pool = self.generate_sql_prompt_pool(user_query, domain_name, schema_link_str_pool, data_date, fewshot_example_list_combo)
        sql_output_task_pool = [self.llm._call_async(sql_prompt) for sql_prompt in sql_prompt_pool]
        sql_output_res_pool = await asyncio.gather(*sql_output_task_pool)
        logger.debug(f'sql_output_res_pool:{sql_output_res_pool}')

        return sql_output_res_pool
    
    async def tasks_run(self, user_query: str, filter_condition: Mapping[str, str], domain_name: str, fields_list: List[str], prior_schema_links: Mapping[str,str], data_date: str, prior_exts: str):
        logger.info("user_query: {}".format(user_query))
        logger.info("domain_name: {}".format(domain_name))
        logger.info("fields_list: {}".format(fields_list))
        logger.info("current_date: {}".format(data_date))
        logger.info("prior_schema_links: {}".format(prior_schema_links))
        logger.info("prior_exts: {}".format(prior_exts))

        if prior_exts != '':
            user_query = user_query + ' 备注:'+prior_exts
        logger.info("user_query_prior_exts: {}".format(user_query))

        fewshot_example_meta_list = self.get_examples_candidates(user_query, filter_condition)
        fewshot_example_list_combo = self.get_fewshot_example_combos(fewshot_example_meta_list)

        schema_linking_output_candidates = await self.generate_schema_linking_tasks(user_query, domain_name, fields_list, prior_schema_links, fewshot_example_list_combo)
        schema_linking_candidate_list = [schema_link_parse(schema_linking_output_candidate) for schema_linking_output_candidate in schema_linking_output_candidates]
        logger.debug(f'schema_linking_candidate_list:{schema_linking_candidate_list}')
        schema_linking_candidate_sorted_list = self.schema_linking_list_str_unify(schema_linking_candidate_list)
        logger.debug(f'schema_linking_candidate_sorted_list:{schema_linking_candidate_sorted_list}')

        schema_linking_output_max, schema_linking_output_vote_percentage = self.self_consistency_vote(schema_linking_candidate_sorted_list)

        sql_output_candicates = await self.generate_sql_tasks(user_query, domain_name, data_date, schema_linking_candidate_list,fewshot_example_list_combo)
        logger.debug(f'sql_output_candicates:{sql_output_candicates}')
        sql_output_max, sql_output_vote_percentage = self.self_consistency_vote(sql_output_candicates)

        resp = dict()
        resp['query'] = user_query
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

class Text2DSLAgentWrapper(object):
    def __init__(self, sql_agent:Text2DSLAgent, sql_agent_cs:Text2DSLAgentConsistency, 
                 is_shortcut:bool, is_self_consistency:bool):
        self.sql_agent = sql_agent
        self.sql_agent_cs = sql_agent_cs

        self.is_shortcut = is_shortcut
        self.is_self_consistency = is_self_consistency

    async def async_query2sql(self, query_text: str, filter_condition: Mapping[str,str],
                    model_name: str, fields_list: List[str],
                    data_date: str, prior_schema_links: Mapping[str,str], prior_exts: str):
        if self.is_self_consistency:
            logger.info("sql wrapper: self_consistency")
            resp = await self.sql_agent_cs.tasks_run(user_query=query_text, filter_condition=filter_condition, domain_name=model_name, fields_list=fields_list, prior_schema_links=prior_schema_links, data_date=data_date, prior_exts=prior_exts)
            return resp
        elif self.is_shortcut:
            logger.info("sql wrapper: shortcut")
            resp = await self.sql_agent.async_query2sql_shortcut(query_text=query_text, filter_condition=filter_condition, model_name=model_name, fields_list=fields_list, data_date=data_date, prior_schema_links=prior_schema_links, prior_exts=prior_exts)
            return resp
        else:
            logger.info("sql wrapper: normal")
            resp = await self.sql_agent.async_query2sql(query_text=query_text, filter_condition=filter_condition, model_name=model_name, fields_list=fields_list, data_date=data_date, prior_schema_links=prior_schema_links, prior_exts=prior_exts)
            return resp
    
    def update_configs(self, is_shortcut, is_self_consistency, 
                       sql_examplars, num_examples, num_fewshots, num_self_consistency):
        self.is_shortcut = is_shortcut
        self.is_self_consistency = is_self_consistency

        self.sql_agent.update_examples(sql_examplars=sql_examplars, num_fewshots=num_examples)
        self.sql_agent_cs.update_examples(sql_examplars=sql_examplars, num_examples=num_examples, num_fewshots=num_fewshots, num_self_consistency=num_self_consistency)


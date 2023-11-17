# -*- coding:utf-8 -*-
import os
import sys
from typing import List, Mapping
from chromadb.api import Collection

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from instances.logging_instance import logger
from services.query_retrieval.retriever import ChromaCollectionRetriever

class FewShotPromptTemplate2(object):
    def __init__(self, collection:Collection, retrieval_key:str, few_shot_seperator:str = "\n\n") -> None:
        self.collection = collection
        self.few_shot_retriever = ChromaCollectionRetriever(self.collection)

        self.retrieval_key = retrieval_key

        self.few_shot_seperator  = few_shot_seperator

    def add_few_shot_example(self, example_ids: List[str] , example_units: List[Mapping[str, str]])-> None:
        query_text_list = []
        
        for idx, example_unit in enumerate(example_units):
            query_text_list.append(example_unit[self.retrieval_key])

        self.few_shot_retriever.add_queries(query_text_list=query_text_list, query_id_list=example_ids, metadatas=example_units)

    def update_few_shot_example(self, example_ids: List[str] , example_units: List[Mapping[str, str]])-> None:
        query_text_list = []
        
        for idx, example_unit in enumerate(example_units):
            query_text_list.append(example_unit[self.retrieval_key])

        self.few_shot_retriever.update_queries(query_text_list=query_text_list, query_id_list=example_ids, metadatas=example_units)

    def delete_few_shot_example(self, example_ids: List[str])-> None:
        self.few_shot_retriever.delete_queries_by_ids(query_ids=example_ids)    

    def count_few_shot_example(self)-> int:
        return self.few_shot_retriever.get_query_size()

    def reload_few_shot_example(self, example_ids: List[str] , example_units: List[Mapping[str, str]])-> None:
        logger.info(f"original {self.collection.name} size: {self.few_shot_retriever.get_query_size()}")

        self.few_shot_retriever.empty_query_collection()        
        logger.info(f"emptied {self.collection.name} size: {self.few_shot_retriever.get_query_size()}")

        self.add_few_shot_example(example_ids=example_ids, example_units=example_units)
        logger.info(f"reloaded {self.collection.name} size: {self.few_shot_retriever.get_query_size()}")

    def _sub_dict(self, d:Mapping[str, str], keys:List[str])-> Mapping[str, str]:
        return {k:d[k] for k in keys if k in d}

    def retrieve_few_shot_example(self, query_text: str, retrieval_num: int, filter_condition: Mapping[str,str] =None)-> List[Mapping[str, str]]:
        query_text_list = [query_text]
        retrieval_res_list = self.few_shot_retriever.retrieval_query_run(query_texts_list=query_text_list, 
                                                                        filter_condition=filter_condition, n_results=retrieval_num)
        retrieval_res_unit_list = retrieval_res_list[0]['retrieval']

        return retrieval_res_unit_list

    def make_few_shot_example_prompt(self, few_shot_template: str, example_keys: List[str], 
                                    few_shot_example_meta_list: List[Mapping[str, str]])-> str:
        few_shot_example_str_unit_list = []

        retrieval_metas_list = [self._sub_dict(few_shot_example_meta['metadata'], example_keys) for few_shot_example_meta in few_shot_example_meta_list]

        for meta in retrieval_metas_list:
            few_shot_example_str_unit_list.append(few_shot_template.format(**meta))

        few_shot_example_str = self.few_shot_seperator.join(few_shot_example_str_unit_list)

        return few_shot_example_str 

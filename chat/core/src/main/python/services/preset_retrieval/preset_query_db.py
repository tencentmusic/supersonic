# -*- coding:utf-8 -*-
import os
import sys
import uuid
from typing import Any, List, Mapping

from chromadb.api import Collection

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))


def get_ids(documents: List[str]) -> List[str]:
    ids = []
    for doc in documents:
        ids.append(str(uuid.uuid5(uuid.NAMESPACE_URL, doc)))

    return ids


def add2preset_query_collection(
    collection: Collection, preset_queries: List[str], preset_query_ids: List[str]
) -> None:

    collection.add(documents=preset_queries, ids=preset_query_ids)


def update_preset_query_collection(
    collection: Collection, preset_queries: List[str], preset_query_ids: List[str]
) -> None:

    collection.update(documents=preset_queries, ids=preset_query_ids)


def query2preset_query_collection(
    collection: Collection, query_texts: List[str], n_results: int = 10
):
    collection_cnt = collection.count()
    min_n_results = 10
    min_n_results = min(collection_cnt, min_n_results)

    if n_results > min_n_results:
        res = collection.query(query_texts=query_texts, n_results=n_results)
        return res
    else:
        res = collection.query(query_texts=query_texts, n_results=min_n_results)

        for _key in res.keys():
            if res[_key] is None:
                continue
            for _idx in range(0, len(query_texts)):
                res[_key][_idx] = res[_key][_idx][:n_results]

        return res


def parse_retrieval_preset_query(res: List[Mapping[str, Any]]):
    parsed_res = [[] for _ in range(0, len(res["ids"]))]

    retrieval_ids = res["ids"]
    retrieval_distances = res["distances"]
    retrieval_sentences = res["documents"]

    for query_idx in range(0, len(retrieval_ids)):
        id_ls = retrieval_ids[query_idx]
        distance_ls = retrieval_distances[query_idx]
        sentence_ls = retrieval_sentences[query_idx]

        for idx in range(0, len(id_ls)):
            id = id_ls[idx]
            distance = distance_ls[idx]
            sentence = sentence_ls[idx]

            parsed_res[query_idx].append(
                {"id": id, "distance": distance, "presetQuery": sentence}
            )

    return parsed_res


def preset_query_retrieval_format(
    query_list: List[str], retrieval_list: List[Mapping[str, Any]]
):
    res = []
    for query_idx in range(0, len(query_list)):
        query = query_list[query_idx]
        retrieval = retrieval_list[query_idx]

        res.append({"query": query, "retrieval": retrieval})

    return res


def empty_preset_query_collection(collection: Collection) -> None:
    collection.delete()


def delete_preset_query_by_ids(
    collection: Collection, preset_query_ids: List[str]
) -> None:
    collection.delete(ids=preset_query_ids)


def get_preset_query_by_ids(collection: Collection, preset_query_ids: List[str]):
    res = collection.get(ids=preset_query_ids)

    return res


def preset_query_collection_size(collection: Collection) -> int:
    return collection.count()

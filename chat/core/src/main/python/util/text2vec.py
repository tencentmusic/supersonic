# -*- coding:utf-8 -*-
import os
from typing import Any, List, Mapping, Optional, Union

from langchain.embeddings import HuggingFaceEmbeddings
from chromadb.api.types import Documents, EmbeddingFunction, Embeddings

from run_config import HF_TEXT2VEC_MODEL_NAME

hg_embedding = HuggingFaceEmbeddings(model_name=HF_TEXT2VEC_MODEL_NAME)


class Text2VecEmbeddingFunction(EmbeddingFunction):
    def __call__(self, texts: Documents) -> Embeddings:

        embeddings = hg_embedding.embed_documents(texts)

        return embeddings


def get_embeddings(documents: List[str]) -> List[List[float]]:
    embeddings = hg_embedding.embed_documents(documents)

    return embeddings

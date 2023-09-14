# -*- coding:utf-8 -*-
from typing import Any, List, Mapping, Optional, Union

import chromadb
from chromadb.api import Collection, Documents, Embeddings
from chromadb.config import Settings

from run_config import CHROMA_DB_PERSIST_PATH

client = chromadb.Client(Settings(
    chroma_db_impl="duckdb+parquet",
    persist_directory=CHROMA_DB_PERSIST_PATH # Optional, defaults to .chromadb/ in the current directory
))


def empty_chroma_collection_2(collection:Collection):
    collection_name = collection.name
    client = collection._client
    metadata = collection.metadata
    embedding_function = collection._embedding_function

    client.delete_collection(collection_name)

    new_collection = client.get_or_create_collection(name=collection_name,
                                                    metadata=metadata,
                                                    embedding_function=embedding_function)

    size_of_new_collection = new_collection.count()

    print(f'Collection {collection_name} emptied. Size of new collection: {size_of_new_collection}')

    return new_collection


def empty_chroma_collection(collection:Collection):
    collection.delete()


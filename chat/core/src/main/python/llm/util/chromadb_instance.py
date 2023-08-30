# -*- coding:utf-8 -*-
import chromadb
from chromadb.config import Settings

from run_config import CHROMA_DB_PERSIST_PATH

client = chromadb.Client(Settings(
    chroma_db_impl="duckdb+parquet",
    persist_directory=CHROMA_DB_PERSIST_PATH # Optional, defaults to .chromadb/ in the current directory
))
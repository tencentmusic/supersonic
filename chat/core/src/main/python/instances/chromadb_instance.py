# -*- coding:utf-8 -*-
from typing import Any, List, Mapping, Optional, Union

import chromadb
from chromadb.api import Collection
from chromadb.config import Settings

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config.config_parse import CHROMA_DB_PERSIST_PATH


client = chromadb.Client(
    Settings(
        chroma_db_impl="duckdb+parquet",
        persist_directory=CHROMA_DB_PERSIST_PATH,  # Optional, defaults to .chromadb/ in the current directory
    )
)
# -*- coding:utf-8 -*-
import os
import logging
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from typing import Any, Mapping

from fastapi import FastAPI, HTTPException

from sql.run import query2sql

app = FastAPI()


@app.post("/query2sql/")
async def din_query2sql(query_body: Mapping[str, Any]):
  if 'queryText' not in query_body:
    raise HTTPException(status_code=400,
                        detail="query_text is not in query_body")
  else:
    query_text = query_body['queryText']

  if 'schema' not in query_body:
    raise HTTPException(status_code=400, detail="schema is not in query_body")
  else:
    schema = query_body['schema']

  resp = query2sql(query_text=query_text, schema=schema)

  return resp

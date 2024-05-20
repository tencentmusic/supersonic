# -*- coding:utf-8 -*-
import os
import sys

import uvicorn

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import FastAPI

from config.config_parse import LLMPARSER_HOST, LLMPARSER_PORT

from services_router import (query2sql_service, preset_query_service,
                            solved_query_service, retriever_service)


app = FastAPI()

@app.get("/health")
def read_health():
    return {"status": "Healthy"}

app.include_router(preset_query_service.router)
app.include_router(solved_query_service.router)
app.include_router(query2sql_service.router)
#app.include_router(plugin_call_service.router)
app.include_router(retriever_service.router)

if __name__ == "__main__":
    uvicorn.run(app, host=LLMPARSER_HOST, port=LLMPARSER_PORT)

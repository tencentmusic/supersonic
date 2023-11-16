# -*- coding:utf-8 -*-
import os
import sys
from typing import Any, List, Mapping, Optional, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import APIRouter, Depends, HTTPException

from services.plugin_call.run import plugin_selection_run


router = APIRouter()

@router.post("/plugin_selection/")
async def tool_selection(query_body: Mapping[str, Any]):
    if "queryText" not in query_body:
        raise HTTPException(status_code=400, detail="query_text is not in query_body")
    else:
        query_text = query_body["queryText"]

    if "pluginConfigs" not in query_body:
        raise HTTPException(
            status_code=400, detail="pluginConfigs is not in query_body"
        )
    else:
        plugin_configs = query_body["pluginConfigs"]

    resp = plugin_selection_run(query_text=query_text, plugin_configs=plugin_configs)

    return resp


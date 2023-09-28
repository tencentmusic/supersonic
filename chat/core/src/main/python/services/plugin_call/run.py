# -*- coding:utf-8 -*-

import os
import sys
from typing import Any, List, Mapping, Union

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from plugin_call.prompt_construct import (
    construct_plugin_pool_prompt,
    construct_task_prompt,
    plugin_selection_output_parse,
)
from util.llm_instance import llm


def plugin_selection_run(
    query_text: str, plugin_configs: List[Mapping[str, Any]]
) -> Union[Mapping[str, str], None]:

    tools_prompt = construct_plugin_pool_prompt(plugin_configs)

    task_prompt = construct_task_prompt(query_text, tools_prompt)
    llm_output = llm(task_prompt)
    parsed_output = plugin_selection_output_parse(llm_output)

    return parsed_output

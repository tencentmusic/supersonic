# -*- coding:utf-8 -*-
import re

def schema_link_parse(schema_link_output):
    try:
        schema_link_output = schema_link_output.strip()
        pattern = r'Schema_links:(.*)'
        schema_link_output = re.findall(pattern, schema_link_output, re.DOTALL)[0].strip()
    except Exception as e:
        print(e)
        schema_link_output = None

    return schema_link_output
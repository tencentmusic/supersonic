import glob
import sqlite3
import os
import requests
import datetime
import yaml
import json
import time
import jwt

from generate_model_json import build_jsons


def get_authorization():
    exp = time.time() + 100000
    # secret 请和 com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig.tokenAppSecret 保持一致
    secret = "WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ=="
    token= jwt.encode({"token_user_name": "admin","exp": exp}, secret, algorithm="HS512")
    return "Bearer "+token

def get_url_pre():
    current_directory = os.path.dirname(os.path.abspath(__file__))
    config_file=current_directory+"/config/config.yaml"
    with open(config_file, 'r') as file:
        config = yaml.safe_load(file)
    return config["url"]

def get_list(url):
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.get(url=url, headers=header)
    json_data=resp.json()
    if json_data["code"]==200:
        return json_data["data"]
    else:
        return None
def build_domain():
    dict_info = {}
    json_data = {
        "name": "Spider测试数据域",
        "bizName": "internet",
        "sensitiveLevel": 0,
        "parentId": 0,
        "isOpen": 0,
        "viewers": ["admin","tom","jack"],
        "viewOrgs": ["1"],
        "admins": ["admin"],
        "adminOrgs": [],
        "viewer": "admin,tom,jack",
        "viewOrg": "1",
        "adminOrg": "",
        "admin": "admin"
    }
    url=get_url_pre()+"/api/semantic/domain/createDomain"
    header = {"Authorization": get_authorization()}
    resp = requests.post(url=url, headers=header,json=json_data)
    url = get_url_pre()+"/api/semantic/domain/getDomainList"
    domain_list = get_list(url)
    domain_id = domain_list[len(domain_list)-1]["id"]
    dict_info["build"] = build
    dict_info["domain_id"] = domain_id
    return dict_info


def build_model(domain_id, model_file):
    with open(model_file, "r", encoding="utf-8") as f:
        json_data = json.load(f)
    json_data["domainId"] = domain_id
    url = get_url_pre()+"/api/semantic/model/createModel"
    header = {"Authorization": get_authorization()}
    resp = requests.post(url=url, headers=header,json=json_data)
    url = get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
    model_list = get_list(url)
    model_id = model_list[len(model_list)-1]["id"]
    return model_id


def build_model_rela(domain_id, model_id, from_model_id, to_model_id):
    json_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "internet_json", model_id + ".json")
    with open(json_path, "r", encoding="utf-8") as f:
        json_data = json.load(f)
    json_data["domainId"]=domain_id
    json_data["fromModelId"]=from_model_id
    json_data["toModelId"]=to_model_id

    url=get_url_pre()+"/api/semantic/modelRela"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_data)


def get_id_list(data_list):
    id_list=[]
    if data_list is not None:
        for data in data_list:
            id_list.append(data["id"])
    return id_list
def build_dataSet(domain_id, model_ids):
    dimension_lists, metric_lists = [], []
    for model_id in model_ids:
        url = get_url_pre() + "/api/semantic/dimension/getDimensionList/" + str(model_id)
        dimension_lists.append(get_id_list(get_list(url)))
        url = get_url_pre() + "/api/semantic/metric/getMetricList/" + str(model_id)
        metric_lists.append(get_id_list(get_list(url)))

    data_set_model_configs = [{"id": model_id, "includesAll": False, "metrics": metric_list, "dimensions": dimension_list}
                           for model_id, dimension_list, metric_list in zip(model_ids, dimension_lists, metric_lists)]
    json_data={
        "name": "Spider测试数据集",
        "bizName": "internet",
        "description": "Spider测试数据集",
        "typeEnum": "DATASET",
        "sensitiveLevel": 0,
        "domainId": domain_id,
        "dataSetDetail": {
            "dataSetModelConfigs": data_set_model_configs
        },
        "queryConfig": {
            "detailTypeDefaultConfig": {},
            "aggregateTypeDefaultConfig": {"timeDefaultConfig": {"unit": 0, "period": "DAY", "timeMode": "RECENT"}}
        },
        "admins": ["admin"],
        "admin": "admin"
    }
    url=get_url_pre()+"/api/semantic/dataSet"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_data)

    url=get_url_pre()+"/api/semantic/dataSet/getDataSetList?domainId="+str(domain_id)
    print(url)
    resp=get_list(url)
    data={}
    data["id"]=resp[0]["id"]
    dim={model_id: dimension_list for model_id, dimension_list in zip(model_ids, dimension_lists)}
    data["dim"]=dim
    return data


def build_agent(dataSetId):
    json_dict={
        "id":10,
        "enableSearch":1,
        "name":"Spider 测试助手",
        "description":"Spider 测试助手",
        "status":1,
        "examples":[],
        "toolConfig":json.dumps({
           "tools":[{
               "id":1,
               "type":"DATASET",
               "dataSetIds":[dataSetId]
           }]
        }),
        "chatAppConfig": {
            "S2SQL_PARSER": {
                "name": "语义SQL解析",
                "description": "通过大模型做语义解析生成S2SQL",
                "prompt": "#Role: You are a data analyst experienced in SQL languages.\n#Task: You will be provided with a natural language question asked by users,please convert it to a SQL query so that relevant data could be returned by executing the SQL query against underlying database.\n#Rules:\n1.SQL columns and values must be mentioned in the `Schema`, DO NOT hallucinate.\n2.ALWAYS specify time range using `>`,`<`,`>=`,`<=` operator.\n3.DO NOT include time range in the where clause if not explicitly expressed in the `Question`.\n4.DO NOT calculate date range using functions.\n5.ALWAYS use `with` statement if nested aggregation is needed.\n6.ALWAYS enclose alias declared by `AS` command in underscores.\n7.Alias created by `AS` command must be in the same language ast the `Question`.\n#Exemplars: {{exemplar}}\n#Query: Question:{{question}},Schema:{{schema}},SideInfo:{{information}}",
                "enable": True,
                "chatModelId": 1

            },
            "MEMORY_REVIEW": {"enable": False},
            "REWRITE_MULTI_TURN": {"enable": False},
            "S2SQL_CORRECTOR": {"enable": False},
            "SMALL_TALK": {"enable": False},
            "DATA_INTERPRETER": {"enable": False},
            "REWRITE_ERROR_MESSAGE": {"enable": False}
        },
        "dataSetIds":[dataSetId]
    }
    url=get_url_pre()+"/api/chat/agent"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    response = requests.post(url=url, headers=header,json=json_dict)
def build_chat(agentId):
    url=get_url_pre()+"/api/chat/manage/save?chatName=DuSQL问答&agentId="+str(agentId)
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header)

    url=get_url_pre()+"/api/chat/manage/getAll?agentId="+str(agentId)
    data=get_list(url)
    return data[0]["chatId"]
def build_dim_value_dict(modelIds,info):
    url=get_url_pre()+"/api/chat/dict/task"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    data={
        "updateMode":"REALTIME_ADD",
        "modelIds":modelIds,
        "modelAndDimPair":info["dim"]
    }
    print(data)
    resp=requests.post(url=url, headers=header,json=data)


def build_models(domain_id, model_files):
    for model_file in model_files:
        yield build_model(domain_id, model_file)


def build():
    dict_info=build_domain()
    domain_id=dict_info["domain_id"]
    model_files = glob.glob(os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "tmp", "model_[0-9]*.json"))
    if not model_files:
        raise FileNotFoundError("There exists no model_[0-9]*.json file")
    model_ids = build_models(domain_id, model_files)
    model_ids = list(model_ids)
    dataSet_id = build_dataSet(domain_id, model_ids)
    # 我搞不清楚下面这四行代码具体在干啥，与web页面中甚么操作相对应？
    # build_model_rela(domain_id, "model_rela1", model_ids[0], model_ids[1])
    # build_model_rela(domain_id, "model_rela2", model_ids[0], model_ids[2])
    # build_model_rela(domain_id, "model_rela3", model_ids[1], model_ids[2])
    # build_model_rela(domain_id, "model_rela4", model_ids[1], model_ids[3])
    build_agent(dataSet_id["id"])
    agentId=10
    chat_id=build_chat(agentId)
    dict={}
    dict["agent_id"]=agentId
    dict["chat_id"]=chat_id
    return dict, model_ids



if __name__ == '__main__':
    build_jsons()
    dict_info=build()
    print(dict_info)





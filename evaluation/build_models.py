import sqlite3
import os
import requests
import datetime
import yaml
import json
import time
import jwt


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
    dict_info={}
    json_data='{"name":"DuSQL_互联网企业","bizName":"internet","sensitiveLevel":0,"parentId":0,"isOpen":0,"viewers":["admin","tom","jack"],"viewOrgs":["1"],"admins":["admin"],"adminOrgs":[],"viewer":"admin,tom,jack","viewOrg":"1","adminOrg":"","admin":"admin"}'
    json_dict=json.loads(json_data)
    url=get_url_pre()+"/api/semantic/domain/getDomainList"
    domain_list=get_list(url)
    build=False
    if domain_list is None :
        build=True
    else:
        exist=False
        for domain in domain_list:
            if domain["bizName"]=="internet":
               exist=True
               break
        if not exist:
           build=True
    if build:
        url=get_url_pre()+"/api/semantic/domain/createDomain"
        authorization=get_authorization()
        header = {}
        header["Authorization"] =authorization
        resp=requests.post(url=url, headers=header,json=json_dict)
        url=get_url_pre()+"/api/semantic/domain/getDomainList"
        domain_list=get_list(url)
    domain_id=domain_list[len(domain_list)-1]["id"]
    dict_info["build"]=build
    dict_info["domain_id"]=domain_id
    return dict_info
def build_model_1(domain_id):
    json_data='{"name":"公司","bizName":"company","description":"公司","sensitiveLevel":0,"databaseId":1,"domainId":4,"modelDetail":{"queryType":"sql_query","sqlQuery":"SELECT imp_date,company_id,company_name,headquarter_address,company_established_time,founder,ceo,annual_turnover,employee_count FROM company","identifiers":[{"name":"公司id","type":"primary","bizName":"company_id","isCreateDimension":0,"fieldName":"company_id"}],"dimensions":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"},{"name":"公司名称","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"company_name","isTag":0,"fieldName":"company_name"},{"name":"总部地点","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"headquarter_address","isTag":0,"fieldName":"headquarter_address"},{"name":"公司成立时间","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"company_established_time","isTag":0,"fieldName":"company_established_time"},{"name":"创始人","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"founder","isTag":0,"fieldName":"founder"},{"name":"首席执行官","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"ceo","isTag":0,"fieldName":"ceo"}],"measures":[{"name":"年营业额","agg":"SUM","bizName":"annual_turnover","isCreateMetric":1},{"name":"员工数","agg":"SUM","bizName":"employee_count","isCreateMetric":1}],"fields":[{"fieldName":"company_id"},{"fieldName":"imp_date"},{"fieldName":"company_established_time"},{"fieldName":"founder"},{"fieldName":"headquarter_address"},{"fieldName":"ceo"},{"fieldName":"company_name"}],"sqlVariables":[]},"viewers":["admin","tom","jack"],"viewOrgs":["1"],"admins":["admin"],"adminOrgs":[],"viewer":"admin,tom,jack","viewOrg":"1","timeDimension":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"}],"adminOrg":"","admin":"admin"}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
    model_list=get_list(url)
    build=False
    if model_list is None :
        build=True
    else:
        exist=False
        for model in model_list:
            if model["bizName"]=="company":
                exist=True
                break
        if not exist:
            build=True
    if build:
        url=get_url_pre()+"/api/semantic/model/createModel"
        authorization=get_authorization()
        header = {}
        header["Authorization"] =authorization
        resp=requests.post(url=url, headers=header,json=json_dict)
        url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
        model_list=get_list(url)
    model_id=model_list[len(model_list)-1]["id"]
    return model_id

def build_model_2(domain_id):
    json_data='{"name":"品牌","bizName":"brand","description":"品牌","sensitiveLevel":0,"databaseId":1,"domainId":4,"modelDetail":{"queryType":"sql_query","sqlQuery":"SELECT  imp_date,brand_id,brand_name,brand_established_time,company_id,legal_representative,registered_capital FROM brand","identifiers":[{"name":"品牌id","type":"primary","bizName":"brand_id","isCreateDimension":0,"fieldName":"brand_id"},{"name":"公司id","type":"foreign","bizName":"company_id","isCreateDimension":0,"fieldName":"company_id"}],"dimensions":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"},{"name":"品牌名称","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"brand_name","isTag":0,"fieldName":"brand_name"},{"name":"品牌成立时间","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"brand_established_time","isTag":0,"fieldName":"brand_established_time"},{"name":"法定代表人","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"legal_representative","isTag":0,"fieldName":"legal_representative"}],"measures":[{"name":"注册资本","agg":"SUM","bizName":"registered_capital","isCreateMetric":1}],"fields":[{"fieldName":"company_id"},{"fieldName":"brand_id"},{"fieldName":"brand_name"},{"fieldName":"imp_date"},{"fieldName":"brand_established_time"},{"fieldName":"legal_representative"}],"sqlVariables":[]},"viewers":["admin","tom","jack"],"viewOrgs":["1"],"admins":["admin"],"adminOrgs":[],"viewer":"admin,tom,jack","viewOrg":"1","timeDimension":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"}],"adminOrg":"","admin":"admin"}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
    model_list=get_list(url)
    build=False
    if model_list is None :
        build=True
    else:
        exist=False
        for model in model_list:
            if model["bizName"]=="brand":
                exist=True
                break
        if not exist:
            build=True
    if build:
        url=get_url_pre()+"/api/semantic/model/createModel"
        authorization=get_authorization()
        header = {}
        header["Authorization"] =authorization
        resp=requests.post(url=url, headers=header,json=json_dict)
        url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
        model_list=get_list(url)
    model_id=model_list[len(model_list)-1]["id"]
    return model_id

def build_model_3(domain_id):
    json_data='{"createdBy":"admin","updatedBy":"admin","createdAt":1713260936677,"updatedAt":1713260936677,"name":"公司各品牌收入排名","bizName":"company_revenue","description":"公司各品牌收入排名","sensitiveLevel":0,"databaseId":1,"domainId":4,"modelDetail":{"queryType":"sql_query","sqlQuery":"SELECT imp_date,company_id,brand_id,revenue_proportion,profit_proportion,expenditure_proportion FROM company_revenue","identifiers":[{"name":"公司id","type":"foreign","bizName":"company_id","isCreateDimension":0,"fieldName":"company_id"},{"name":"品牌id","type":"foreign","bizName":"brand_id","isCreateDimension":0,"fieldName":"brand_id"}],"dimensions":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"}],"measures":[{"name":"营收占比","agg":"SUM","bizName":"revenue_proportion","isCreateMetric":1,"fieldName":"revenue_proportion"},{"name":"利润占比","agg":"SUM","bizName":"profit_proportion","isCreateMetric":1,"fieldName":"profit_proportion"},{"name":"支出占比","agg":"SUM","bizName":"expenditure_proportion","isCreateMetric":1,"fieldName":"expenditure_proportion"}],"fields":[{"fieldName":"company_id"},{"fieldName":"brand_id"},{"fieldName":"imp_date"},{"fieldName":"expenditure_proportion"},{"fieldName":"revenue_proportion"},{"fieldName":"profit_proportion"}],"sqlVariables":[]},"viewers":["admin","tom","jack"],"viewOrgs":["1"],"admins":["admin"],"adminOrgs":[],"viewer":"admin,tom,jack","viewOrg":"1","timeDimension":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"}],"adminOrg":"","admin":"admin"}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
    model_list=get_list(url)
    build=False
    if model_list is None :
        build=True
    else:
        exist=False
        for model in model_list:
            if model["bizName"]=="company_revenue":
                exist=True
                break
        if not exist:
            build=True
    if build:
        url=get_url_pre()+"/api/semantic/model/createModel"
        authorization=get_authorization()
        header = {}
        header["Authorization"] =authorization
        resp=requests.post(url=url, headers=header,json=json_dict)
        url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
        model_list=get_list(url)
    model_id=model_list[len(model_list)-1]["id"]
    return model_id

def build_model_4(domain_id):
    json_data='{"name":"公司品牌历年收入","bizName":"company_brand_revenue","description":"公司品牌历年收入","sensitiveLevel":0,"databaseId":1,"domainId":4,"modelDetail":{"queryType":"sql_query","sqlQuery":"SELECT imp_date,year_time,brand_id,revenue,profit,revenue_growth_year_on_year,profit_growth_year_on_year FROM company_brand_revenue","identifiers":[{"name":"品牌id","type":"foreign","bizName":"brand_id","isCreateDimension":0,"fieldName":"brand_id"}],"dimensions":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"},{"name":"年份","type":"categorical","dateFormat":"yyyy-MM-dd","isCreateDimension":1,"bizName":"year_time","isTag":0,"fieldName":"year_time"}],"measures":[{"name":"营收","agg":"SUM","bizName":"revenue","isCreateMetric":1},{"name":"利润","agg":"SUM","bizName":"profit","isCreateMetric":1},{"name":"营收同比增长","agg":"SUM","bizName":"revenue_growth_year_on_year","isCreateMetric":1},{"name":"利润同比增长","agg":"SUM","bizName":"profit_growth_year_on_year","isCreateMetric":1}],"fields":[{"fieldName":"brand_id"},{"fieldName":"imp_date"},{"fieldName":"year_time"}],"sqlVariables":[]},"viewers":["admin","tom","jack"],"viewOrgs":["1"],"admins":["admin"],"adminOrgs":[],"viewer":"admin,tom,jack","viewOrg":"1","timeDimension":[{"name":"","type":"time","dateFormat":"yyyy-MM-dd","typeParams":{"isPrimary":"false","timeGranularity":"none"},"isCreateDimension":0,"bizName":"imp_date","isTag":0,"fieldName":"imp_date"}],"adminOrg":"","admin":"admin"}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
    model_list=get_list(url)
    build=False
    if model_list is None :
        build=True
    else:
        exist=False
        for model in model_list:
            if model["bizName"]=="company_brand_revenue":
                exist=True
                break
        if not exist:
            build=True
    if build:
        url=get_url_pre()+"/api/semantic/model/createModel"
        authorization=get_authorization()
        header = {}
        header["Authorization"] =authorization
        resp=requests.post(url=url, headers=header,json=json_dict)
        url=get_url_pre()+"/api/semantic/model/getModelList/"+str(domain_id)
        model_list=get_list(url)
    model_id=model_list[len(model_list)-1]["id"]
    return model_id
def build_model_rela1(domain_id,from_model_id,to_model_id):
    json_data='{"domainId":4,"fromModelId":9,"toModelId":10,"joinType":"inner join","joinConditions":[{"leftField":"company_id","rightField":"company_id","operator":"="}]}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    json_dict["fromModelId"]=from_model_id
    json_dict["toModelId"]=to_model_id

    url=get_url_pre()+"/api/semantic/modelRela"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_dict)

def build_model_rela2(domain_id,from_model_id,to_model_id):
    json_data='{"domainId":4,"fromModelId":9,"toModelId":11,"joinType":"inner join","joinConditions":[{"leftField":"company_id","rightField":"company_id","operator":"="}]}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    json_dict["fromModelId"]=from_model_id
    json_dict["toModelId"]=to_model_id
    url=get_url_pre()+"/api/semantic/modelRela"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_dict)

def build_model_rela3(domain_id,from_model_id,to_model_id):
    json_data='{"domainId":4,"fromModelId":10,"toModelId":11,"joinType":"inner join","joinConditions":[{"leftField":"brand_id","rightField":"brand_id","operator":"="}]}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    json_dict["fromModelId"]=from_model_id
    json_dict["toModelId"]=to_model_id
    url=get_url_pre()+"/api/semantic/modelRela"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_dict)

def build_model_rela4(domain_id,from_model_id,to_model_id):
    json_data='{"domainId":4,"fromModelId":10,"toModelId":12,"joinType":"inner join","joinConditions":[{"leftField":"brand_id","rightField":"brand_id","operator":"="}]}'
    json_dict=json.loads(json_data)
    json_dict["domainId"]=domain_id
    json_dict["fromModelId"]=from_model_id
    json_dict["toModelId"]=to_model_id
    url=get_url_pre()+"/api/semantic/modelRela"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_dict)
def get_id_list(data_list):
    id_list=[]
    if data_list is not None:
        for data in data_list:
            id_list.append(data["id"])
    return id_list
def build_dataSet(domain_id,model_id1,model_id2,model_id3,model_id4):
    url=get_url_pre()+"/api/semantic/dimension/getDimensionList/"+str(model_id1)
    dimension_list1=get_id_list(get_list(url))
    url=get_url_pre()+"/api/semantic/dimension/getDimensionList/"+str(model_id2)
    dimension_list2=get_id_list(get_list(url))
    url=get_url_pre()+"/api/semantic/dimension/getDimensionList/"+str(model_id3)
    dimension_list3=get_id_list(get_list(url))
    url=get_url_pre()+"/api/semantic/dimension/getDimensionList/"+str(model_id4)
    dimension_list4=get_id_list(get_list(url))

    url=get_url_pre()+"/api/semantic/metric/getMetricList/"+str(model_id1)
    metric_list1=get_id_list(get_list(url))
    url=get_url_pre()+"/api/semantic/metric/getMetricList/"+str(model_id2)
    metric_list2=get_id_list(get_list(url))
    url=get_url_pre()+"/api/semantic/metric/getMetricList/"+str(model_id3)
    metric_list3=get_id_list(get_list(url))
    url=get_url_pre()+"/api/semantic/metric/getMetricList/"+str(model_id4)
    metric_list4=get_id_list(get_list(url))

    json_dict={"name":"DuSQL 互联网企业","bizName":"internet","description":"DuSQL互联网企业数据源相关的指标和维度等",
    "typeEnum":"VIEW","sensitiveLevel":0,"domainId":domain_id,"viewDetail":
    {"viewModelConfigs":[{"id":model_id1,"includesAll":False,"metrics":metric_list1,
    "dimensions":dimension_list1},{"id":model_id2,"includesAll":False,
    "metrics":metric_list2,"dimensions":dimension_list2},{"id":model_id3,"includesAll":False,"metrics":metric_list3,"dimensions":dimension_list3},
    {"id":model_id4,"includesAll":False,"metrics":metric_list4,"dimensions":dimension_list4}]},"queryConfig":{"tagTypeDefaultConfig":
    {"dimensionIds":[],"metricIds":[]},"metricTypeDefaultConfig":{"timeDefaultConfig":{"unit":1,"period":"DAY","timeMode":"RECENT"}}},"admins":["admin"],"admin":"admin"}

    json_dict={"name":"DuSQL 互联网企业","bizName":"internet","description":"DuSQL互联网企业数据源相关的指标和维度等","typeEnum":"DATASET","sensitiveLevel":0,"domainId":domain_id,
               "dataSetDetail":{"dataSetModelConfigs":[
                   {"id":model_id1,"includesAll":False,"metrics":metric_list1,"dimensions":dimension_list1},
                   {"id":model_id2,"includesAll":False,"metrics":metric_list2,"dimensions":dimension_list2},
                   {"id":model_id3,"includesAll":False,"metrics":metric_list3,"dimensions":dimension_list3},
                   {"id":model_id4,"includesAll":False,"metrics":metric_list4,"dimensions":dimension_list4}
               ]},
               "queryConfig":{"tagTypeDefaultConfig":{},"metricTypeDefaultConfig":{"timeDefaultConfig":{"unit":0,"period":"DAY","timeMode":"RECENT"}}},"admins":["admin"],"admin":"admin"}
    url=get_url_pre()+"/api/semantic/dataSet"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_dict)

    url=get_url_pre()+"/api/semantic/dataSet/getDataSetList?domainId="+str(domain_id)
    print(url)
    resp=get_list(url)
    data={}
    data["id"]=resp[0]["id"]
    dim={}
    dim[model_id1]=dimension_list1
    dim[model_id2]=dimension_list2
    dim[model_id3]=dimension_list3
    dim[model_id4]=dimension_list4
    data["dim"]=dim
    return  data


def build_agent(dataSetId):
    # json_dict={
    #     "id":10,
    #     "enableSearch":1,
    #     "name":"DuSQL 互联网企业",
    #     "description":"DuSQL",
    #     "status":1,
    #     "examples":[],
    #     "agentConfig":json.dumps({
    #         "tools":[{
    #             "id":1,
    #             "type":"NL2SQL_LLM",
    #             "viewIds":[view_id]
    #         }]
    #     })
    # }
    json_dict={"id":10,
               "enableSearch":1,
               "name":"DuSQL 互联网企业",
               "description":"DuSQL",
               "status":1,
               "examples":[],
               "agentConfig":json.dumps({
                   "tools":[{
                       "id":1,
                       "type":"NL2SQL_LLM",
                       "dataSetIds":[dataSetId]
                   }]
               }),
               "dataSetIds":[dataSetId]}
    url=get_url_pre()+"/api/chat/agent"
    authorization=get_authorization()
    header = {}
    header["Authorization"] =authorization
    resp=requests.post(url=url, headers=header,json=json_dict)
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


def build():
    dict_info=build_domain()
    domain_id=dict_info["domain_id"]
    if dict_info["build"]:
        model_id1=build_model_1(domain_id)
        model_id2=build_model_2(domain_id)
        model_id3=build_model_3(domain_id)
        model_id4=build_model_4(domain_id)
        dataSet_id=build_dataSet(domain_id,model_id1,model_id2,model_id3,model_id4)
        build_model_rela1(domain_id,model_id1,model_id2)
        build_model_rela2(domain_id,model_id1,model_id3)
        build_model_rela3(domain_id,model_id2,model_id3)
        build_model_rela4(domain_id,model_id2,model_id4)
        build_agent(dataSet_id["id"])
        agentId=10
        chat_id=build_chat(agentId)
        dict={}
        dict["agent_id"]=agentId
        dict["chat_id"]=chat_id
    else:
        agentId=10
        chat_id=build_chat(agentId)
        dict={}
        dict["agent_id"]=agentId
        dict["chat_id"]=chat_id
    return dict



if __name__ == '__main__':
    dict_info=build()
    print(dict_info)





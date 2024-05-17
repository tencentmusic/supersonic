import time

import requests
import logging
import json
import os
import yaml
from build_models import build,get_authorization

def read_query(input_path):
    result=[]
    with open(input_path, "r") as f:
        for line in f.readlines():
            line = line.strip('\n')
            result.append(line)
    return result
def write_sql(output_path,result):
    file = open(output_path, mode='a')
    file.writelines(result)
    file.close()
def get_pred_sql(query,url,agentId,chatId,authorization,default_sql):
    url=url+"/api/chat/query/parse"
    data = {"agentId": agentId, "chatId":chatId,"queryText":query}
    header = {}
    header["Authorization"] =authorization
    try:
        result = requests.post(url=url, headers=header, json=data)
        #print(result.json())
        print(result.json()["traceId"])
        if result.status_code == 200:
            data = result.json()["data"]
            selectedParses = data["selectedParses"]
            if selectedParses is not None and len(selectedParses) > 0:
                querySQL = selectedParses[0]["sqlInfo"]["querySQL"]
                if querySQL is not None:
                   querySQL=querySQL.replace("`dusql`.", "").replace("dusql", "").replace("\n", "")
                   return querySQL+'\n'
        return default_sql+'\n'
    except Exception as e:
        print(url)
        print(result.json())
        print(e)
        logging.info(e)
        return default_sql+'\n'

def get_pred_result():
    current_directory = os.path.dirname(os.path.abspath(__file__))
    config_file=current_directory+"/config/config.yaml"
    with open(config_file, 'r') as file:
        config = yaml.safe_load(file)
    input_path=current_directory+"/data/internet.txt"
    pred_sql_path = current_directory+"/data/pred_example_dusql.txt"
    pred_sql_exist=os.path.exists(pred_sql_path)
    if pred_sql_exist:
        os.remove(pred_sql_path)
        print("pred_sql_path removed!")
    dict_info=build()
    print(dict_info)
    agent_id=dict_info["agent_id"]
    chat_id=dict_info["chat_id"]
    url=config["url"]
    authorization=get_authorization()
    print(input_path)
    print(pred_sql_path)
    questions=read_query(input_path)
    pred_sql_list=[]
    default_sql="select * from tablea "
    time_cost=[]
    time.sleep(60)
    for i in range(0,len(questions)):
        start_time = time.time()
        pred_sql=get_pred_sql(questions[i],url,agent_id,chat_id,authorization,default_sql)
        end_time = time.time()
        cost='%.3f'%(end_time-start_time)
        time_cost.append(cost)
        pred_sql_list.append(pred_sql)
        time.sleep(3)
    write_sql(pred_sql_path, pred_sql_list)

    return [float(cost) for cost in time_cost]

if __name__ == "__main__":
    print("pred")



import requests

from build_models import get_url_pre, get_authorization


def get_metric_ids(model_id):
    url = get_url_pre() + "/api/semantic/metric/queryMetric"
    header = {"Authorization": get_authorization()}
    json_data = {
        "current": 1,
        "pageSize": 20,
        "total": 0,
        "modelId": model_id,
        "modelIds": [model_id]
    }
    response = requests.post(url, json=json_data, headers=header)
    response = response.json()
    metric_ids = [element["id"] for element in response["data"]["list"]]
    return metric_ids


def get_dimension_ids(model_id):
    url = get_url_pre() + "/api/semantic/dimension/queryDimension"
    header = {"Authorization": get_authorization()}
    json_data = {
        "current": 1,
        "pageSize": 20,
        "total": 0,
        "modelId": model_id,
        "modelIds": [model_id]
    }
    response = requests.post(url, json=json_data, headers=header)
    response = response.json()
    dimension_ids = [element["id"] for element in response["data"]["list"]]
    return dimension_ids


def delete_dimensions(dimension_ids):
    url = get_url_pre() + "/api/semantic/dimension/batchUpdateStatus"
    header = {"Authorization": get_authorization()}
    json_data = {
        "ids": dimension_ids,
        "status": 3
    }
    response = requests.post(url, json=json_data, headers=header)
    if response.status_code == 200:
        print("dimension of ids: ", dimension_ids, "are deleted successfully")


def delete_metrics(metric_ids):
    url = get_url_pre() + "/api/semantic/metric/batchUpdateStatus"
    header = {"Authorization": get_authorization()}
    json_data = {
        "ids": metric_ids,
        "status": 3
    }
    response = requests.post(url, json=json_data, headers=header)
    if response.status_code == 200:
        print("metrics of ids: ", metric_ids, "are deleted successfully")


def delete_model(model_id):
    url = get_url_pre() + f"/api/semantic/model/deleteModel/{model_id}"
    header = {"Authorization": get_authorization()}
    response = requests.delete(url, headers=header)
    if response.status_code == 200:
        print("model of id: ", model_id, "is deleted successfully")


def delete_models(model_ids):
    for model_id in model_ids:
        metric_ids = get_metric_ids(model_id)
        dimension_ids = get_dimension_ids(model_id)
        delete_dimensions(dimension_ids)
        delete_metrics(metric_ids)
        delete_model(model_id)


def delete_dataset(tmp_dataset_id=4):
    url = get_url_pre() + f"/api/semantic/dataSet/{tmp_dataset_id}"
    header = {"Authorization": get_authorization()}
    response = requests.delete(url, headers=header)
    if response.status_code == 200:
        print("dataset of id: ", tmp_dataset_id, "is deleted successfully")


def delete_domain(domain_id=4):
    url = get_url_pre() + f"/api/semantic/domain//deleteDomain/{domain_id}"
    header = {"Authorization": get_authorization()}
    response = requests.delete(url, headers=header)
    if response.status_code == 200:
        print("domain of id: ", domain_id, "is deleted successfully")


def delete_agent(agent_id=10):
    url = get_url_pre() + f"/api/chat/agent/{agent_id}"
    header = {"Authorization": get_authorization()}
    response = requests.delete(url, headers=header)
    if response.status_code == 200:
        print("agent of id: ", agent_id, "is deleted successfully")

if __name__ == '__main__':
    # 删除“企业数据域”2 “企业数据集” 2 和所有模型4， 5， 6。删除其对应的agent
    delete_models([8, 9])
    delete_dataset(10)
    delete_domain(4)
    delete_agent(10)
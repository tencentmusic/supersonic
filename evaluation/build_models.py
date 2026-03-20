import json
from typing import Dict, List

from eval_client import EvalClient, EvalApiError, find_by
from eval_config import load_config


def get_authorization():
    config = load_config()
    return EvalClient(config).session.headers["Authorization"]


def _admin_fields():
    return {
        "viewers": ["admin", "tom", "jack"],
        "viewOrgs": ["1"],
        "admins": ["admin"],
        "adminOrgs": [],
        "viewer": "admin,tom,jack",
        "viewOrg": "1",
        "adminOrg": "",
        "admin": "admin",
    }


def _model_configs(prefix: str, database_id: int) -> List[Dict]:
    return [
        {
            "name": f"{prefix} 公司",
            "bizName": f"{prefix}_company",
            "description": "公司",
            "sensitiveLevel": 0,
            "databaseId": database_id,
            "modelDetail": {
                "queryType": "sql_query",
                "sqlQuery": "SELECT imp_date,company_id,company_name,headquarter_address,company_established_time,founder,ceo,annual_turnover,employee_count FROM company",
                "identifiers": [
                    {"name": "公司id", "type": "primary", "bizName": "company_id", "isCreateDimension": 0, "fieldName": "company_id"}
                ],
                "dimensions": [
                    {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"},
                    {"name": "公司名称", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "company_name", "isTag": 0, "fieldName": "company_name"},
                    {"name": "总部地点", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "headquarter_address", "isTag": 0, "fieldName": "headquarter_address"},
                    {"name": "公司成立时间", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "company_established_time", "isTag": 0, "fieldName": "company_established_time"},
                    {"name": "创始人", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "founder", "isTag": 0, "fieldName": "founder"},
                    {"name": "首席执行官", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "ceo", "isTag": 0, "fieldName": "ceo"},
                ],
                "measures": [
                    {"name": "年营业额", "agg": "SUM", "bizName": "annual_turnover", "isCreateMetric": 1},
                    {"name": "员工数", "agg": "SUM", "bizName": "employee_count", "isCreateMetric": 1},
                ],
                "fields": [
                    {"fieldName": "company_id"},
                    {"fieldName": "imp_date"},
                    {"fieldName": "company_established_time"},
                    {"fieldName": "founder"},
                    {"fieldName": "headquarter_address"},
                    {"fieldName": "ceo"},
                    {"fieldName": "company_name"},
                ],
                "sqlVariables": [],
            },
            "timeDimension": [
                {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"}
            ],
            **_admin_fields(),
        },
        {
            "name": f"{prefix} 品牌",
            "bizName": f"{prefix}_brand",
            "description": "品牌",
            "sensitiveLevel": 0,
            "databaseId": database_id,
            "modelDetail": {
                "queryType": "sql_query",
                "sqlQuery": "SELECT imp_date,brand_id,brand_name,brand_established_time,company_id,legal_representative,registered_capital FROM brand",
                "identifiers": [
                    {"name": "品牌id", "type": "primary", "bizName": "brand_id", "isCreateDimension": 0, "fieldName": "brand_id"},
                    {"name": "公司id", "type": "foreign", "bizName": "company_id", "isCreateDimension": 0, "fieldName": "company_id"},
                ],
                "dimensions": [
                    {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"},
                    {"name": "品牌名称", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "brand_name", "isTag": 0, "fieldName": "brand_name"},
                    {"name": "品牌成立时间", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "brand_established_time", "isTag": 0, "fieldName": "brand_established_time"},
                    {"name": "法定代表人", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "legal_representative", "isTag": 0, "fieldName": "legal_representative"},
                ],
                "measures": [
                    {"name": "注册资本", "agg": "SUM", "bizName": "registered_capital", "isCreateMetric": 1}
                ],
                "fields": [
                    {"fieldName": "company_id"},
                    {"fieldName": "brand_id"},
                    {"fieldName": "brand_name"},
                    {"fieldName": "imp_date"},
                    {"fieldName": "brand_established_time"},
                    {"fieldName": "legal_representative"},
                ],
                "sqlVariables": [],
            },
            "timeDimension": [
                {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"}
            ],
            **_admin_fields(),
        },
        {
            "name": f"{prefix} 公司各品牌收入排名",
            "bizName": f"{prefix}_company_revenue",
            "description": "公司各品牌收入排名",
            "sensitiveLevel": 0,
            "databaseId": database_id,
            "modelDetail": {
                "queryType": "sql_query",
                "sqlQuery": "SELECT imp_date,company_id,brand_id,revenue_proportion,profit_proportion,expenditure_proportion FROM company_revenue",
                "identifiers": [
                    {"name": "公司id", "type": "foreign", "bizName": "company_id", "isCreateDimension": 0, "fieldName": "company_id"},
                    {"name": "品牌id", "type": "foreign", "bizName": "brand_id", "isCreateDimension": 0, "fieldName": "brand_id"},
                ],
                "dimensions": [
                    {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"}
                ],
                "measures": [
                    {"name": "营收占比", "agg": "SUM", "bizName": "revenue_proportion", "isCreateMetric": 1, "fieldName": "revenue_proportion"},
                    {"name": "利润占比", "agg": "SUM", "bizName": "profit_proportion", "isCreateMetric": 1, "fieldName": "profit_proportion"},
                    {"name": "支出占比", "agg": "SUM", "bizName": "expenditure_proportion", "isCreateMetric": 1, "fieldName": "expenditure_proportion"},
                ],
                "fields": [
                    {"fieldName": "company_id"},
                    {"fieldName": "brand_id"},
                    {"fieldName": "imp_date"},
                    {"fieldName": "expenditure_proportion"},
                    {"fieldName": "revenue_proportion"},
                    {"fieldName": "profit_proportion"},
                ],
                "sqlVariables": [],
            },
            "timeDimension": [
                {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"}
            ],
            **_admin_fields(),
        },
        {
            "name": f"{prefix} 公司品牌历年收入",
            "bizName": f"{prefix}_company_brand_revenue",
            "description": "公司品牌历年收入",
            "sensitiveLevel": 0,
            "databaseId": database_id,
            "modelDetail": {
                "queryType": "sql_query",
                "sqlQuery": "SELECT imp_date,year_time,brand_id,revenue,profit,revenue_growth_year_on_year,profit_growth_year_on_year FROM company_brand_revenue",
                "identifiers": [
                    {"name": "品牌id", "type": "foreign", "bizName": "brand_id", "isCreateDimension": 0, "fieldName": "brand_id"}
                ],
                "dimensions": [
                    {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"},
                    {"name": "年份", "type": "categorical", "dateFormat": "yyyy-MM-dd", "isCreateDimension": 1, "bizName": "year_time", "isTag": 0, "fieldName": "year_time"},
                ],
                "measures": [
                    {"name": "营收", "agg": "SUM", "bizName": "revenue", "isCreateMetric": 1},
                    {"name": "利润", "agg": "SUM", "bizName": "profit", "isCreateMetric": 1},
                    {"name": "营收同比增长", "agg": "SUM", "bizName": "revenue_growth_year_on_year", "isCreateMetric": 1},
                    {"name": "利润同比增长", "agg": "SUM", "bizName": "profit_growth_year_on_year", "isCreateMetric": 1},
                ],
                "fields": [
                    {"fieldName": "brand_id"},
                    {"fieldName": "imp_date"},
                    {"fieldName": "year_time"},
                ],
                "sqlVariables": [],
            },
            "timeDimension": [
                {"name": "", "type": "time", "dateFormat": "yyyy-MM-dd", "typeParams": {"isPrimary": "false", "timeGranularity": "none"}, "isCreateDimension": 0, "bizName": "imp_date", "isTag": 0, "fieldName": "imp_date"}
            ],
            **_admin_fields(),
        },
    ]


def _dataset_request(prefix: str, domain_id: int, model_ids: List[int], dimension_map: Dict[int, List[int]], metric_map: Dict[int, List[int]]) -> Dict:
    return {
        "name": f"{prefix} DuSQL互联网企业",
        "bizName": f"{prefix}_internet",
        "description": "DuSQL互联网企业数据源相关的指标和维度等",
        "typeEnum": "DATASET",
        "sensitiveLevel": 0,
        "domainId": domain_id,
        "dataSetDetail": {
            "dataSetModelConfigs": [
                {
                    "id": model_id,
                    "includesAll": False,
                    "metrics": metric_map[model_id],
                    "dimensions": dimension_map[model_id],
                }
                for model_id in model_ids
            ]
        },
        "queryConfig": {
            "detailTypeDefaultConfig": {},
            "aggregateTypeDefaultConfig": {"timeDefaultConfig": {"unit": 0, "period": "DAY", "timeMode": "RECENT"}},
        },
        "admins": ["admin"],
        "admin": "admin",
    }


def _poll_fields_ready(client: EvalClient, model_id: int):
    def fetch():
        dimensions = client.get(f"/api/semantic/dimension/getDimensionList/{model_id}") or []
        metrics = client.get(f"/api/semantic/metric/getMetricList/{model_id}") or []
        return {"dimensions": dimensions, "metrics": metrics}

    return client.poll_until(
        fetcher=fetch,
        condition=lambda data: len(data["dimensions"]) > 0 or len(data["metrics"]) > 0,
        description=f"model fields for model_id={model_id}",
    )


def build():
    config = load_config()
    client = EvalClient(config)
    prefix = config.resource_prefix

    domain_req = {
        "name": f"{prefix} DuSQL_互联网企业",
        "bizName": f"{prefix}_internet",
        "sensitiveLevel": 0,
        "parentId": 0,
        "isOpen": 0,
        **_admin_fields(),
    }
    domain = client.post("/api/semantic/domain/createDomain", json=domain_req)
    if not domain or "id" not in domain:
        raise EvalApiError("failed to create evaluation domain")
    domain_id = domain["id"]

    model_specs = _model_configs(prefix, config.database_id)
    models = []
    dimension_map = {}
    metric_map = {}
    for model_spec in model_specs:
        model_req = dict(model_spec)
        model_req["domainId"] = domain_id
        model = client.post("/api/semantic/model/createModel", json=model_req)
        model_id = model["id"]
        models.append(model)
        field_info = _poll_fields_ready(client, model_id)
        dimension_map[model_id] = [item["id"] for item in field_info["dimensions"]]
        metric_map[model_id] = [item["id"] for item in field_info["metrics"]]

    relations = [
        (models[0]["id"], models[1]["id"], "company_id", "company_id"),
        (models[0]["id"], models[2]["id"], "company_id", "company_id"),
        (models[1]["id"], models[2]["id"], "brand_id", "brand_id"),
        (models[1]["id"], models[3]["id"], "brand_id", "brand_id"),
    ]
    for from_model_id, to_model_id, left_field, right_field in relations:
        client.post(
            "/api/semantic/modelRela",
            json={
                "domainId": domain_id,
                "fromModelId": from_model_id,
                "toModelId": to_model_id,
                "joinType": "inner join",
                "joinConditions": [{"leftField": left_field, "rightField": right_field, "operator": "="}],
            },
        )

    dataset_req = _dataset_request(
        prefix=prefix,
        domain_id=domain_id,
        model_ids=[model["id"] for model in models],
        dimension_map=dimension_map,
        metric_map=metric_map,
    )
    dataset = client.post("/api/semantic/dataSet", json=dataset_req)
    dataset_id = dataset["id"]

    agent_req = {
        "enableSearch": 1,
        "name": f"{prefix} DuSQL Agent",
        "description": "DuSQL evaluation agent",
        "status": 1,
        "examples": [],
        "toolConfig": json.dumps({"tools": [{"id": 1, "type": "NL2SQL_LLM", "dataSetIds": [dataset_id]}]}),
        "dataSetIds": [dataset_id],
    }
    agent = client.post("/api/chat/agent", json=agent_req)
    agent_id = agent["id"]

    chat_name = f"{prefix} DuSQL问答"
    client.post("/api/chat/manage/save", params={"chatName": chat_name, "agentId": agent_id})
    chats = client.get("/api/chat/manage/getAll", params={"agentId": agent_id}) or []
    chat = find_by(chats, "chatName", chat_name) or (chats[0] if chats else None)
    if not chat:
        raise EvalApiError("failed to create evaluation chat")

    result = {
        "run_id": config.run_id,
        "resource_prefix": prefix,
        "domain_id": domain_id,
        "dataset_id": dataset_id,
        "agent_id": agent_id,
        "chat_id": chat["chatId"],
        "model_ids": [model["id"] for model in models],
    }
    print(result)
    return result


if __name__ == "__main__":
    print(build())

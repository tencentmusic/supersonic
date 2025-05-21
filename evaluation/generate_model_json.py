import sqlite3
import json
import os
import glob


def build_jsons():
    model_files = glob.glob(
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "tmp", "model_[0-9]*.json"))
    for model_file in model_files:
        os.remove(model_file)
    # 我还没有处理time Dimension的转化问题。
    sqlite_path = os.path.join(os.path.dirname(__file__), "data", "tmp", "database.sqlite")
    conn = sqlite3.connect(sqlite_path)
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cursor.fetchall()
    for i, table_name in enumerate(tables):
        table_name = table_name[0]
        cursor.execute(f"PRAGMA table_info({table_name});")
        columns_info = cursor.fetchall()
        column_names = []
        metrics = []
        dimensions = []
        primary_keys = []
        for col in columns_info:
            col_id, name, dtype, notnull, default, pk = col
            column_names.append(name)
            # 判断是 metric 还是 dimension（粗略依据类型）
            dtype_upper = dtype.upper()
            if any(t in dtype_upper for t in ["INT", "FLOAT", "DOUBLE", "REAL", "NUMERIC", "BIGINT"]):
                metrics.append(name)
            else:
                dimensions.append(name)
            # 主键信息
            if pk:
                primary_keys.append(name)
        # 提取外键（通过 PRAGMA foreign_key_list）
        cursor.execute(f"PRAGMA foreign_key_list({table_name})")
        fk_info = cursor.fetchall()
        foreign_keys = [row[3] for row in fk_info]
        print(column_names, metrics, dimensions, primary_keys, foreign_keys, sep="\n")

        json_demo = {
            "name": table_name,
            "bizName": table_name,
            "description": "",
            "sensitiveLevel": 0,
            "databaseId": 1,
            "domainId": 4,
            "modelDetail": {
                "queryType": "sql_query",
                "sqlQuery": f"SELECT {', '.join(column_names)} FROM {table_name}",
                "identifiers": [],
                "dimensions": [],
                "measures": [],
                "fields": [{"fieldName": dimension} for dimension in dimensions],
                "sqlVariables": []
            },
            "timeDimension": [],
            "viewers": ["admin"],
            "viewOrgs": ["1"],
            "admins": ["admin"],
            "adminOrgs": [],
            "viewer": "admin",
            "viewOrg": "1",
            "admin": "admin",
            "adminOrg": ""
        }
        for primary_key in primary_keys:
            tmp = {
                "name": primary_key,
                "type": "primary",
                "bizName": primary_key,
                "isCreateDimension": 0,
                "fieldName": primary_key
            }
            json_demo["modelDetail"]["identifiers"].append(tmp)
        for foreign_key in foreign_keys:
            tmp = {
                "name": foreign_key,
                "type": "foreign",
                "bizName": foreign_key,
                "isCreateDimension": 0,
                "fieldName": foreign_key
            }
            json_demo["modelDetail"]["identifiers"].append(tmp)
        for dimension in dimensions:
            tmp = {
                "name": dimension,
                "type": "categorical",
                "dateFormat": "yyyy-MM-dd",
                "isCreateDimension": 1,
                "bizName": dimension,
                "isTag": 0,
                "fieldName": dimension
            }
            json_demo["modelDetail"]["dimensions"].append(tmp)
        for metric in metrics:
            tmp = {
                "name": metric,
                "agg": "SUM",
                "bizName": metric,
                "isCreateDimension": 1
            }
            json_demo["modelDetail"]["measures"].append(tmp)
        json_file = os.path.join(os.path.dirname(__file__), "data", "tmp", f"model_{i}.json")
        if os.path.exists(json_file):
            os.remove(json_file)
        with open(json_file, "w", encoding="utf-8") as f:
            json.dump(json_demo, f, indent=4, ensure_ascii=False)
    print("all model json files created!")

if __name__ == '__main__':
    build_jsons()
    print("all model json files created!")
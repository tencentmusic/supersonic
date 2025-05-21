import os
import json

from build_models import build
from build_pred_result import get_pred_result
from build_tables import build_table
from delete_domain import delete_models, delete_dataset, delete_domain, delete_agent
from evaluation import remove_unused_file
from generate_model_json import build_jsons

spider_data_path = "D:/python_work/python_workspace/Research_workspace/NL2SQL/data/spider_data"
with open(spider_data_path + "/test.json") as f:
    spider_data = json.load(f)
print("The leng of ./spider_data/test.json is: ", len(spider_data))

db_questions_dcit = {}
for data in spider_data:
    db_id = data["db_id"]
    question = data["question"]
    gold_sql = data["query"]
    if db_id not in db_questions_dcit:
        db_questions_dcit[db_id] = []
        db_questions_dcit[db_id].append((question, gold_sql))
    else:
        db_questions_dcit[db_id].append((question, gold_sql))

for i, (db_id, question_goldSQL) in enumerate(db_questions_dcit.items()):
    if i <= 29:
        continue
    # copy spider's schema.sql file into directory ./tmp/schema.sql
    db_path = os.path.join(spider_data_path, "test_database", db_id, "schema.sql")
    if not os.path.exists(db_path):
        raise FileNotFoundError(f"{db_path} does not exist")
    with open(db_path, "r", encoding="utf-8") as f:
        data = f.read()
    with open(os.path.join(os.path.dirname(__file__), "data", "tmp", "schema.sql"), "w", encoding="utf-8") as f:
        f.write(data)
    # copy spider's questions into ./tmp/questions.txt
    with open(os.path.join(os.path.dirname(__file__), "data", "tmp", "questions.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join([element[0] for element in question_goldSQL]) + "\n")
    # copy spider's gold sqls into ./tmp/gold_sql.txt
    with open(os.path.join(os.path.dirname(__file__), "data", "tmp", "gold_sql.txt"), "a", encoding="utf-8") as f:
        f.write("\n".join([element[1] for element in question_goldSQL]) + "\n")

    build_table()
    build_jsons()
    dic_info, model_ids = build()
    time_cost = get_pred_result(dic_info)
    remove_unused_file()

    delete_models(model_ids)
    delete_dataset(i + 4)
    delete_domain(i + 4)
    delete_agent()
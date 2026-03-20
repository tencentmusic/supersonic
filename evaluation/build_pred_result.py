import json
import logging
import time

from build_models import build
from eval_client import EvalApiError, EvalClient
from eval_config import load_config

FAILED_SQL = "select * from __eval_parse_failed__"


def read_query(input_path):
    result = []
    with open(input_path, "r", encoding="utf-8") as file:
        for line in file.readlines():
            line = line.strip("\n")
            if line:
                result.append(line)
    return result


def write_sql(output_path, result):
    with open(output_path, mode="w", encoding="utf-8") as file:
        file.writelines(result)


def classify_parse_response(payload):
    if payload is None:
        return "empty_response", None
    selected_parses = payload.get("selectedParses") or []
    if not selected_parses:
        return "empty_parse", None
    sql_info = selected_parses[0].get("sqlInfo") or {}
    query_sql = sql_info.get("querySQL")
    if not query_sql:
        return "missing_sql", None
    query_sql = query_sql.replace("`dusql`.", "").replace("dusql", "").replace("\n", "")
    return "success", query_sql


def parse_query(client, agent_id, chat_id, query):
    start_time = time.time()
    try:
        payload = client.post(
            "/api/chat/query/parse",
            json={"agentId": agent_id, "chatId": chat_id, "queryText": query},
        )
        status, sql = classify_parse_response(payload)
        return {
            "query": query,
            "status": status,
            "pred_sql": sql or FAILED_SQL,
            "raw_response": payload,
            "latency_seconds": round(time.time() - start_time, 3),
        }
    except EvalApiError as ex:
        return {
            "query": query,
            "status": "request_failed",
            "pred_sql": FAILED_SQL,
            "error": str(ex),
            "latency_seconds": round(time.time() - start_time, 3),
        }
    except Exception as ex:  # pragma: no cover - defensive guard for ad hoc script execution
        logging.exception("unexpected parse error")
        return {
            "query": query,
            "status": "unexpected_error",
            "pred_sql": FAILED_SQL,
            "error": str(ex),
            "latency_seconds": round(time.time() - start_time, 3),
        }


def wait_until_ready(client, agent_id, chat_id, warmup_query):
    result = client.poll_until(
        fetcher=lambda: parse_query(client, agent_id, chat_id, warmup_query),
        condition=lambda item: item["status"] == "success",
        description="parse readiness",
    )
    print(json.dumps({"warmup_status": result["status"], "latency_seconds": result["latency_seconds"]}, ensure_ascii=False))


def get_pred_result():
    config = load_config()
    if config.pred_sql_path.exists():
        config.pred_sql_path.unlink()
        print("pred_sql_path removed!")

    setup_info = build()
    client = EvalClient(config)
    questions = read_query(config.questions_path)
    if not questions:
        raise EvalApiError("no evaluation questions found")

    wait_until_ready(client, setup_info["agent_id"], setup_info["chat_id"], questions[0])

    query_results = []
    for query in questions:
        result = parse_query(client, setup_info["agent_id"], setup_info["chat_id"], query)
        query_results.append(result)
        if config.query_interval_seconds > 0:
            time.sleep(config.query_interval_seconds)

    write_sql(config.pred_sql_path, [f"{item['pred_sql']}\n" for item in query_results])
    return {
        "setup": setup_info,
        "queries": query_results,
        "time_cost": [item["latency_seconds"] for item in query_results],
    }


if __name__ == "__main__":
    print(json.dumps(get_pred_result(), ensure_ascii=False, indent=2))

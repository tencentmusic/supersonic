import os
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict

import yaml


def _as_bool(value: Any, default: bool) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def _as_int(value: Any, default: int) -> int:
    if value is None or value == "":
        return default
    return int(value)


def _as_float(value: Any, default: float) -> float:
    if value is None or value == "":
        return default
    return float(value)


@dataclass(frozen=True)
class EvalConfig:
    base_dir: Path
    data_dir: Path
    base_url: str
    username: str
    token: str
    token_secret: str
    database_id: int
    request_timeout_seconds: int
    prepare_timeout_seconds: int
    poll_interval_seconds: float
    query_interval_seconds: float
    run_id: str
    resource_prefix: str
    cleanup_local_artifacts: bool
    questions_path: Path
    gold_path: Path
    tables_path: Path
    pred_sql_path: Path
    error_case_path: Path
    report_path: Path
    db_path: Path


def _load_yaml(base_dir: Path) -> Dict[str, Any]:
    config_path = base_dir / "config" / "config.yaml"
    if not config_path.exists():
        return {}
    with config_path.open("r", encoding="utf-8") as file:
        return yaml.safe_load(file) or {}


def load_config() -> EvalConfig:
    base_dir = Path(__file__).resolve().parent
    yaml_config = _load_yaml(base_dir)
    run_id = os.getenv("EVAL_RUN_ID") or yaml_config.get("runId") or datetime.now().strftime(
        "%Y%m%d_%H%M%S"
    )
    run_prefix = os.getenv("EVAL_RUN_PREFIX") or yaml_config.get("runPrefix") or "eval"
    resource_prefix = f"{run_prefix}_{run_id}".lower()
    data_dir = base_dir / "data"

    return EvalConfig(
        base_dir=base_dir,
        data_dir=data_dir,
        base_url=(os.getenv("EVAL_BASE_URL") or yaml_config.get("url") or "http://localhost:9080").rstrip("/"),
        username=os.getenv("EVAL_USERNAME") or yaml_config.get("username") or "admin",
        token=os.getenv("EVAL_TOKEN") or yaml_config.get("token") or "",
        token_secret=os.getenv("EVAL_TOKEN_SECRET")
        or yaml_config.get("tokenSecret")
        or "WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ==",
        database_id=_as_int(os.getenv("EVAL_DATABASE_ID") or yaml_config.get("databaseId"), 1),
        request_timeout_seconds=_as_int(
            os.getenv("EVAL_REQUEST_TIMEOUT_SECONDS") or yaml_config.get("requestTimeoutSeconds"), 30
        ),
        prepare_timeout_seconds=_as_int(
            os.getenv("EVAL_PREPARE_TIMEOUT_SECONDS") or yaml_config.get("prepareTimeoutSeconds"), 180
        ),
        poll_interval_seconds=_as_float(
            os.getenv("EVAL_POLL_INTERVAL_SECONDS") or yaml_config.get("pollIntervalSeconds"), 3.0
        ),
        query_interval_seconds=_as_float(
            os.getenv("EVAL_QUERY_INTERVAL_SECONDS") or yaml_config.get("queryIntervalSeconds"), 0.0
        ),
        run_id=run_id,
        resource_prefix=resource_prefix,
        cleanup_local_artifacts=_as_bool(
            os.getenv("EVAL_CLEANUP_LOCAL_ARTIFACTS") or yaml_config.get("cleanupLocalArtifacts"), True
        ),
        questions_path=base_dir / (yaml_config.get("questionsPath") or "data/internet.txt"),
        gold_path=base_dir / (yaml_config.get("goldPath") or "data/gold_example_dusql.txt"),
        tables_path=base_dir / (yaml_config.get("tablesPath") or "data/tables_dusql.json"),
        pred_sql_path=base_dir / (yaml_config.get("predSqlPath") or "data/pred_example_dusql.txt"),
        error_case_path=base_dir / (yaml_config.get("errorCasePath") or "error_case.json"),
        report_path=base_dir / (yaml_config.get("reportPath") or "evaluation_report.json"),
        db_path=data_dir / (yaml_config.get("dbFileName") or "internet.db"),
    )

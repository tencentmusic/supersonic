#!/usr/bin/env python
# -*- encoding: utf-8 -*-

from __future__ import annotations

"""
批量问答测试脚本。

支持两种输入格式：
1. 单轮：question
2. 多轮：group_id, turn, question
"""

import argparse
import json
import os
import time
import traceback
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Optional

try:
    import pandas as pd
except ImportError:
    pd = None

try:
    import requests
except ImportError:
    requests = None


DEFAULT_SLEEP_SECONDS = 1


def require_pandas():
    if pd is None:
        raise RuntimeError("缺少 pandas 依赖，请先执行 pip install -r requirements.txt")
    return pd


def require_requests():
    if requests is None:
        raise RuntimeError("缺少 requests 依赖，请先执行 pip install -r requirements.txt")
    return requests


@dataclass
class TurnResult:
    group_id: str
    turn: int
    chat_id: int
    question: str
    query_id: Optional[int]
    parse_status: str
    parse_cost: float
    parse_error: str
    execute_status: str
    execute_cost: float
    execute_error: str
    total_cost: float


class BenchmarkReporter:
    def __init__(self, file_name: str = "output"):
        pandas = require_pandas()
        self.file_name = file_name
        self.turn_results: List[TurnResult] = []
        self.db_report = pandas.DataFrame()

    def append_turn(self, turn_result: TurnResult):
        self.turn_results.append(turn_result)

    def detail_df(self) -> pd.DataFrame:
        pandas = require_pandas()
        if not self.turn_results:
            return pandas.DataFrame(columns=[
                'group_id', 'turn', 'chat_id', 'question', 'query_id',
                'parse_status', 'parse_cost', 'parse_error',
                'execute_status', 'execute_cost', 'execute_error', 'total_cost',
            ])
        return pandas.DataFrame([vars(item) for item in self.turn_results])

    def summary_df(self) -> pd.DataFrame:
        detail = self.detail_df()
        if detail.empty:
            pandas = require_pandas()
            return pandas.DataFrame(columns=[
                'group_id', 'chat_id', 'turn_count', 'parse_success_count',
                'execute_success_count', 'avg_parse_cost', 'avg_execute_cost',
                'avg_total_cost', 'max_total_cost', 'min_total_cost',
            ])
        grouped = detail.groupby(['group_id', 'chat_id'], as_index=False).agg(
            turn_count=('turn', 'count'),
            parse_success_count=('parse_status', lambda s: (s == '解析成功').sum()),
            execute_success_count=('execute_status', lambda s: (s == '执行成功').sum()),
            avg_parse_cost=('parse_cost', 'mean'),
            avg_execute_cost=('execute_cost', 'mean'),
            avg_total_cost=('total_cost', 'mean'),
            max_total_cost=('total_cost', 'max'),
            min_total_cost=('total_cost', 'min'),
        )
        return grouped.round(2)

    def print_analysis_result(self):
        detail = self.detail_df()
        if detail.empty:
            print("没有可展示的测试结果")
            return

        print("=== 测试汇总 ===")
        print(f"测试轮次总数 : {len(detail)}")
        print(f"解析成功数量 : {(detail['parse_status'] == '解析成功').sum()}")
        print(f"执行成功数量 : {(detail['execute_status'] == '执行成功').sum()}")
        print(f"解析平均耗时 : {round(detail['parse_cost'].mean(), 2)} 秒")
        print(f"执行平均耗时 : {round(detail['execute_cost'].mean(), 2)} 秒")
        print(f"总平均耗时 : {round(detail['total_cost'].mean(), 2)} 秒")
        print(f"最长耗时 : {round(detail['total_cost'].max(), 2)} 秒")
        print(f"最短耗时 : {round(detail['total_cost'].min(), 2)} 秒")

        summary = self.summary_df()
        if len(summary) > 1:
            print("\n=== 会话汇总 ===")
            print(summary.to_string(index=False))

    def write_outputs(self):
        if not os.path.exists('res'):
            os.makedirs('res')

        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        detail_path = os.path.join('res', f'{self.file_name}_detail_{timestamp}.csv')
        summary_path = os.path.join('res', f'{self.file_name}_summary_{timestamp}.csv')
        html_path = os.path.join('res', f'{self.file_name}_report_{timestamp}.html')

        detail_df = self.detail_df()
        summary_df = self.summary_df()
        detail_df.to_csv(detail_path, index=False)
        summary_df.to_csv(summary_path, index=False)
        self._write_html_report(html_path, summary_df, detail_df, self.db_report)

        print(f"明细结果已保存到 {detail_path}")
        print(f"汇总结果已保存到 {summary_path}")
        print(f"HTML报告已保存到 {html_path}")

        if not self.db_report.empty:
            db_report_path = os.path.join('res', f'{self.file_name}_db_report_{timestamp}.csv')
            self.db_report.to_csv(db_report_path, index=False)
            print(f"数据库结果报告已保存到 {db_report_path}")

    def _write_html_report(self, html_path: str, summary_df: pd.DataFrame,
            detail_df: pd.DataFrame, db_report_df: pd.DataFrame):
        sections = [
            "<html><head><meta charset='utf-8'><title>Benchmark Report</title>",
            "<style>body{font-family:Arial,sans-serif;padding:24px;} "
            "table{border-collapse:collapse;width:100%;margin-bottom:24px;} "
            "th,td{border:1px solid #ddd;padding:8px;vertical-align:top;} "
            "th{background:#f5f5f5;} h1,h2{margin-top:24px;}</style></head><body>",
            "<h1>Benchmark Report</h1>",
            f"<p>生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>",
            "<h2>会话汇总</h2>",
            summary_df.to_html(index=False, border=0, justify='left') if not summary_df.empty
            else "<p>无汇总数据</p>",
            "<h2>逐轮明细</h2>",
            detail_df.to_html(index=False, border=0, justify='left') if not detail_df.empty
            else "<p>无明细数据</p>",
        ]
        if not db_report_df.empty:
            sections.extend([
                "<h2>数据库解析/执行结果</h2>",
                db_report_df.to_html(index=False, border=0, justify='left'),
            ])
        sections.append("</body></html>")
        with open(html_path, 'w', encoding='utf-8') as file:
            file.write("\n".join(sections))


class BatchTest:
    def __init__(self, url: str, agent_id: str, user_name: str):
        self.root_url = url.rstrip('/')
        self.query_url = self.root_url + '/api/chat/query'
        self.manage_url = self.root_url + '/api/chat/manage'
        self.agent_id = agent_id
        self.auth_token = self.__get_authorization(user_name)

    def parse(self, query_text: str, chat_id: int):
        http = require_requests()
        url = self.query_url + '/parse'
        data = {
            'queryText': query_text,
            'agentId': self.agent_id,
            'chatId': chat_id,
        }
        response = http.post(url, headers=self._json_headers(), data=json.dumps(data))
        response.raise_for_status()
        return response.json()

    def execute(self, query_text: str, query_id: int, chat_id: int):
        http = require_requests()
        url = self.query_url + '/execute'
        data = {
            'agentId': self.agent_id,
            'queryText': query_text,
            'parseId': 1,
            'chatId': chat_id,
            'queryId': query_id,
        }
        response = http.post(url, headers=self._json_headers(), data=json.dumps(data))
        response.raise_for_status()
        return response.json()

    def create_chat(self, chat_name: str) -> int:
        http = require_requests()
        response = http.post(
            self.manage_url + '/save',
            headers=self._auth_headers(),
            params={'chatName': chat_name, 'agentId': self.agent_id},
        )
        response.raise_for_status()
        return int(response.json()['data'])

    def read_question_from_csv(self, file_path: str) -> pd.DataFrame:
        pandas = require_pandas()
        return pandas.read_csv(file_path)

    def _auth_headers(self) -> Dict[str, str]:
        return {'Authorization': 'Bearer ' + self.auth_token}

    def _json_headers(self) -> Dict[str, str]:
        headers = self._auth_headers()
        headers['Content-Type'] = 'application/json'
        return headers

    def __get_authorization(self, user_name: str):
        import jwt

        # secret 请和 AuthenticationConfig.tokenAppSecret 保持一致
        secret = "WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ=="
        exp = time.time() + 100000000
        token = jwt.encode({"token_user_name": user_name, "exp": exp}, secret,
                algorithm="HS512")
        return token


def normalize_input(df: pd.DataFrame) -> pd.DataFrame:
    normalized = df.rename(columns={
        'groupId': 'group_id',
        'groupID': 'group_id',
        'turnId': 'turn',
    }).copy()
    if 'question' not in normalized.columns:
        raise ValueError("CSV文件缺少 question 列")
    normalized['question'] = normalized['question'].astype(str).str.strip()
    normalized = normalized[normalized['question'] != '']
    if normalized.empty:
        raise ValueError("CSV文件中没有有效问题")

    if 'group_id' in normalized.columns:
        normalized['group_id'] = normalized['group_id'].astype(str)
        if 'turn' not in normalized.columns:
            normalized['turn'] = normalized.groupby('group_id').cumcount() + 1
        normalized['turn'] = normalized['turn'].astype(int)
        normalized = normalized.sort_values(by=['group_id', 'turn']).reset_index(drop=True)
    else:
        normalized['group_id'] = 'default'
        normalized['turn'] = normalized.index + 1
    return normalized[['group_id', 'turn', 'question']]


def safe_get(payload: Dict, *keys, default=None):
    current = payload
    for key in keys:
        if not isinstance(current, dict):
            return default
        current = current.get(key)
        if current is None:
            return default
    return current


def parse_seconds(milliseconds) -> float:
    if milliseconds is None:
        return 0.0
    return round(float(milliseconds) / 1000, 2)


def safe_json_loads(value):
    if value is None or value == '':
        return {}
    if isinstance(value, dict):
        return value
    try:
        return json.loads(value)
    except Exception:
        return {}


def get_json_path(payload: Dict, path: List[str]):
    current = payload
    for key in path:
        if not isinstance(current, dict):
            return None
        current = current.get(key)
        if current is None:
            return None
    return current


def build_session_chat_ids(batch_test: BatchTest, data: pd.DataFrame, chat_id: Optional[int],
        auto_create_chat: bool) -> Dict[str, int]:
    group_ids = data['group_id'].drop_duplicates().tolist()
    has_multiple_groups = len(group_ids) > 1

    if not has_multiple_groups:
        if chat_id is not None:
            return {group_ids[0]: int(chat_id)}
        if auto_create_chat:
            return {group_ids[0]: batch_test.create_chat(f'benchmark-{group_ids[0]}')}
        raise ValueError("单轮或单会话模式下，必须提供 chatId 或开启 auto-create-chat")

    if chat_id is not None and not auto_create_chat:
        raise ValueError("多组多轮模式下，一个 chatId 无法安全复用到多个会话，请开启 auto-create-chat")

    return {
        group_id: batch_test.create_chat(f'benchmark-{group_id}')
        for group_id in group_ids
    }


def fetch_db_report(db_url: str, detail_df: pd.DataFrame) -> pd.DataFrame:
    pandas = require_pandas()
    if detail_df.empty:
        return pandas.DataFrame()
    try:
        from sqlalchemy import bindparam, create_engine, text
    except ImportError as exception:
        raise RuntimeError("需要安装 SQLAlchemy 及对应数据库驱动后才能拉取数据库报告") from exception

    engine = create_engine(db_url)
    query_ids = [int(query_id) for query_id in detail_df['query_id'].dropna().tolist()]
    if not query_ids:
        return pandas.DataFrame()

    params = {'query_ids': tuple(query_ids)}
    parse_sql = text("""
        SELECT question_id, chat_id, create_time, query_text, parse_info
        FROM s2_chat_parse
        WHERE question_id IN :query_ids
    """).bindparams(bindparam('query_ids', expanding=True))
    query_sql = text("""
        SELECT question_id, chat_id, create_time, query_text, query_result, query_state
        FROM s2_chat_query
        WHERE question_id IN :query_ids
    """).bindparams(bindparam('query_ids', expanding=True))

    with engine.connect() as connection:
        parse_df = pandas.read_sql(parse_sql, connection, params=params)
        query_df = pandas.read_sql(query_sql, connection, params=params)

    if parse_df.empty and query_df.empty:
        return pandas.DataFrame()

    parse_df = parse_df.rename(columns={'question_id': 'query_id'})
    query_df = query_df.rename(columns={'question_id': 'query_id'})
    merged = detail_df.merge(parse_df[['query_id', 'parse_info']], on='query_id', how='left')
    merged = merged.merge(
        query_df[['query_id', 'query_result', 'query_state']], on='query_id', how='left'
    )

    rows = []
    for _, row in merged.iterrows():
        parse_info = safe_json_loads(row.get('parse_info'))
        query_result = safe_json_loads(row.get('query_result'))
        rows.append({
            'group_id': row.get('group_id'),
            'turn': row.get('turn'),
            'chat_id': row.get('chat_id'),
            'query_id': row.get('query_id'),
            'question': row.get('question'),
            'parse_status': row.get('parse_status'),
            'execute_status': row.get('execute_status'),
            's2sql': get_json_path(parse_info, ['sqlInfo', 's2SQL']),
            'correct_s2sql': get_json_path(parse_info, ['sqlInfo', 'correctS2SQL']),
            'query_sql': query_result.get('querySql'),
            'query_results': json.dumps(query_result.get('queryResults'), ensure_ascii=False),
            'query_state': row.get('query_state'),
        })
    return pandas.DataFrame(rows)


def benchmark(url: str, agent_id: str, chat_id: Optional[int], file_path: str, user_name: str,
        db_url: Optional[str], auto_create_chat: bool, sleep_seconds: float):
    batch_test = BatchTest(url, agent_id, user_name)
    raw_df = batch_test.read_question_from_csv(file_path)
    normalized_df = normalize_input(raw_df)

    reporter = BenchmarkReporter(os.path.splitext(os.path.basename(file_path))[0])
    session_chat_ids = build_session_chat_ids(batch_test, normalized_df, chat_id, auto_create_chat)

    for group_id, session_df in normalized_df.groupby('group_id', sort=False):
        current_chat_id = session_chat_ids[group_id]
        print(f"start session: group_id={group_id}, chat_id={current_chat_id}")
        for _, row in session_df.sort_values(by='turn').iterrows():
            question = row['question']
            turn = int(row['turn'])
            print(f"start to ask question(group={group_id}, turn={turn}): {question}")
            try:
                parse_resp = batch_test.parse(question, current_chat_id)
                parse_data = parse_resp.get('data') or {}
                parse_error = parse_data.get('errorMsg') or ''
                parse_status = '解析成功' if not parse_error else '解析失败'
                parse_cost = parse_seconds(
                    safe_get(parse_data, 'parseTimeCost', 'parseTime', default=0)
                )
                query_id = parse_data.get('queryId')

                execute_status = '执行失败'
                execute_cost = 0.0
                execute_error = ''
                if parse_status == '解析成功' and query_id is not None:
                    execute_resp = batch_test.execute(question, int(query_id), current_chat_id)
                    execute_data = execute_resp.get('data') or {}
                    execute_error = execute_data.get('errorMsg') or ''
                    if not execute_error:
                        execute_status = '执行成功'
                    execute_cost = parse_seconds(execute_data.get('queryTimeCost', 0))

                reporter.append_turn(TurnResult(
                    group_id=group_id,
                    turn=turn,
                    chat_id=current_chat_id,
                    question=question,
                    query_id=int(query_id) if query_id is not None else None,
                    parse_status=parse_status,
                    parse_cost=parse_cost,
                    parse_error=parse_error,
                    execute_status=execute_status,
                    execute_cost=execute_cost,
                    execute_error=execute_error,
                    total_cost=round(parse_cost + execute_cost, 2),
                ))
            except Exception as exception:
                print('error:', exception)
                traceback.print_exc()
                reporter.append_turn(TurnResult(
                    group_id=group_id,
                    turn=turn,
                    chat_id=current_chat_id,
                    question=question,
                    query_id=None,
                    parse_status='请求异常',
                    parse_cost=0.0,
                    parse_error=str(exception),
                    execute_status='未执行',
                    execute_cost=0.0,
                    execute_error='',
                    total_cost=0.0,
                ))
            time.sleep(sleep_seconds)

    if db_url:
        reporter.db_report = fetch_db_report(db_url, reporter.detail_df())

    reporter.print_analysis_result()
    reporter.write_outputs()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-u', '--url', type=str, required=True,
            help='url:问答系统url,例如：https://chatdata-dev.test.com')
    parser.add_argument('-a', '--agentId', type=str, required=True, help='agentId：助手ID')
    parser.add_argument('-c', '--chatId', type=int, required=False,
            help='chatId：会话ID。单轮模式可复用；多组多轮模式建议不传，改用自动创建会话')
    parser.add_argument('-f', '--filePath', type=str, required=True,
            help='filePath：问题文件路径, csv格式. 请提前上传到benchmark/data目录下')
    parser.add_argument('-p', '--userName', type=str, required=True,
            help='userName：用户名，用于获取登录token')
    parser.add_argument('--db-url', type=str, required=False,
            help='可选：数据库连接URL，用于拉取每轮 parse/result 生成报告')
    parser.add_argument('--auto-create-chat', action='store_true',
            help='自动创建会话。多组多轮模式下推荐开启')
    parser.add_argument('--sleep-seconds', type=float, default=DEFAULT_SLEEP_SECONDS,
            help='每轮问题之间的等待秒数，默认1秒')
    args = parser.parse_args()

    print('批量测试配置信息[url:', args.url, 'agentId:', args.agentId, 'chatId:', args.chatId,
          'filePath:', args.filePath, 'userName:', args.userName, 'dbUrl:', args.db_url,
          'autoCreateChat:', args.auto_create_chat, ']')
    print('请确认输入的测试信息是否正确:')
    print('1. Yes')
    print('2. No')
    confirm = input()
    if confirm in {'1', 'Yes', 'yes', 'YES'}:
        benchmark(args.url, args.agentId, args.chatId, args.filePath, args.userName,
                  args.db_url, args.auto_create_chat, args.sleep_seconds)
    else:
        print('请重新输入测试配置信息')

#!/usr/bin/env python
# -*- encoding: utf-8 -*-
# -----------------------------------------------------------------------------------
'''
@filename     : batchmark.py
@time         : 2024/06/20
@author       : zhaodongsheng
@Version      : 1.0
@description  : 批量问答测试
'''
# -----------------------------------------------------------------------------------
import pandas as pd
import json
import requests
import time
import jwt
import traceback
import os
from datetime import datetime


class DataFrameAppender:
    def __init__(self,file_name = "output"):
        # 定义表头
        columns = ['问题', '解析状态', '解析耗时', '执行状态', '执行耗时', '总耗时']
        # 创建只有表头的 DataFrame
        self.df = pd.DataFrame(columns=columns)
        self.file_name = file_name

    def append_data(self, new_data):
        # 假设 new_data 是一维数组，将其转换为字典
        columns = ['问题', '解析状态', '解析耗时', '执行状态', '执行耗时', '总耗时']
        new_dict = dict(zip(columns, new_data))
        # 使用 loc 方法追加数据
        self.df.loc[len(self.df)] = new_dict
    def print_analysis_result(self):
        # 测试样例总数
        total_samples = len(self.df)

        # 解析成功数量
        parse_success_count = (self.df['解析状态'] == '解析成功').sum()

        # 执行成功数量
        execute_success_count = (self.df['执行状态'] == '执行成功').sum()

        # 解析平均耗时，保留两位小数
        avg_parse_time = round(self.df['解析耗时'].mean(), 2)

        # 执行平均耗时，保留两位小数
        avg_execute_time = round(self.df['执行耗时'].mean(), 2)

        # 总平均耗时，保留两位小数
        avg_total_time = round(self.df['总耗时'].mean(), 2)

        # 最长耗时，保留两位小数
        max_time = round(self.df['总耗时'].max(), 2)

        # 最短耗时，保留两位小数
        min_time = round(self.df['总耗时'].min(), 2)

        print(f"测试样例总数 : {total_samples}")
        print(f"解析成功数量 : {parse_success_count}")
        print(f"执行成功数量 : {execute_success_count}")
        print(f"解析平均耗时 : {avg_parse_time} 秒")
        print(f"执行平均耗时 : {avg_execute_time} 秒")
        print(f"总平均耗时 : {avg_total_time} 秒")
        print(f"最长耗时 : {max_time} 秒")
        print(f"最短耗时 : {min_time} 秒")

    def write_to_csv(self):
        # 检查 data 文件夹是否存在，如果不存在则创建
        if not os.path.exists('res'):
            os.makedirs('res')
        # 获取当前时间戳
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        # 生成带时间戳的文件名
        file_path = os.path.join('res', f'{self.file_name}_{timestamp}.csv')
        self.df.to_csv(file_path, index=False)
        print(f"测试结果已保存到 {file_path}")

class BatchTest:
    def __init__(self, url, agentId, chatId, userName):
        self.base_url = url + '/api/chat/query/'
        self.agentId = agentId
        self.auth_token = self.__get_authorization(userName)
        self.chatId = chatId

    def parse(self, query_text):
        url = self.base_url + 'parse'
        data = {
            'queryText': query_text,
            'agentId': self.agentId,
            'chatId': self.chatId,
        }
        headers = {
            'Authorization': 'Bearer ' + self.auth_token,
            'Content-Type': 'application/json',
        }

        response = requests.post(url, headers=headers, data=json.dumps(data))
        return response.json()

    def execute(self, agentId, query_text, queryId):
        url = self.base_url + 'execute'
        data = {
            'agentId': agentId,
            'queryText': query_text,
            'parseId': 1,
            'chatId': self.chatId,
            'queryId': queryId,
        }
        headers = {
            'Authorization': 'Bearer ' + self.auth_token,
            'Content-Type': 'application/json',
        }

        response = requests.post(url, headers=headers, data=json.dumps(data))
        return response.json()

    def read_question_from_csv(self, filePath):
        df = pd.read_csv(filePath)
        return df

    def __get_authorization(self, userName):
        # secret 请和 com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig.tokenAppSecret 保持一致
        secret = "WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ=="
        exp = time.time() + 100000000
        token= jwt.encode({"token_user_name": userName,"exp": exp}, secret, algorithm="HS512")
        return token


def benchmark(url:str, agentId:str, chatId:str, filePath:str, userName:str):
    batch_test = BatchTest(url, agentId, chatId, userName)
    df = batch_test.read_question_from_csv(filePath)
    appender = DataFrameAppender(os.path.basename(filePath))
    for index, row in df.iterrows():
        question = row['question']
        print('start to ask question:', question)
        # 捕获异常，防止程序中断
        try:
            parse_resp = batch_test.parse(question)
            parse_status = '解析失败'
            if parse_resp.get('data').get('errorMsg') is None:
                parse_status = '解析成功'
            parse_cost = parse_resp.get('data').get('parseTimeCost').get('parseTime')
            execute_resp = batch_test.execute(agentId, question, parse_resp['data']['queryId'])
            execute_status = '执行失败'
            execute_cost = 0
            if parse_status == '解析成功' and execute_resp.get('data').get('errorMsg') is None:
                execute_status = '执行成功'
                execute_cost = execute_resp.get('data').get('queryTimeCost')
            res = [question.replace(',', '#'),parse_status,parse_cost/1000,execute_status,execute_cost/1000,(parse_cost+execute_cost)/1000]
            appender.append_data(res)

        except Exception as e:
            print('error:', e)
            traceback.print_exc()
            continue
        time.sleep(1)
    # 打印分析结果
    appender.print_analysis_result()
    # 分析明细输出
    appender.write_to_csv()

if __name__ == '__main__':

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('-u', '--url', type=str, required=True, help='url:问答系统url,例如：https://chatdata-dev.test.com')
    parser.add_argument('-a', '--agentId', type=str, required=True, help='agentId：助手ID')
    parser.add_argument('-c', '--chatId', type=str, required=True, help='chatId:会话ID,需要通过浏览器开发者模式获取')
    parser.add_argument('-f', '--filePath', type=str, required=True, help='filePath：问题文件路径, csv格式. 请提前上传到benchmark/data目录下')
    parser.add_argument('-p', '--userName', type=str, required=True, help='userName：用户名，用户获取登录token')
    args = parser.parse_args()

    print('批量测试配置信息[url:', args.url,'agentId:', args.agentId, 'chatId:', args.chatId, 'filePath:', args.filePath, 'userName:', args.userName, ']')
    print('请确认输入的压力测试信息是否正确:')
    print('1. Yes')
    print('2. No')
    confirm = input()
    if confirm == '1' or confirm == 'Yes' or confirm == 'yes' or confirm == 'YES':
        benchmark(args.url, args.agentId, args.chatId, args.filePath, args.userName)
    else:
        print('请重新输入压力测试配置信息: url, agentId, chatId, filePath, userName')
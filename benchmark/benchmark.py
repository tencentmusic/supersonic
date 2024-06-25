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

    def execute(self, query_text, queryId):
        url = self.base_url + 'execute'
        data = {
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
        token= jwt.encode({"token_userName": userName,"exp": exp}, secret, algorithm="HS512")
        return token


def benchmark(url:str, agentId:str, chatId:str, filePath:str, userName:str):
    batch_test = BatchTest(url, agentId, chatId, userName)
    df = batch_test.read_question_from_csv(filePath)
    for index, row in df.iterrows():
        question = row['question']
        print('start to ask question:', question)
        # 捕获异常，防止程序中断
        try:
            parse_resp = batch_test.parse(question)
            batch_test.execute(question, parse_resp['data']['queryId'])
        except Exception as e:
            print('error:', e)
            traceback.print_exc()
            continue
        time.sleep(1)

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
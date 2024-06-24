#!/usr/bin/env python
# -*- encoding: utf-8 -*-
# -----------------------------------------------------------------------------------
'''
@filename     : batchmark.py
@time         : 2024/06/20
@author       : zhaodongsheng
@Version      : 1.0
@description  : 批量测试业务问题
'''
# -----------------------------------------------------------------------------------
import pandas as pd
import json
import requests
import time
import jwt

class BatchTest:
    def __init__(self, agentId, chatId, user_name):
        self.base_url = 'https://chatdata-dev.test.seewo.com/api/chat/query/'
        self.agentId = agentId
        self.auth_token = self.__get_authorization(user_name)
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

    def read_question_from_csv(self, file_path):
        df = pd.read_csv(file_path)
        return df

    def __get_authorization(self, user_name):
        exp = time.time() + 100000000
        token= jwt.encode({"token_user_name": user_name,"exp": exp}, "secret", algorithm="HS512")
        return token


def benchmark(agentId:str, chatId:str, file_path:str, user_name:str):
    batch_test = BatchTest(agentId, chatId, user_name)
    df = batch_test.read_question_from_csv(file_path)
    for index, row in df.iterrows():
        question = row['question']
        print('start to ask question:', question)
        # 捕获异常，防止程序中断
        try:
            parse_resp = batch_test.parse(question)
            batch_test.execute(question, parse_resp['data']['queryId'])
        except Exception as e:
            print('error:', e)
            continue
        time.sleep(1)

if __name__ == '__main__':

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('-a', '--agentId', type=str, required=True, help='agentId')
    parser.add_argument('-c', '--chatId', type=str, required=True, help='chatId')
    parser.add_argument('-f', '--file_path', type=str, required=True, help='file_path')
    parser.add_argument('-u', '--user_name', type=str, required=True, help='user_name')

    args = parser.parse_args()
    print('压力测试配置信息[agentId:', args.agentId, 'chatId:', args.chatId, 'file_path:', args.file_path, 'user_name:', args.user_name, ']')
    print('请确认输入的压力测试信息是否正确:')
    print('1. Yes')
    print('2. No')
    confirm = input()
    if confirm == '1' or confirm == 'Yes' or confirm == 'yes' or confirm == 'YES':
        benchmark(args.agentId, args.chatId, args.file_path, args.user_name)
    else:
        print('请重新输入压力测试配置信息: agentId, chatId, file_path, user_name')
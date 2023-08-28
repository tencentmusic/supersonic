#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/../runtime
buildDir=$baseDir/build

cd $baseDir

#2. package lib

tar -zxvf ${buildDir}/supersonic-chat.tar.gz  -C ${runtimeDir}

mv ${runtimeDir}/launchers-chat-* ${runtimeDir}/supersonic-chat

tar -zxvf ${buildDir}/supersonic-webapp.tar.gz  -C ${buildDir}

mkdir -p ${runtimeDir}/supersonic-chat/webapp

cp -fr  ${buildDir}/supersonic-webapp/* ${runtimeDir}/supersonic-chat/webapp

rm -fr  ${buildDir}/supersonic-webapp

json=$(cat ${runtimeDir}/supersonic-chat/webapp/supersonic.config.json)
json=$(echo $json | jq '.env="chat"')
echo $json > ${runtimeDir}/supersonic-chat/webapp/supersonic.config.json

#3. start service
#3.1 start chat service
echo ${runtimeDir}
sh ${runtimeDir}/supersonic-chat/bin/service.sh restart

#3.2 start llm service
sh ${runtimeDir}/supersonic-chat/llm/bin/service.sh restart


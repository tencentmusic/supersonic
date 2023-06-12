#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(readlink -f $sbinDir/../)
runtimeDir=$baseDir/runtime
buildDir=$baseDir/build

cd $baseDir

#1. clear file
rm -fr ${runtimeDir}/*

#2. package lib

tar -zxvf ${buildDir}/supersonic-semantic.tar.gz  -C ${runtimeDir}
tar -zxvf ${buildDir}/supersonic-chat.tar.gz  -C ${runtimeDir}

mv ${runtimeDir}/launchers-chat-1.0.0-SNAPSHOT ${runtimeDir}/supersonic-chat
mv ${runtimeDir}/launchers-semantic-1.0.0-SNAPSHOT ${runtimeDir}/supersonic-semantic

tar -zxvf ${buildDir}/supersonic-webapp.tar.gz  -C ${buildDir}

mkdir -p ${runtimeDir}/supersonic-semantic/webapp
mkdir -p ${runtimeDir}/supersonic-chat/webapp

cp -fr  ${buildDir}/supersonic-webapp/* ${runtimeDir}/supersonic-semantic/webapp
cp -fr  ${buildDir}/supersonic-webapp/* ${runtimeDir}/supersonic-chat/webapp

rm -fr  ${buildDir}/supersonic-webapp

#3. start service
sh ${runtimeDir}/supersonic-semantic/bin/service.sh restart
sleep 5
sh ${runtimeDir}/supersonic-chat/bin/service.sh restart


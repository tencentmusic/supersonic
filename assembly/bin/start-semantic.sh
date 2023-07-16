#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(readlink -f $sbinDir/../)
runtimeDir=$baseDir/../runtime
buildDir=$baseDir/build

cd $baseDir

#1. clear file
mkdir -p ${runtimeDir}
rm -fr ${runtimeDir}/*

#2. package lib

tar -zxvf ${buildDir}/supersonic-semantic.tar.gz  -C ${runtimeDir}

mv ${runtimeDir}/launchers-semantic-* ${runtimeDir}/supersonic-semantic

tar -zxvf ${buildDir}/supersonic-webapp.tar.gz  -C ${buildDir}

mkdir -p ${runtimeDir}/supersonic-semantic/webapp

cp -fr  ${buildDir}/supersonic-webapp/* ${runtimeDir}/supersonic-semantic/webapp

rm -fr  ${buildDir}/supersonic-webapp


json=$(cat ${runtimeDir}/supersonic-semantic/webapp/supersonic.config.json)
json=$(echo $json | jq '.env="semantic"')
echo $json > ${runtimeDir}/supersonic-semantic/webapp/supersonic.config.json

#3. start service
sh ${runtimeDir}/supersonic-semantic/bin/service.sh restart



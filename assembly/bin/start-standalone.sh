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

tar -zxvf ${buildDir}/supersonic.tar.gz  -C ${runtimeDir}

mv ${runtimeDir}/launchers-standalone-* ${runtimeDir}/supersonic-standalone

tar -zxvf ${buildDir}/supersonic-webapp.tar.gz  -C ${buildDir}

mkdir -p ${runtimeDir}/supersonic-standalone/webapp

cp -fr  ${buildDir}/supersonic-webapp/* ${runtimeDir}/supersonic-standalone/webapp

rm -fr  ${buildDir}/supersonic-webapp

#3. start service
#start standalone service
sh ${runtimeDir}/supersonic-standalone/bin/service.sh restart
#start llm service
sh ${runtimeDir}/supersonic-standalone/llm/bin/service.sh restart
#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(readlink -f $sbinDir/../)
runtimeDir=$baseDir/runtime
buildDir=$baseDir/build

cd $baseDir

#1. build semantic chat service
rm -fr ${buildDir}/*.tar.gz
rm -fr dist

mvn -f $baseDir/../ clean package -DskipTests

#2. move package to build
cp $baseDir/../launchers/chat/target/*.tar.gz ${buildDir}/supersonic-chat.tar.gz
cp $baseDir/../launchers/semantic/target/*.tar.gz ${buildDir}/supersonic-semantic.tar.gz

#3. build webapp
chmod +x $baseDir/../webapp/start-fe-prod.sh
cd ../webapp
sh ./start-fe-prod.sh
cp -fr  ./supersonic-webapp.tar.gz ${buildDir}/
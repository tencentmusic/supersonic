#!/usr/bin/env bash

set -x
sbinDir=$(cd "$(dirname "$0")"; pwd)
chmod +x $sbinDir/supersonic-common.sh
source $sbinDir/supersonic-common.sh

cd $baseDir

#1. build backend java modules
rm -fr ${buildDir}/*.tar.gz
rm -fr dist

set +x

mvn -f $baseDir/../ clean package -DskipTests

#2. move package to build
cp $baseDir/../launchers/semantic/target/*.tar.gz ${buildDir}/supersonic-semantic.tar.gz
cp $baseDir/../launchers/chat/target/*.tar.gz ${buildDir}/supersonic-chat.tar.gz
cp $baseDir/../launchers/standalone/target/*.tar.gz ${buildDir}/supersonic-standalone.tar.gz

#3. build frontend webapp
chmod +x $baseDir/../webapp/start-fe-prod.sh
cd ../webapp
sh ./start-fe-prod.sh
cp -fr  ./supersonic-webapp.tar.gz ${buildDir}/

#4. copy webapp to java classpath
cd $buildDir
tar xvf supersonic-webapp.tar.gz
mv supersonic-webapp webapp
cp -fr webapp ../../launchers/semantic/target/classes
cp -fr webapp ../../launchers/chat/target/classes
cp -fr webapp ../../launchers/standalone/target/classes
rm -fr  ${buildDir}/webapp

#5. build backend python modules
echo "start installing python modules with pip: ${pip_path}"
requirementPath=$baseDir/../chat/core/src/main/python/requirements.txt
${pip_path} install -r ${requirementPath}
echo "install python modules success"

#6. reset runtime
rm -fr $runtimeDir/*
moveAllToRuntime
setEnvToWeb chat
setEnvToWeb semantic

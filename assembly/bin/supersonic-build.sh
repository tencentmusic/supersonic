#!/usr/bin/env bash

# pip path
pip_path="/usr/local/bin/pip3"

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/../runtime
buildDir=$baseDir/build

cd $baseDir

#1. build backend java modules
rm -fr ${buildDir}/*.tar.gz
rm -fr dist

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

#5. build backend python modules
requirementPath=$baseDir/../chat/core/src/main/python/requirements.txt
${pip_path}  install -r ${requirementPath}
echo "install python modules success"

#6. reset runtime
function setEnvToWeb {
   model_name=$1
   json='{"env": "'$model_name'"}'
   echo $json > ${runtimeDir}/supersonic-${model_name}/webapp/supersonic.config.json
   echo $json > ../../launchers/${model_name}/target/classes/webapp/supersonic.config.json
}

function moveToRuntime {
  model_name=$1
  tar -zxvf ${buildDir}/supersonic-${model_name}.tar.gz  -C ${runtimeDir}
  mv ${runtimeDir}/launchers-${model_name}-* ${runtimeDir}/supersonic-${model_name}

  mkdir -p ${runtimeDir}/supersonic-${model_name}/webapp
  cp -fr  ${buildDir}/webapp/* ${runtimeDir}/supersonic-${model_name}/webapp
}

mkdir -p ${runtimeDir}
rm -fr $runtimeDir/*

moveToRuntime chat
moveToRuntime semantic
moveToRuntime standalone

setEnvToWeb chat
setEnvToWeb semantic

rm -fr  ${buildDir}/webapp

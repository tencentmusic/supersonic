#!/usr/bin/env bash

set -x
sbinDir=$(cd "$(dirname "$0")"; pwd)
chmod +x $sbinDir/supersonic-common.sh
source $sbinDir/supersonic-common.sh
cd $baseDir

service=$1
#1. build backend java modules
rm -fr ${buildDir}/*.tar.gz
rm -fr dist
set +x
mvn -f $baseDir/../ clean package -DskipTests
# check build result
if [ $? -ne 0 ]; then
    echo "Failed to build backend Java modules."
    exit 1
fi

#2. move package to build
cp $baseDir/../launchers/headless/target/*.tar.gz ${buildDir}/supersonic-headless.tar.gz
cp $baseDir/../launchers/chat/target/*.tar.gz ${buildDir}/supersonic-chat.tar.gz
cp $baseDir/../launchers/standalone/target/*.tar.gz ${buildDir}/supersonic-standalone.tar.gz

#3. build frontend webapp
chmod +x $baseDir/../webapp/start-fe-prod.sh
cd ../webapp
sh ./start-fe-prod.sh
cp -fr  ./supersonic-webapp.tar.gz ${buildDir}/

# check build result
if [ $? -ne 0 ]; then
    echo "Failed to build frontend webapp."
    exit 1
fi
#4. copy webapp to java classpath
cd $buildDir
tar xvf supersonic-webapp.tar.gz
mv supersonic-webapp webapp
cp -fr webapp ../../launchers/headless/target/classes
cp -fr webapp ../../launchers/chat/target/classes
cp -fr webapp ../../launchers/standalone/target/classes
rm -fr  ${buildDir}/webapp

#5. build backend python modules
if [ "$service" == "pyllm" ]; then
  echo "start installing python modules with pip: ${pip_path}"
  requirementPath=$baseDir/../headless/python/requirements.txt
  ${pip_path} install -r ${requirementPath}
  echo "install python modules success"
fi

#6. reset runtime
rm -fr $runtimeDir/supersonic*
moveAllToRuntime
setEnvToWeb chat
setEnvToWeb headless

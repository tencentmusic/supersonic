#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/runtime
buildDir=$baseDir/build

cd $baseDir

#1. build backend java modules
rm -fr ${buildDir}/*.tar.gz
rm -fr dist

mvn -f $baseDir/../ clean package -DskipTests

#2. move package to build
cp $baseDir/../launchers/standalone/target/*.tar.gz ${buildDir}/supersonic.tar.gz

#3. build frontend webapp
chmod +x $baseDir/../webapp/start-fe-prod.sh
cd ../webapp
sh ./start-fe-prod.sh
cp -fr  ./supersonic-webapp.tar.gz ${buildDir}/

#4. copy webapp to java classpath
cd $buildDir
tar xvf supersonic-webapp.tar.gz
mv supersonic-webapp webapp
mv webapp ../../launchers/standalone/target/classes

#!/usr/bin/env bash

binDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(readlink -f $binDir/../)
libDir=$baseDir/lib
confDir=$baseDir/conf
webDir=$baseDir/webapp

source ${baseDir}/bin/env.sh


CLASSPATH=""
CLASSPATH=$CLASSPATH:$confDir

for jarPath in $libDir/*.jar; do
 CLASSPATH=$CLASSPATH:$jarPath
done



export CLASSPATH
export LANG="zh_CN.UTF-8"

cd $baseDir

if [[ "$JAVA_HOME" == "" ]]; then
  JAVA_HOME=$(ls /usr/jdk64/jdk* -d 2>/dev/null | xargs | awk '{print "'$APP_NAME'"}')
fi
export PATH=$JAVA_HOME/bin:$PATH

command="-Dfile.encoding="UTF-8"  -Duser.language="Zh" -Duser.region="CN" -Duser.timezone="GMT+08"  -Xms1024m -Xmx2048m "$MAIN_CLASS

mkdir -p $baseDir/logs
if [[ "$is_test" == "true" ]]; then
  java -Dspring.profiles.active="dev" $command >/dev/null 2>$baseDir/logs/error.log &
else
  java  $command $baseDir >/dev/null 2>$baseDir/logs/error.log &
fi

#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
source $sbinDir/supersonic-common.sh

command=$1
service=$2
profile=$3

if [ -z "$service"  ]; then
  service=${STANDALONE_SERVICE}
fi

if [ -z "$profile" ]; then
  profile="local"
fi

model_name=$service
cd $baseDir

function setMainClass {
  if [ "$service" == $CHAT_SERVICE ]; then
    main_class="com.tencent.supersonic.ChatLauncher"
  elif [ "$service" == $HEADLESS_SERVICE ]; then
    main_class="com.tencent.supersonic.HeadlessLauncher"
  else
    main_class="com.tencent.supersonic.StandaloneLauncher"
  fi
}

function setAppName {
  if [ "$service" == $CHAT_SERVICE ]; then
    app_name=$CHAT_APP_NAME
  elif [ "$service" == $HEADLESS_SERVICE ]; then
    app_name=$HEADLESS_APP_NAME
  else
    app_name=$STANDALONE_APP_NAME
  fi
}

function runJavaService {
  javaRunDir=$baseDir
  local_app_name=$1
  libDir=$baseDir/lib
  confDir=$baseDir/conf

  CLASSPATH=""
  CLASSPATH=$CLASSPATH:$confDir

  for jarPath in $libDir/*.jar; do
   CLASSPATH=$CLASSPATH:$jarPath
  done

  export CLASSPATH
  export LANG="zh_CN.UTF-8"

  cd $javaRunDir
  if [[ "$JAVA_HOME" == "" ]]; then
    JAVA_HOME=$(ls /usr/jdk64/jdk* -d 2>/dev/null | xargs | awk '{print "'$local_app_name'"}')
  fi
  export PATH=$JAVA_HOME/bin:$PATH
  command="-Dfile.encoding=UTF-8 -Duser.language=Zh -Duser.region=CN -Duser.timezone=GMT+08 -Dapp_name=${local_app_name} -Xms1024m -Xmx2048m $main_class"

  mkdir -p $javaRunDir/logs
  java -Dspring.profiles.active="$profile" $command >/dev/null 2>$javaRunDir/logs/error.log &
}

function start() {
  local_app_name=$1
  echo "Starting ${local_app_name}"
  pid=$(ps aux | grep ${local_app_name} | grep -v grep | awk '{print $2}')
  if [[ "$pid" == "" ]]; then
    runJavaService ${local_app_name}
  else
    echo "Process (PID = $pid) is running."
    return 1
  fi
  echo "Start success"
}

function stop() {
  echo "Stopping $1"
  pid=$(ps aux | grep $1 | grep -v grep | awk '{print $2}')
  if [[ "$pid" == "" ]]; then
    echo "Process $1 is not running!"
    return 1
  else
    kill -9 $pid
    echo "Process (PID = $pid) is killed!"
    return 0
  fi
  echo "Stop success"
}

setMainClass
setAppName
case "$command" in
  start)
    start ${app_name}
    ;;
  stop)
    stop $app_name
    ;;
  restart)
    stop ${app_name}
    start ${app_name}
    ;;
  *)
    echo "Use command {start|stop|restart} to run."
    exit 1
esac